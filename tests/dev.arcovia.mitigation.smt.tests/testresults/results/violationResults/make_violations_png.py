#!/usr/bin/env python3
import json
from pathlib import Path
import matplotlib.pyplot as plt
from matplotlib.transforms import Bbox


def make_violations_png(input_json: Path, output_png: Path):
    data = json.loads(input_json.read_text(encoding="utf-8"))

    models = sorted({r["model"] for r in data})
    constraints = sorted({int(r["constraints"]) for r in data})

    table = {
        (r["model"], int(r["constraints"])):
        f'{r["violationsBefore"]} → {r["violationsAfter"]}'
        for r in data
    }

    cell_text = [[table.get((m, c), "") for c in constraints] for m in models]

    fig_w = max(8, 1.2 + 0.9 * len(constraints))
    fig_h = max(4, 1.2 + 0.6 * len(models))

    dpi = 200
    fig, ax = plt.subplots(figsize=(fig_w, fig_h), dpi=dpi)
    ax.axis("off")

    tbl = ax.table(
        cellText=cell_text,
        rowLabels=models,
        colLabels=[str(c) for c in constraints],
        cellLoc="center",
        loc="center",
    )
    tbl.auto_set_font_size(False)
    tbl.set_fontsize(10)
    tbl.scale(1.0, 1.4)

    # --- Draw once to get the table's exact bbox ---
    fig.canvas.draw()
    renderer = fig.canvas.get_renderer()
    table_bbox_px = tbl.get_window_extent(renderer=renderer)

    # --- Place title relative to the TABLE bbox (pixel-perfect) ---
    title_str = "Confidentiality violations before and after the repair"
    gap_px = 1  # set to 0 if you want it touching; 1-3 usually looks best

    # Convert table bbox x-center and y-top from pixels -> figure fraction
    x_center_px = (table_bbox_px.x0 + table_bbox_px.x1) / 2
    y_top_px = table_bbox_px.y1

    x_fig = x_center_px / (fig.bbox.width)
    y_fig = (y_top_px + gap_px) / (fig.bbox.height)

    title_text = fig.text(
        x_fig, y_fig,
        title_str,
        ha="center", va="bottom",
        fontsize=12, fontweight="bold",
    )

    # Need another draw to get the title bbox
    fig.canvas.draw()
    renderer = fig.canvas.get_renderer()
    title_bbox_px = title_text.get_window_extent(renderer=renderer)

    # --- Crop to union of table + title ---
    full_bbox_px = Bbox.union([table_bbox_px, title_bbox_px])

    # 🔧 ADD TOP PADDING (in pixels)
    top_pad_px = 4  # <-- adjust this (2–6 is usually perfect)
    full_bbox_px = Bbox.from_extents(
        full_bbox_px.x0,
        full_bbox_px.y0,
        full_bbox_px.x1,
        full_bbox_px.y1 + top_pad_px,
    )

    # Convert pixels -> inches
    full_bbox_in = full_bbox_px.transformed(fig.dpi_scale_trans.inverted())

    output_png.parent.mkdir(parents=True, exist_ok=True)
    fig.savefig(
        output_png,
        dpi=dpi,
        bbox_inches=full_bbox_in,
        pad_inches=0,
    )



    fig.savefig(
        output_png,
        dpi=dpi,
        bbox_inches=full_bbox_in,
        pad_inches=0,   # NO whitespace around crop
        transparent=False,
    )
    plt.close(fig)


if __name__ == "__main__":
    input_json = Path("data.json")
    output_png = Path("violations.png")
    make_violations_png(input_json, output_png)
    print(f"Wrote: {output_png.resolve()}")

