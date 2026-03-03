import json
import math
from scipy.stats import wilcoxon, rankdata, norm

file = open("data.json", "r")
data = json.load(file)
file.close()

smt = []
sat = []

for entry in data:
    smt.append(entry["smtCost"])
    sat.append(entry["satCost"])

diff_all = []
for i in range(len(smt)):
    diff_all.append(smt[i] - sat[i])

identical = 0
for d in diff_all:
    if d == 0:
        identical += 1

differing = len(diff_all) - identical

sum_total_difference = 0
for d in diff_all:
    sum_total_difference += d

diff = []
for d in diff_all:
    if d != 0:
        diff.append(d)

N = len(diff)

abs_diff = []
for d in diff:
    abs_diff.append(abs(d))

ranks = rankdata(abs_diff, method="average")

W_plus = 0.0
W_minus = 0.0

for i in range(N):
    if diff[i] > 0:
        W_plus += ranks[i]
    elif diff[i] < 0:
        W_minus += ranks[i]

W = W_plus
if W_minus < W:
    W = W_minus

result = wilcoxon(
    smt,
    sat,
    alternative="less",
    zero_method="wilcox",
    correction=False,
    mode="auto"
)

z = norm.ppf(result.pvalue)

if N > 0:
    R = abs(z) / math.sqrt(N)
else:
    R = 0

print("Amount of Identical Values:", identical)
print("Amount of differing values:", differing)
print("Sum of the total difference (sum SMT - sum SAT):", sum_total_difference)
print(f"W: {result.statistic}")
print(f"p-value: {result.pvalue:.12f}")
print("R:", R)
print("N:", N)
print(f"Wilcoxon scipy result: {result}")