#!/usr/bin/env python3
"""
Create a box plot with:
  - x-axis: dagSize (one box per entry, no grouping)
  - y-axis: runtimesSMT

Input:
  data.json in the current directory

Output:
  - Displays the plot
  - Saves boxplot_dagSize_runtimesSMT.png
"""

import json
from pathlib import Path
import matplotlib.pyplot as plt


def main() -> None:
    path = Path("data.json")
    if not path.exists():
        raise FileNotFoundError(f"Could not find {path.resolve()}")

    with path.open("r", encoding="utf-8") as f:
        data = json.load(f)

    if not isinstance(data, list):
        raise TypeError("data.json must contain a JSON array")

    # One box per entry (no grouping)
    samples = []
    labels = []

    for i, entry in enumerate(data):
        dag = entry.get("dagSize")
        runtimes = entry.get("runtimesSMT")

        if dag is None or runtimes is None:
            continue
        if not isinstance(runtimes, list):
            continue

        # keep only numeric runtimes
        runtimes = [v for v in runtimes if isinstance(v, (int, float))]
        if not runtimes:
            continue

        samples.append(runtimes)
        labels.append(str(dag))

    if not samples:
        raise ValueError("No valid runtimesSMT data found.")

    fig, ax = plt.subplots(figsize=(max(10, 0.4 * len(samples)), 5))

    ax.boxplot(samples, labels=labels, showfliers=True)

    ax.set_xlabel("dagSize (one box per entry)")
    ax.set_ylabel("runtimeSMT")
    ax.set_title("runtimeSMT per experiment (no grouping)")
    ax.tick_params(axis="x", rotation=90)

    fig.tight_layout()
    out = Path("boxplot_dagSize_runtimesSMT.png")
    fig.savefig(out, dpi=200)
    plt.show()

    print(f"Saved: {out.resolve()}")


if __name__ == "__main__":
    main()

