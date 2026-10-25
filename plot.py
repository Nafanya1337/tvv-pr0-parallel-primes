"""График ускорения S(p) по results/results.csv. Запуск: python3 plot.py"""
import csv
import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt

rows = [r for r in csv.DictReader(open("results/results.csv")) if r["mode"] == "parallel"]
sizes = sorted({int(r["n_values"]) for r in rows})
processes = sorted({int(r["processes"]) for r in rows})

fig, ax = plt.subplots(figsize=(7, 4.5))
for n in sizes:
    speedup = [float(r["speedup_vs_1_process"]) for r in rows if int(r["n_values"]) == n]
    ax.plot(processes, speedup, marker="o", label=f"N = {n}")
ax.plot(processes, processes, "k--", label="идеальное (линейное)")
ax.set_xlabel("Число процессов p")
ax.set_ylabel("Ускорение S(p) = T(1) / T(p)")
ax.set_xticks(processes)
ax.grid(True, alpha=0.3)
ax.legend()
fig.tight_layout()
fig.savefig("results/speedup.png", dpi=200)
