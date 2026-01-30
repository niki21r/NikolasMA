import json
import math
from math import erf, sqrt

# ---- Load JSON ----
with open("data.json", "r") as f:
    data = json.load(f)

smt = [entry["smtCost"] for entry in data]
sat = [entry["satCost"] for entry in data]

diff_all = [s - t for s, t in zip(smt, sat)]

# Remove zero differences
diff = [d for d in diff_all if d != 0]
n = len(diff)

print(f"Total pairs: {len(diff_all)}")
print(f"Zero-difference pairs removed: {len(diff_all) - n}")
print(f"Non-zero pairs used in test: {n}")

pos = sum(1 for d in diff if d > 0)
neg = sum(1 for d in diff if d < 0)
print(f"Non-zero diffs: {neg} negative (SMT<SAT), {pos} positive (SMT>SAT)")

# ---- Rank absolute differences (with average ranks for ties) ----
abs_diff = list(map(abs, diff))
sorted_idx = sorted(range(n), key=lambda i: abs_diff[i])

ranks = [0.0] * n
rank = 1
i = 0
while i < n:
    j = i
    while j + 1 < n and abs_diff[sorted_idx[j]] == abs_diff[sorted_idx[j + 1]]:
        j += 1

    avg_rank = (rank + (rank + (j - i))) / 2.0
    for k in range(i, j + 1):
        ranks[sorted_idx[k]] = avg_rank

    rank += (j - i + 1)
    i = j + 1

W_plus = sum(r for r, d in zip(ranks, diff) if d > 0)
W_minus = sum(r for r, d in zip(ranks, diff) if d < 0)

# Common reporting choice:
W = min(W_plus, W_minus)

# ---- Normal approximation for Z (no tie correction) ----
mean_W = n * (n + 1) / 4.0
sd_W = math.sqrt(n * (n + 1) * (2 * n + 1) / 24.0)

Z = (W - mean_W) / sd_W

# One-sided p-value for alternative "less" (SMT < SAT) corresponds to Z being small/negative
# p = Phi(Z)
p_value = 0.5 * (1.0 + erf(Z / sqrt(2.0)))

r = abs(Z) / math.sqrt(n)
# ---- Additional descriptive statistics ----
total_difference = sum(diff_all)
total_absolute_difference = sum(abs(d) for d in diff_all)

mean_difference = total_difference / len(diff_all)

print("\n--- Descriptive Difference Statistics ---")
print(f"Total raw difference (sum SMT - SAT): {total_difference}")
print(f"Total absolute difference: {total_absolute_difference}")
print(f"Mean difference: {mean_difference:.4f}")

print("\n--- Results ---")
print(f"W+ (sum ranks where SMT>SAT): {W_plus:.3f}")
print(f"W- (sum ranks where SMT<SAT): {W_minus:.3f}")
print(f"W  (min(W+,W-)): {W:.3f}")
print(f"Z value: {Z:.4f}")
print(f"One-sided p-value: {p_value:.12f}")  # fixed decimal
print(f"Effect size r: {r:.4f}")