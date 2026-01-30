#!/usr/bin/env python3
import json
from pathlib import Path
from statistics import mean

import matplotlib.pyplot as plt


def make_dag_memory_png(input_json: Path, output_png: Path):
    data = json.loads(input_json.read_text(encoding="utf-8"))

    dag_sizes = [entry["dagSize"] for entry in data]

    # Average bytes -> MiB (one dot per entry; do NOT merge same dagSize)
    memory_mib = [
        mean(entry["peakRssBytes"]) / (1024 * 1024)
        for entry in data
    ]

    fig, ax = plt.subplots(figsize=(8, 6))
    ax.scatter(dag_sizes, memory_mib)

    ax.set_xlabel("Expression tree size")
    ax.set_ylabel("Average Peak Memory Usage (MiB)")
    ax.set_title("Average peak memory consumption in relation to size of expression tree")
    ax.grid(True)

    output_png.parent.mkdir(parents=True, exist_ok=True)
    fig.savefig(output_png, dpi=200, bbox_inches="tight")
    plt.close(fig)


if __name__ == "__main__":
    input_json = Path("data.json")
    output_png = Path("dag_vs_memory.png")

    make_dag_memory_png(input_json, output_png)
    print(f"Wrote: {output_png.resolve()}")

