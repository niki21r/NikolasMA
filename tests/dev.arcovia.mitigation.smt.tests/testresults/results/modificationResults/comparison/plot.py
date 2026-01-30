#!/usr/bin/env python3
import json
from pathlib import Path
import matplotlib.pyplot as plt
from matplotlib.transforms import Bbox

def clamp01(x):
    return 0.0 if x < 0.0 else 1.0 if x > 1.0 else x

def diff_to_rgb(diff_val, max_abs):
    if max_abs <= 0:
        return (1.0, 1.0, 1.0)
    strength = clamp01(abs(diff_val) / max_abs)
    if diff_val > 0:
        return (1.0 - 0.7 * strength, 1.0, 1.0 - 0.7 * strength)
    if diff_val < 0:
        return (1.0, 1.0 - 0.7 * strength, 1.0 - 0.7 * strength)
    return (1.0, 1.0, 1.0)

def make_cost_png(input_json: Path, output_png: Path):
    data = json.loads(input_json.read_text(encoding="utf-8"))

    models = sorted({r["model"] for r in data})
    constraints = sorted({int(r["constraints"]) for r in data})

    sat = {}
    smt = {}
    diffs = []
    for r in data:
        key = (r["model"], int(r["constraints"]))
        s1 = r.get("satCost", "")
        s2 = r.get("smtCost", "")
        sat[key] = s1
        smt[key] = s2
        try:
            if s1 != "" and s2 != "":
                diffs.append(float(s1) - float(s2))
        except Exception:
            pass

    max_abs = max((abs(d) for d in diffs), default=0.0)

    cell_text = []
    cell_colors = []
    for m in models:
        row_text = []
        row_colors = []
        for c in constraints:
            key = (m, c)
            s1 = sat.get(key, "")
            s2 = smt.get(key, "")
            if s1 == "" and s2 == "":
                row_text.append("")
                row_colors.append((0.92, 0.92, 0.92))
            else:
                row_text.append(f"{s1} → {s2}")
                try:
                    d = float(s1) - float(s2)
                except Exception:
                    d = 0.0
                row_colors.append(diff_to_rgb(d, max_abs))
        cell_text.append(row_text)
        cell_colors.append(row_colors)

    fig_w = max(8, 1.2 + 0.9 * len(constraints))
    fig_h = max(4, 1.2 + 0.6 * len(models))

    dpi = 200
    fig, ax = plt.subplots(figsize=(fig_w, fig_h), dpi=dpi)
    ax.axis("off")

    tbl = ax.table(
        cellText=cell_text,
        cellColours=cell_colors,
        rowLabels=models,
        colLabels=[str(c) for c in constraints],
        cellLoc="center",
        loc="center",
    )

    tbl.auto_set_font_size(False)
    tbl.set_fontsize(10)
    tbl.scale(1.0, 1.4)

    fig.canvas.draw()
    renderer = fig.canvas.get_renderer()
    table_bbox_px = tbl.get_window_extent(renderer=renderer)

    title_str = "Cost comparison to existing tool with uniform cost"
    gap_px = 1
    top_pad_px = 4

    x_center_px = (table_bbox_px.x0 + table_bbox_px.x1) / 2
    y_top_px = table_bbox_px.y1

    x_fig = x_center_px / fig.bbox.width
    y_fig = (y_top_px + gap_px) / fig.bbox.height

    title_text = fig.text(
        x_fig, y_fig,
        title_str,
        ha="center", va="bottom",
        fontsize=12, fontweight="bold",
    )

    fig.canvas.draw()
    renderer = fig.canvas.get_renderer()
    title_bbox_px = title_text.get_window_extent(renderer=renderer)

    full_bbox_px = Bbox.union([table_bbox_px, title_bbox_px])
    full_bbox_px = Bbox.from_extents(
        full_bbox_px.x0,
        full_bbox_px.y0,
        full_bbox_px.x1,
        full_bbox_px.y1 + top_pad_px,
    )

    full_bbox_in = full_bbox_px.transformed(fig.dpi_scale_trans.inverted())

    output_png.parent.mkdir(parents=True, exist_ok=True)
    fig.savefig(
        output_png,
        dpi=dpi,
        bbox_inches=full_bbox_in,
        pad_inches=0,
    )
    plt.close(fig)

if __name__ == "__main__":
    input_json = Path("data.json")
    output_png = Path("plot.png")
    make_cost_png(input_json, output_png)
    print(f"Wrote: {output_png.resolve()}")

