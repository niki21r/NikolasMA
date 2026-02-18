#!/usr/bin/env python3
"""
Reads ./data.json (array of objects) and writes ./plot.png.

- X axis: evenly spaced points (by index), tick labels are 2^scale
- Y axis: milliseconds (log scale)
- Y tick labels: human-readable time (ms/s/min/h) + log10(value in ms) appended
- X label text is configurable via CLI arg, e.g.:
    python plot.py "2^scale"
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any, Dict, List, Tuple

import matplotlib.pyplot as plt
from matplotlib.ticker import FuncFormatter, LogLocator
import math


DATA_FILE = Path("data.json")
OUT_FILE = Path("plot.png")


def _require_int(obj: Dict[str, Any], key: str) -> int:
    if key not in obj:
        raise KeyError(f"Missing key '{key}' in object: {obj}")
    val = obj[key]
    if isinstance(val, bool) or not isinstance(val, int):
        raise TypeError(f"Key '{key}' must be an integer, got {type(val).__name__}: {val}")
    return val


def _require_number(obj: Dict[str, Any], key: str) -> float:
    if key not in obj:
        raise KeyError(f"Missing key '{key}' in object: {obj}")
    val = obj[key]
    if isinstance(val, bool) or not isinstance(val, (int, float)):
        raise TypeError(f"Key '{key}' must be a number, got {type(val).__name__}: {val}")
    return float(val)


def load_data(path: Path) -> List[Dict[str, Any]]:
    if not path.exists():
        raise FileNotFoundError(f"Could not find {path.resolve()}")
    with path.open("r", encoding="utf-8") as f:
        data = json.load(f)
    if not isinstance(data, list):
        raise TypeError(f"Top-level JSON must be a list/array, got {type(data).__name__}")
    for i, item in enumerate(data):
        if not isinstance(item, dict):
            raise TypeError(f"Element {i} must be an object/dict, got {type(item).__name__}")
    return data  # type: ignore[return-value]


def prepare_series(rows: List[Dict[str, Any]]) -> Tuple[List[int], List[float], List[float], List[float]]:
    parsed = []
    for obj in rows:
        scale = _require_int(obj, "scale")
        sat = _require_number(obj, "totalRuntimeSAT")
        smt = _require_number(obj, "totalRuntimeSMT")
        tfg = _require_number(obj, "totalTimeFindTFGs")
        parsed.append((scale, sat, smt, tfg))

    parsed.sort(key=lambda t: t[0])

    x_vals = [2 ** t[0] for t in parsed]  # tick labels
    sat_vals = [t[1] for t in parsed]
    smt_vals = [t[2] for t in parsed]
    tfg_vals = [t[3] for t in parsed]
    return x_vals, sat_vals, smt_vals, tfg_vals


def _min_positive(values: List[float]) -> float:
    pos = [v for v in values if v > 0]
    return min(pos) if pos else 1.0


def _ensure_y_extremes_labeled(ax, min_log_sep: float = 0.20) -> None:
    """
    Add ymin/ymax as major ticks only if they are sufficiently separated
    from existing major ticks in log10-space (avoids overlapping labels).
    min_log_sep: minimum separation in decades (0.20 ~ factor 1.58).
    """
    ymin, ymax = ax.get_ylim()
    ticks = [t for t in ax.get_yticks() if t > 0]

    def is_far_enough(v: float) -> bool:
        lv = math.log10(v)
        for t in ticks:
            if abs(lv - math.log10(t)) < min_log_sep:
                return False
        return True

    new_ticks = list(ticks)
    if ymin > 0 and is_far_enough(ymin):
        new_ticks.append(ymin)
    if ymax > 0 and is_far_enough(ymax):
        new_ticks.append(ymax)

    ax.set_yticks(sorted(set(new_ticks)))


def plot(
    x_vals: List[int],
    sat: List[float],
    smt: List[float],
    tfg: List[float],
    out_path: Path,
    x_label: str,
) -> None:
    # Log scale cannot show <= 0; replace non-positive with a small epsilon.
    all_vals = sat + smt + tfg
    eps = _min_positive(all_vals) / 10.0

    def sanitize(vals: List[float]) -> List[float]:
        return [v if v > 0 else eps for v in vals]

    sat_s = sanitize(sat)
    smt_s = sanitize(smt)
    tfg_s = sanitize(tfg)

    # evenly spaced positions, labels are the actual 2^scale values
    x_positions = list(range(len(x_vals)))
    x_tick_labels = [str(v) for v in x_vals]

    plt.figure(figsize=(10, 6))
    plt.plot(x_positions, sat_s, marker="o", linestyle="-", label="totalRuntimeSAT")
    plt.plot(x_positions, smt_s, marker="s", linestyle="-", label="totalRuntimeSMT")
    plt.plot(x_positions, tfg_s, marker="^", linestyle="-", label="totalTimeFindTFGs")

    ax = plt.gca()
    ax.set_yscale("log")

    # Human-readable time + log10(ms) on the same tick label
    def time_plus_log_formatter(ms: float, _) -> str:
        if ms <= 0:
            return ""
        # human readable
        if ms < 1000:
            human = f"{ms:.0f} ms"
        else:
            s = ms / 1000.0
            if s < 60:
                human = f"{s:.1f} s"
            else:
                m = s / 60.0
                if m < 60:
                    human = f"{m:.1f} min"
                else:
                    h = m / 60.0
                    human = f"{h:.1f} h"
        # log10 in ms
        # (show 2 decimals; adjust if you prefer scientific notation)
        import math

        logv = math.log10(ms)
        return f"{human}  (log10={logv:.2f})"

    ax.yaxis.set_major_formatter(FuncFormatter(time_plus_log_formatter))

    ax.set_xlabel(x_label)
    ax.set_ylabel("Runtime (log scale)")
    ax.set_title("Runtimes vs scale")
    ax.grid(True, which="major", linestyle="--", linewidth=0.5)
    ax.legend()

    ax.set_xticks(x_positions)
    ax.set_xticklabels(x_tick_labels, rotation=45, ha="right")

    # Ensure the bottom/top of the visible y-range also get labeled
    plt.tight_layout()
    _ensure_y_extremes_labeled(ax)

    plt.tight_layout()
    plt.savefig(out_path, dpi=200)
    plt.close()


def main() -> None:
    parser = argparse.ArgumentParser()

    parser.add_argument(
        "xlabel",
        nargs="?",
        default="2^scale",
        help='X-axis label text (default: "2^scale")',
    )

    parser.add_argument(
        "-i",
        "--input",
        default="data.json",
        help="Input JSON file (default: data.json)",
    )

    parser.add_argument(
        "-o",
        "--output",
        default="plot.png",
        help="Output PNG file (default: plot.png)",
    )

    args = parser.parse_args()

    input_path = Path(args.input)
    output_path = Path(args.output)

    rows = load_data(input_path)
    x_vals, sat, smt, tfg = prepare_series(rows)
    plot(x_vals, sat, smt, tfg, output_path, x_label=args.xlabel)

    print(f"Wrote {output_path.resolve()}")

if __name__ == "__main__":
    main()
