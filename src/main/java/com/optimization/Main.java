package com.optimization;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║   TRANSCENDENCE — Level 11 Optimization Engine          ║
 * ║   The Absolute Final Form · There is no Level 12         ║
 * ╚══════════════════════════════════════════════════════════╝
 */
public class Main {

    private static final int    PROFILE_DIM   = 100;
    private static final int    PROFILE_ITER  = 3000;
    private static final double PROFILE_TEMP  = 5.0;
    private static final double PROFILE_LR    = 0.001;
    private static final int    NUM_THREADS   = 8;
    private static final double H             = 1e-5;

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║   TRANSCENDENCE — High-Dimensional Optimization          ║");
        System.out.println("║   Level 11 · CMA-ES · UCB Bandit · ELA · GA Meta-Opt    ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");

        // ── PHASE 1: Parallelism Profiling ─────────────────────────────────────
        System.out.println("▶ PHASE 1: Parallelism Profiling (" + PROFILE_DIM + "D)");
        ObjectiveFunction profileFn = new HighDimensionalRastrigin(PROFILE_DIM);
        Random rng = new Random(42);

        System.out.print("  Single-thread run ... ");
        long t0 = System.currentTimeMillis();
        AdaptiveOptimizer singleOpt = new AdaptiveOptimizer(profileFn, H);
        Vector singleStart = randomVector(PROFILE_DIM, 5.0, rng);
        singleOpt.optimize(singleStart, PROFILE_ITER, PROFILE_TEMP, PROFILE_LR, null);
        long singleMs = System.currentTimeMillis() - t0;
        System.out.println(singleMs + " ms");

        System.out.print("  8-thread parallel run ... ");
        ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS);
        List<Future<?>> futures = new ArrayList<>();
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < NUM_THREADS; i++) {
            final Vector start = randomVector(PROFILE_DIM, 5.0, rng);
            final AdaptiveOptimizer opt = new AdaptiveOptimizer(profileFn, H);
            futures.add(pool.submit(() -> opt.optimize(start, PROFILE_ITER, PROFILE_TEMP, PROFILE_LR, null)));
        }
        for (Future<?> f : futures) f.get();
        long multiMs = System.currentTimeMillis() - t1;
        pool.shutdown();
        System.out.println(multiMs + " ms");

        long   estimatedSerial = singleMs * NUM_THREADS;
        double speedup         = (double) estimatedSerial / multiMs;
        double efficiency      = (speedup / NUM_THREADS) * 100.0;
        System.out.printf("  Speedup: %.2fx  |  Efficiency: %.1f%%%n%n", speedup, efficiency);

        // ── PHASE 2: Full Benchmark (8 algorithms × 3 functions) ────────────────
        System.out.println("▶ PHASE 2: TRANSCENDENCE Full Benchmark");
        System.out.println("  Functions: Sphere(50D) · Ackley(50D) · Rastrigin(50D)");
        System.out.println("  Algorithms: TRANSCENDENCE · APEX · OMEGA · Neuro-Quantum · Q-Island · HybridPSO · AdaptiveSA · ClassicSA");
        System.out.println();

        List<ObjectiveFunction> functions = Arrays.asList(
            new SphereFunction(50),
            new AckleyFunction(50),
            new HighDimensionalRastrigin(50)
        );

        List<BenchmarkSuite.AlgorithmResult> results = BenchmarkSuite.run(functions);

        // ── PHASE 3: Report Generation ─────────────────────────────────────────
        System.out.println("\n▶ PHASE 3: Generating TRANSCENDENCE Reports");
        ReportGenerator.generateCSV("profiling_report.csv", results, singleMs, multiMs, speedup, efficiency);
        ReportGenerator.generateHTML("optimization_report.html", results, singleMs, multiMs, speedup, efficiency);

        // ── Final Summary ──────────────────────────────────────────────────────
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║               TRANSCENDENCE — COMPLETE                   ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.printf ("║  Speedup Ratio       : %-33s║%n", String.format("%.2fx faster", speedup));
        System.out.printf ("║  Parallel Efficiency : %-33s║%n", String.format("%.1f%%", efficiency));
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  Algorithm              Function    Mean Value   Time     ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        for (BenchmarkSuite.AlgorithmResult r : results) {
            System.out.printf("║  %-22s  %-11s  %8.4f  %5dms║%n",
                r.algorithmName.substring(0, Math.min(22, r.algorithmName.length())),
                r.functionName.substring(0, Math.min(11, r.functionName.length())),
                r.meanBestValue, r.meanTimeMs);
        }
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println("\n✔ profiling_report.csv    saved");
        System.out.println("✔ optimization_report.html saved  ← Open in browser! (GODMODE edition)");
    }

    private static Vector randomVector(int dim, double radius, Random rng) {
        Vector v = new Vector(dim);
        for (int i = 0; i < dim; i++) v.set(i, (rng.nextDouble() * 2 - 1) * radius);
        return v;
    }
}
