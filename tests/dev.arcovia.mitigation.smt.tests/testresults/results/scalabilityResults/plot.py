#!/usr/bin/env python3
"""
Reads two JSON files from a folder and writes plot.png into that folder.

Input files (each is a JSON array of objects):
- smtData.json: objects contain { "scale": int, "totalRuntimeSMT": number, "totalTimeFindTFGs": number }
- satData.json: objects contain { "scale": int, "totalRuntimeSAT": number }

Behavior:
- Each curve is plotted independently based on the scales present in its own file.
- X axis: evenly spaced points (by index) *per series*, labels are from the series' x values (scale or 2^scale).
- Y axis: milliseconds (log scale), major ticks only at 10^n (integer log10 values),
  tick labels: human-readable + log10(ms).
- Y axis always starts at 10^1 (log10=1) and ends at the next decade above the maximum value.
"""

from __future__ import annotations

import argparse
import json
import math
from pathlib import Path
from typing import Any, Dict, List, Tuple

import matplotlib.pyplot as plt
from matplotlib.ticker import FuncFormatter, LogLocator

SMT_FILE_NAME = "smtData.json"
SAT_FILE_NAME = "satData.json"
OUT_FILE_NAME = "plot.png"


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


def load_list(path: Path) -> List[Dict[str, Any]]:
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


def _x_value(scale: int, x_mode: str) -> int:
    if x_mode == "pow2":
        return 2**scale
    if x_mode == "scale":
        return scale
    raise ValueError(f"Unknown x_mode: {x_mode}")


def prepare_smt_series(
    rows: List[Dict[str, Any]],
    x_mode: str,
) -> Tuple[List[int], List[float], List[float]]:
    parsed: List[Tuple[int, float, float]] = []
    for obj in rows:
        scale = _require_int(obj, "scale")
        smt = _require_number(obj, "totalRuntimeSMT")
        tfg = _require_number(obj, "totalTimeFindTFGs")
        parsed.append((scale, smt, tfg))

    parsed.sort(key=lambda t: t[0])

    x_vals = [_x_value(scale, x_mode) for scale, _, _ in parsed]
    smt_vals = [smt for _, smt, _ in parsed]
    tfg_vals = [tfg for _, _, tfg in parsed]
    return x_vals, smt_vals, tfg_vals


def prepare_sat_series(
    rows: List[Dict[str, Any]],
    x_mode: str,
) -> Tuple[List[int], List[float]]:
    parsed: List[Tuple[int, float]] = []
    for obj in rows:
        scale = _require_int(obj, "scale")
        sat = _require_number(obj, "totalRuntimeSAT")
        parsed.append((scale, sat))

    parsed.sort(key=lambda t: t[0])

    x_vals = [_x_value(scale, x_mode) for scale, _ in parsed]
    sat_vals = [sat for _, sat in parsed]
    return x_vals, sat_vals


def _min_positive(values: List[float]) -> float:
    pos = [v for v in values if v > 0]
    return min(pos) if pos else 1.0


def plot(
    sat_x: List[int],
    sat_y: List[float],
    smt_x: List[int],
    smt_y: List[float],
    tfg_x: List[int],
    tfg_y: List[float],
    out_path: Path,
    x_label: str,
) -> None:

    all_vals_raw = sat_y + smt_y + tfg_y
    eps = _min_positive(all_vals_raw) / 10.0

    def sanitize(vals: List[float]) -> List[float]:
        return [v if v > 0 else eps for v in vals]

    sat_s = sanitize(sat_y)
    smt_s = sanitize(smt_y)
    tfg_s = sanitize(tfg_y)


    all_positive = [v for v in (sat_s + smt_s + tfg_s) if v > 0]
    if not all_positive:
        raise ValueError("No positive runtime values to plot.")

    max_val = max(all_positive)
    y_min_decade = 0  # log10 = 1 (10 ms)
    y_max_decade = int(math.ceil(math.log10(max_val)))+1
    y_min = 10 ** y_min_decade
    y_max = 10 ** y_max_decade

    plt.figure(figsize=(10, 6))
    ax = plt.gca()
    ax.set_yscale("log")


    ax.yaxis.set_major_locator(LogLocator(base=10.0, subs=(1.0,)))
    ax.yaxis.set_minor_locator(LogLocator(base=10.0, subs=[]))


    ax.set_ylim(y_min, y_max)


    def time_plus_log_formatter(ms: float, _) -> str:
        if ms <= 0:
            return ""

        logv = math.log10(ms)

        if not math.isclose(logv, round(logv), rel_tol=1e-9, abs_tol=1e-12):
            return ""

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

        return f"{human}  (log10={logv:.0f})"

    ax.yaxis.set_major_formatter(FuncFormatter(time_plus_log_formatter))


    if sat_x and sat_s:
        sat_pos = list(range(len(sat_x)))
        plt.plot(
            sat_pos,
            sat_s,
            marker="o",
            linestyle="-",
            label="Total runtime of Niehues et al. approach",
        )

    if smt_x and smt_s:
        smt_pos = list(range(len(smt_x)))
        plt.plot(
            smt_pos,
            smt_s,
            marker="s",
            linestyle="-",
            label="Total runtime of our approach",
        )

    if tfg_x and tfg_s:
        tfg_pos = list(range(len(tfg_x)))
        plt.plot(
            tfg_pos,
            tfg_s,
            marker="^",
            linestyle="-",
            label="Total time to find TFGs",
        )

    # X ticks: use the longest series for a single, consistent tick set.
    series_for_ticks = max([sat_x, smt_x, tfg_x], key=len)
    x_positions = list(range(len(series_for_ticks)))
    x_tick_labels = [str(v) for v in series_for_ticks]

    ax.set_xticks(x_positions)
    ax.set_xticklabels(x_tick_labels, rotation=45, ha="right")

    ax.set_xlabel(x_label)
    ax.set_ylabel("Total runtime in ms (log scale)")
    ax.set_title("")
    ax.grid(True, which="major", linestyle="--", linewidth=0.5)
    ax.legend()

    plt.tight_layout()
    out_path.parent.mkdir(parents=True, exist_ok=True)
    plt.savefig(out_path, dpi=200)
    plt.close()


def main() -> None:
    parser = argparse.ArgumentParser()

    parser.add_argument(
        "folder",
        nargs="?",
        default=".",
        help='Folder containing smtData.json and satData.json (default: ".")',
    )

    parser.add_argument(
        "xlabel",
        nargs="?",
        default="2^scale",
        help='X-axis label text (default: "2^scale")',
    )

    parser.add_argument(
        "--xmode",
        choices=["pow2", "scale"],
        default="scale",
        help="X-axis tick values: 'pow2' = 2^scale, 'scale' = raw scale (default: scale)",
    )

    args = parser.parse_args()

    folder = Path(args.folder)
    if not folder.exists():
        raise FileNotFoundError(f"Folder does not exist: {folder.resolve()}")
    if not folder.is_dir():
        raise NotADirectoryError(f"Not a folder: {folder.resolve()}")

    smt_path = folder / SMT_FILE_NAME
    sat_path = folder / SAT_FILE_NAME
    out_path = folder / OUT_FILE_NAME

    smt_rows = load_list(smt_path)
    sat_rows = load_list(sat_path)

    smt_x, smt_y, tfg_y = prepare_smt_series(smt_rows, x_mode=args.xmode)
    sat_x, sat_y = prepare_sat_series(sat_rows, x_mode=args.xmode)


    tfg_x = smt_x

    plot(
        sat_x=sat_x,
        sat_y=sat_y,
        smt_x=smt_x,
        smt_y=smt_y,
        tfg_x=tfg_x,
        tfg_y=tfg_y,
        out_path=out_path,
        x_label=args.xlabel,
    )

    print(f"Wrote {out_path.resolve()}")


if __name__ == "__main__":
    main()
