package dev.arcovia.mitigation.smt.tests;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.examplemodels.TuhhModels;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import dev.arcovia.mitigation.smt.Main;

public class MemoryIsolatedTest {

    private static final int RUNS_PER_CONFIGURATION = 100;
    private static final int PARALLELISM = Runtime.getRuntime().availableProcessors();

    @Test
    public void testAllForMaxProcessMemoryFreshJvmEachRun() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(PARALLELISM);
        CompletionService<Long> completion = new ExecutorCompletionService<>(pool);

        try {
            var tuhhModels = TuhhModels.getTuhhModels();
            Map<Integer, List<AnalysisConstraint>> constraintMap =
                    ConstraintMapProvider.buildConstraintMap();

            List<MemoryResult> memoryResults = new ArrayList<>();
            long pageSizeBytes = readLinuxPageSizeBytes();

            for (var model : tuhhModels.keySet()) {
                if (!tuhhModels.get(model).contains(0))
                    continue;

                for (int i : List.of(1, 2, 4, 5, 7, 8, 10, 11)) {
                    List<AnalysisConstraint> constraint = constraintMap.get(i);

                    if (constraint == null) {
                        System.out.println("Skipping " + model + " with constraint " + i
                                + " because Constraint is undefined");
                        continue;
                    } else if (!tuhhModels.get(model).contains(i)) {
                        System.out.println("Skipping " + model + " with constraint " + i
                                + " because no model for this constraint is defined");
                        continue;
                    }

                    int dagSizeAfter = (int)
                            Main.findDagSize(Main.loadDFD(model, model + "_0"), constraint);

                    System.out.println("Measuring peak RSS in fresh JVM (parallel=" + PARALLELISM + ") for "
                            + model + " constraint " + i);

                    // Submit RUNS_PER_CONFIGURATION jobs
                    for (int run = 0; run < RUNS_PER_CONFIGURATION; run++) {
                        final int runIdx = run;
                        completion.submit(() -> {
                            long peak = runInFreshJvmAndMeasurePeakRss(model, i, pageSizeBytes);
                            System.out.println("Completed run " + runIdx + " peakMB=" + (peak / 1024L / 1024L));
                            return peak;
                        });
                    }

                    // Collect results
                    List<Long> peakRssBytes = new ArrayList<>(RUNS_PER_CONFIGURATION);
                    for (int k = 0; k < RUNS_PER_CONFIGURATION; k++) {
                        try {
                            Future<Long> f = completion.take();
                            peakRssBytes.add(f.get());
                        } catch (ExecutionException ee) {
                            // unwrap task failure
                            throw new RuntimeException("Measurement task failed", ee.getCause());
                        }
                    }

                    memoryResults.add(new MemoryResult(dagSizeAfter, peakRssBytes));
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            Path out = Path.of("testresults/results/memoryResults/freshJvm" + RUNS_PER_CONFIGURATION + "runs/data.json");
            Files.createDirectories(out.getParent());
            mapper.writeValue(out.toFile(), memoryResults);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            System.exit(1);
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private record MemoryResult(int dagSize, List<Long> peakRssBytes) {}

    private static long runInFreshJvmAndMeasurePeakRss(
            String model, int constraintId, long pageSizeBytes) throws Exception {

        String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = System.getProperty("java.class.path");

        ProcessBuilder pb = new ProcessBuilder(
                javaBin,
                "-cp", classpath,
                "dev.arcovia.mitigation.smt.tests.MemoryIsolatedRunner",
                model,
                Integer.toString(constraintId)
        );

        pb.redirectErrorStream(true);

        Process p = pb.start();
        long pid = p.pid();

        long peakRss = 0;

        while (p.isAlive()) {
            long rss = readRssBytesFromProcStatm(pid, pageSizeBytes);
            if (rss > peakRss) peakRss = rss;

            try {
                Thread.sleep(1);
            } catch (InterruptedException ignored) {
            }
        }

        String output;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null)
                sb.append(line).append('\n');
            output = sb.toString().trim();
        }

        int exit = p.waitFor();
        if (exit != 0) {
            throw new IllegalStateException(
                    "Child JVM failed (exit=" + exit + "):\n" + output);
        }

        return peakRss;
    }

    private static long readRssBytesFromProcStatm(long pid, long pageSizeBytes) {
        try {
            String statm = Files.readString(
                    Path.of("/proc", Long.toString(pid), "statm"),
                    StandardCharsets.US_ASCII).trim();

            int sp1 = statm.indexOf(' ');
            if (sp1 < 0) return 0;

            int sp2 = statm.indexOf(' ', sp1 + 1);
            String residentStr =
                    (sp2 < 0)
                            ? statm.substring(sp1 + 1)
                            : statm.substring(sp1 + 1, sp2);

            long residentPages = Long.parseLong(residentStr);
            return residentPages * pageSizeBytes;
        } catch (Exception e) {
            return 0;
        }
    }

    private static long readLinuxPageSizeBytes() {
        try {
            Process p = new ProcessBuilder("getconf", "PAGESIZE").start();
            String s;
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.US_ASCII))) {
                s = br.readLine();
            }
            p.waitFor();
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return 4096L;
        }
    }
}
