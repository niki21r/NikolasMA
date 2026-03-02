#!/usr/bin/env python3
import json
from pathlib import Path
from statistics import mean

import matplotlib.pyplot as plt


def load_data(input_json: Path):
    return json.loads(input_json.read_text(encoding="utf-8"))


def avg(values, default=0.0):
    return mean(values) if values else default


def plot_complexity_reduction(data, out_png: Path):
    x = [avg(d["runtimesOn"]) for d in data]
    y = [avg(d["runtimesOff"]) for d in data]

    fig, ax = plt.subplots(figsize=(7, 6))
    ax.scatter(x, y)

    mn = min(min(x), min(y))
    mx = max(max(x), max(y))
    ax.plot([mn, mx], [mn, mx])

    ax.set_xlabel("Average runtime with enabled complexity reduction (ms)")
    ax.set_ylabel("Average runtime with disabled complexity reduction (ms)")
    ax.grid(True)

    fig.savefig(out_png, dpi=200, bbox_inches="tight")
    plt.close(fig)


def main():
    input_json = Path("data.json")
    out_png = Path("plots.pdf")  

    data = load_data(input_json)
    plot_complexity_reduction(data, out_png)

    print("Wrote plot to:", out_png.resolve())


if __name__ == "__main__":
    main()
