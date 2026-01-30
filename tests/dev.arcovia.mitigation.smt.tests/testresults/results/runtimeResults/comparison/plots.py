#!/usr/bin/env python3
import json
from pathlib import Path
from statistics import mean

import matplotlib.pyplot as plt


def load_data(input_json: Path):
    return json.loads(input_json.read_text(encoding="utf-8"))


def avg(values, default=0.0):
    # Defensive: handle empty lists just in case
    return mean(values) if values else default


def plot_sat_vs_smt(data, out_png: Path):
    x = [avg(d["runtimesSAT"]) for d in data]
    y = [avg(d["runtimesSMT"]) for d in data]

    fig, ax = plt.subplots(figsize=(7, 6))
    ax.scatter(x, y)

    mn = min(min(x), min(y))
    mx = max(max(x), max(y))
    ax.plot([mn, mx], [mn, mx])

    ax.set_xlabel("Average runtime of existing approach (ms)")
    ax.set_ylabel("Average runtime (ms)")
    ax.set_title("Runtime compared to existing approach")
    ax.grid(True)

    out_png.parent.mkdir(parents=True, exist_ok=True)
    fig.savefig(out_png, dpi=200, bbox_inches="tight")
    plt.close(fig)


def plot_offset_vs_dag(data, out_png: Path):
    x = [d["dagSize"] for d in data]
    y = [avg(d["runtimesSMT"]) - avg(d["runtimesSAT"]) for d in data]

    fig, ax = plt.subplots(figsize=(7, 6))
    ax.scatter(x, y)
    ax.axhline(0.0)

    ax.set_xlabel("DAG size")
    ax.set_ylabel("SMT − SAT runtime (ms) [avg]")
    ax.set_title("Avg Runtime Difference vs DAG Size")
    ax.grid(True)

    out_png.parent.mkdir(parents=True, exist_ok=True)
    fig.savefig(out_png, dpi=200, bbox_inches="tight")
    plt.close(fig)


def plot_offset_vs_clause(data, out_png: Path):
    x = [d["clauseCount"] for d in data]
    y = [avg(d["runtimesSMT"]) - avg(d["runtimesSAT"]) for d in data]

    fig, ax = plt.subplots(figsize=(7, 6))
    ax.scatter(x, y)
    ax.axhline(0.0)

    ax.set_xlabel("Clause count")
    ax.set_ylabel("SMT − SAT runtime (ms) [avg]")
    ax.set_title("Avg Runtime Difference vs Clause Count")
    ax.grid(True)

    out_png.parent.mkdir(parents=True, exist_ok=True)
    fig.savefig(out_png, dpi=200, bbox_inches="tight")
    plt.close(fig)


def main():
    input_json = Path("data.json")
    out_dir = Path("plots")

    data = load_data(input_json)

    plot_sat_vs_smt(data, out_dir / "sat_vs_smt.png")
    plot_offset_vs_dag(data, out_dir / "offset_vs_dag.png")
    plot_offset_vs_clause(data, out_dir / "offset_vs_clause.png")

    print("Wrote plots to:", out_dir.resolve())


if __name__ == "__main__":
    main()

