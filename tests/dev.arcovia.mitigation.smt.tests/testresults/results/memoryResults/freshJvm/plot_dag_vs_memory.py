#!/usr/bin/env python3
import json
from pathlib import Path
import matplotlib.pyplot as plt


def make_dag_memory_png(input_json: Path, output_png: Path):
    data = json.loads(input_json.read_text(encoding="utf-8"))

    dag_sizes = [entry["dagSize"] for entry in data]

    # Convert bytes -> MiB
    memory_mib = [entry["peakRssBytes"] / (1024 * 1024) for entry in data]

    fig, ax = plt.subplots(figsize=(8, 6))
    ax.scatter(dag_sizes, memory_mib)

    ax.set_xlabel("DAG Size")
    ax.set_ylabel("Memory (MiB)")
    ax.set_title("DAG Size vs. Peak Memory Consumption")
    ax.grid(True)

    output_png.parent.mkdir(parents=True, exist_ok=True)
    fig.savefig(output_png, dpi=200, bbox_inches="tight")
    plt.close(fig)


if __name__ == "__main__":
    input_json = Path("data.json")
    output_png = Path("dag_vs_memory.png")

    make_dag_memory_png(input_json, output_png)
    print(f"Wrote: {output_png.resolve()}")

