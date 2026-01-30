#!/usr/bin/env python3
import json
from pathlib import Path

import matplotlib.pyplot as plt


def make_dag_runtime_png(input_json: Path, output_png: Path):
    data = json.loads(input_json.read_text(encoding="utf-8"))

    dag_sizes = []
    mean_runtimes = []

    for entry in data:
        dag_size = entry["dagSize"]
        samples = entry["averageRuntime"]

        if not isinstance(samples, list) or len(samples) == 0:
            raise ValueError(f"averageRuntime must be a non-empty list for dagSize={dag_size}")

        mean_runtime = sum(samples) / len(samples)

        dag_sizes.append(dag_size)
        mean_runtimes.append(mean_runtime)

    fig, ax = plt.subplots(figsize=(8, 6))
    ax.scatter(dag_sizes, mean_runtimes)

    ax.set_xlabel("DAG Size")
    ax.set_ylabel("Average Runtime (ms)")
    ax.set_title("DAG Size in relation to Average Runtime")
    ax.grid(True)

    output_png.parent.mkdir(parents=True, exist_ok=True)
    fig.savefig(output_png, dpi=200, bbox_inches="tight")
    plt.close(fig)


if __name__ == "__main__":
    input_json = Path("data.json")
    output_png = Path("dag_vs_runtime.png")

    make_dag_runtime_png(input_json, output_png)
    print(f"Wrote: {output_png.resolve()}")

