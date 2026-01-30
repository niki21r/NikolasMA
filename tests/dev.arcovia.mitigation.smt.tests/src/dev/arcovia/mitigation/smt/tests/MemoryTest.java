package dev.arcovia.mitigation.smt.tests;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.examplemodels.TuhhModels;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import dev.arcovia.mitigation.smt.Main;

public class MemoryTest {

    private static final int MEASUREMENTS_PER_RUN = 1000;

    @Test
    public void testAllForMemory() throws Exception {
        try {
            var tuhhModels = TuhhModels.getTuhhModels();

            List<MemoryResult> memoryResults = new ArrayList<>();
            Map<Integer, List<AnalysisConstraint>> constraintMap =
                    ConstraintMapProvider.buildConstraintMap();

            for (var model : tuhhModels.keySet()) {
                if (!tuhhModels.get(model).contains(0))
                    continue;

                for (int i : List.of(1, 2, 4, 5, 7, 8, 10, 11)) {
                    List<AnalysisConstraint> constraint = constraintMap.get(i);

                    if (constraint == null) {
                        System.out.println("Skipping " + model + " with constraint " + i +
                                " because Constraint is undefined");
                        continue;
                    } else if (!tuhhModels.get(model).contains(i)) {
                        System.out.println("Skipping " + model + " with constraint " + i +
                                " because no model for this constraint is defined");
                        continue;
                    }

                    int dagSizeAfter = (int)
                            Main.findDagSize(Main.loadDFD(model, model + "_0"), constraint);

                    System.out.println("Measuring memory for " + model +
                            " with constraints " + i);

                    DataFlowDiagramAndDictionary dfd =
                            Main.loadDFD(model, model + "_0");

                    List<Long> peakRss = new ArrayList<>(MEASUREMENTS_PER_RUN);

                    for (int run = 0; run < MEASUREMENTS_PER_RUN; run++) {
                        long rssPeak;
                        try (RssSampler sampler = new RssSampler(1)) { // 1 ms sampling
                            sampler.start();
                            Main.run(dfd, constraint, null, null);
                            rssPeak = sampler.getPeakRssBytes();
                        }

                        peakRss.add(rssPeak);
                    }

                    memoryResults.add(
                            new MemoryResult(dagSizeAfter, peakRss)
                    );
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            Path out = Path.of("testresults/results/memoryResults/multiRun/data.json");
            Files.createDirectories(out.getParent());
            mapper.writeValue(out.toFile(), memoryResults);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * dagSize + multiple peak RSS deltas (one per measurement run).
     */
    private record MemoryResult(int dagSize, List<Long> peakRssDeltaBytes) {
    }


    /**
     * Linux RSS reader using /proc/self/statm (fast, JNI-inclusive).
     */
    static final class LinuxRss {
        private static final long PAGE_SIZE_BYTES = 4096L;

        static long readRssBytes() {
            try {
                String statm = Files.readString(
                        Path.of("/proc/self/statm"),
                        StandardCharsets.US_ASCII
                ).trim();

                // Format: size resident shared text lib data dt
                int firstSpace = statm.indexOf(' ');
                if (firstSpace < 0)
                    return -1;

                int secondSpace = statm.indexOf(' ', firstSpace + 1);
                String resident =
                        (secondSpace < 0)
                                ? statm.substring(firstSpace + 1)
                                : statm.substring(firstSpace + 1, secondSpace);

                long residentPages = Long.parseLong(resident);
                return residentPages * PAGE_SIZE_BYTES;

            } catch (Exception e) {
                return -1;
            }
        }
    }

    /**
     * 1 ms RSS sampler; records peak process memory.
     */
    static final class RssSampler implements AutoCloseable {
        private final long intervalMs;
        private final AtomicBoolean running = new AtomicBoolean(false);
        private Thread thread;
        private volatile long peakRssBytes = -1;

        RssSampler(long intervalMs) {
            this.intervalMs = intervalMs;
        }

        void start() {
            if (running.getAndSet(true))
                return;

            peakRssBytes = LinuxRss.readRssBytes();

            thread = new Thread(() -> {
                while (running.get()) {
                    long rss = LinuxRss.readRssBytes();
                    if (rss > peakRssBytes) {
                        peakRssBytes = rss;
                    }
                    try {
                        Thread.sleep(intervalMs);
                    } catch (InterruptedException ignored) {
                    }
                }
            }, "rss-sampler");

            thread.setDaemon(true);
            thread.start();
        }

        long getPeakRssBytes() {
            return peakRssBytes;
        }

        @Override
        public void close() {
            running.set(false);
            if (thread != null) {
                thread.interrupt();
                try {
                    thread.join(100);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}
