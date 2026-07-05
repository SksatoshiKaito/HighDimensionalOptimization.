package com.optimization;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Benchmark Suite
 * Runs three algorithms (Adaptive SA, Classic SA, Random Search)
 * on multiple functions, repeats each N times for statistical robustness,
 * and returns a complete BenchmarkReport.
 */
public class BenchmarkSuite {

    private static final int    REPEATS      = 3;
    private static final int    ITERATIONS   = 5000;
    private static final double INIT_TEMP    = 5.0;
    private static final double INIT_LR      = 0.001;
    private static final int    NUM_THREADS  = 8;
    private static final double H            = 1e-5;

    public static class AlgorithmResult {
        public final String algorithmName;
        public final String functionName;
        public final double meanBestValue;
        public final double stdDev;
        public final long   meanTimeMs;
        public final List<Double> convergenceHistory;

        public AlgorithmResult(String algorithmName, String functionName,
                               double meanBestValue, double stdDev,
                               long meanTimeMs, List<Double> convergenceHistory) {
            this.algorithmName    = algorithmName;
            this.functionName     = functionName;
            this.meanBestValue    = meanBestValue;
            this.stdDev           = stdDev;
            this.meanTimeMs       = meanTimeMs;
            this.convergenceHistory = convergenceHistory;
        }
    }

    /**
     * Run the full benchmark suite on the given functions.
     */
    public static List<AlgorithmResult> run(List<ObjectiveFunction> functions) throws Exception {
        List<AlgorithmResult> results = new ArrayList<>();

        for (ObjectiveFunction fn : functions) {
            System.out.println("\n  [" + fn + "]");

            // ── Level 11: TRANSCENDENCE (CMA-ES + UCB Bandit + ELA + GA) ──
            results.add(runTranscendence(fn));

            // ── Level 10: APEX SINGULARITY (ELA + GA Meta-Opt + Cooperative Coevolution) ──
            results.add(runApex(fn));

            // ── Level 9: OMEGA Architecture (RL-HH + Neuro-Quantum) ──
            results.add(runOmega(fn));

            // ── Level 8: Neuro-Quantum Architecture (Ultimate Surrogate)
            results.add(runNeuroQuantum(fn));

            // ── Level 7: Quantum-Inspired Heterogeneous Architecture ─
            results.add(runIslandModel(fn));

            // ── Hybrid PSO (Level 5 Advanced Architecture) ───────────
            results.add(runHybridPSO(fn));

            // ── Adaptive SA (our engine, multi-threaded) ─────────────
            results.add(runAdaptiveSA(fn));

            // ── Classic SA (fixed cooling, single thread) ────────────
            results.add(runClassicSA(fn));

            // ── Random Search baseline ────────────────────────────────
            results.add(runRandomSearch(fn));
        }

        return results;
    }

    // ── TRANSCENDENCE Architecture (Level 11) ────────────────────────────────
    private static AlgorithmResult runTranscendence(ObjectiveFunction fn) {
        System.out.println("    ┌─ TRANSCENDENCE (CMA-ES+UCB+ELA+GA) ─────────");
        double[] values = new double[REPEATS];
        long totalTime = 0;
        List<Double> lastHistory = null;

        for (int rep = 0; rep < REPEATS; rep++) {
            long t0 = System.currentTimeMillis();
            ConvergenceTracker ct = new ConvergenceTracker(50);

            TranscendenceOptimizer trans = new TranscendenceOptimizer(fn, 8);
            Vector result = trans.optimize(5000, 5.0, ct);
            values[rep] = fn.evaluate(result);
            totalTime += System.currentTimeMillis() - t0;

            if (rep == REPEATS - 1) lastHistory = ct.getHistory();
        }

        double mean = mean(values);
        double std  = stdDev(values, mean);
        System.out.printf("    └─ TRANSCENDENCE RESULT: mean=%.4f  std=%.4f  time=%dms%n", mean, std, totalTime / REPEATS);
        return new AlgorithmResult("TRANSCENDENCE", fn.toString(), mean, std, totalTime / REPEATS, lastHistory);
    }

    // ── APEX SINGULARITY Architecture (Level 10) ─────────────────────────────
    private static AlgorithmResult runApex(ObjectiveFunction fn) {
        System.out.println("    ┌─ APEX SINGULARITY (ELA+GA+CoopCoevo+Pareto) ─");
        double[] values = new double[REPEATS];
        long totalTime = 0;
        List<Double> lastHistory = null;

        for (int rep = 0; rep < REPEATS; rep++) {
            long t0 = System.currentTimeMillis();
            ConvergenceTracker ct = new ConvergenceTracker(50);

            ApexSingularityOptimizer apex = new ApexSingularityOptimizer(fn, 8);
            Vector result = apex.optimize(5, 100, 5.0, ct);
            values[rep] = fn.evaluate(result);
            totalTime += System.currentTimeMillis() - t0;

            if (rep == REPEATS - 1) lastHistory = ct.getHistory();
        }

        double mean = mean(values);
        double std  = stdDev(values, mean);
        System.out.printf("    └─ APEX RESULT: mean=%.4f  std=%.4f  time=%dms%n", mean, std, totalTime / REPEATS);
        return new AlgorithmResult("APEX SINGULARITY", fn.toString(), mean, std, totalTime / REPEATS, lastHistory);
    }

    // ── OMEGA Architecture (Level 9) ──────────────────────────────────────────
    private static AlgorithmResult runOmega(ObjectiveFunction fn) {
        System.out.print("    OMEGA Model (RL + AI + Quantum) ... ");
        double[] values = new double[REPEATS];
        long totalTime = 0;
        List<Double> lastHistory = null;

        for (int rep = 0; rep < REPEATS; rep++) {
            long t0 = System.currentTimeMillis();
            ConvergenceTracker ct = new ConvergenceTracker(50);
            
            OmegaOptimizer omega = new OmegaOptimizer(fn, 8);
            Vector result = omega.optimize(5, 125, 5.0, ct);
            values[rep] = fn.evaluate(result);
            totalTime += System.currentTimeMillis() - t0;
            
            if (rep == REPEATS - 1) lastHistory = ct.getHistory();
        }

        double mean = mean(values);
        double std  = stdDev(values, mean);
        System.out.printf("mean=%.4f  std=%.4f  time=%dms%n", mean, std, totalTime / REPEATS);
        return new AlgorithmResult("OMEGA Model (RL+AI+QPSO)", fn.toString(), mean, std, totalTime / REPEATS, lastHistory);
    }

    // ── Neuro-Quantum Architecture (Level 8) ──────────────────────────────────
    private static AlgorithmResult runNeuroQuantum(ObjectiveFunction fn) {
        System.out.print("    Neuro-Quantum (Surrogate) ... ");
        double[] values = new double[REPEATS];
        long totalTime = 0;
        List<Double> lastHistory = null;

        for (int rep = 0; rep < REPEATS; rep++) {
            long t0 = System.currentTimeMillis();
            ConvergenceTracker ct = new ConvergenceTracker(50);
            
            SurrogateOptimizer surrogate = new SurrogateOptimizer(fn, 8);
            Vector result = surrogate.optimize(5, 125, 5.0, ct);
            values[rep] = fn.evaluate(result);
            totalTime += System.currentTimeMillis() - t0;
            
            if (rep == REPEATS - 1) lastHistory = ct.getHistory();
        }

        double mean = mean(values);
        double std  = stdDev(values, mean);
        System.out.printf("mean=%.4f  std=%.4f  time=%dms%n", mean, std, totalTime / REPEATS);
        return new AlgorithmResult("Neuro-Quantum (Surrogate)", fn.toString(), mean, std, totalTime / REPEATS, lastHistory);
    }

    // ── Heterogeneous Quantum Island Model (Level 7) ──────────────────────────
    private static AlgorithmResult runIslandModel(ObjectiveFunction fn) {
        System.out.print("    Q-Island Model (DE+QPSO) ... ");
        double[] values = new double[REPEATS];
        long totalTime = 0;
        List<Double> lastHistory = null;

        for (int rep = 0; rep < REPEATS; rep++) {
            long t0 = System.currentTimeMillis();
            ConvergenceTracker ct = new ConvergenceTracker(50);
            
            // 8 Islands, 5 particles each = 40 particles total.
            // 125 iterations * 40 = 5000 evaluations total.
            DistributedIslandOptimizer islandOpt = new DistributedIslandOptimizer(fn, 8);
            Vector result = islandOpt.optimize(5, 125, 5.0, ct);
            values[rep] = fn.evaluate(result);
            totalTime += System.currentTimeMillis() - t0;
            
            if (rep == REPEATS - 1) lastHistory = ct.getHistory();
        }

        double mean = mean(values);
        double std  = stdDev(values, mean);
        System.out.printf("mean=%.4f  std=%.4f  time=%dms%n", mean, std, totalTime / REPEATS);
        return new AlgorithmResult("Q-Island Model (DE+QPSO)", fn.toString(), mean, std, totalTime / REPEATS, lastHistory);
    }

    // ── Hybrid PSO (Level 5) ────────────────────────────────────────────────
    private static AlgorithmResult runHybridPSO(ObjectiveFunction fn) {
        System.out.print("    Hybrid PSO (Swarm+ASA)  ... ");
        double[] values = new double[REPEATS];
        long totalTime = 0;
        List<Double> lastHistory = null;

        for (int rep = 0; rep < REPEATS; rep++) {
            long t0 = System.currentTimeMillis();
            ConvergenceTracker ct = new ConvergenceTracker(50);
            HybridPSOOptimizer pso = new HybridPSOOptimizer(fn);
            
            // Give it a swarm of 20 particles, 250 iterations (same total evals as 5000 iterations for 1 thread)
            // 20 * 250 = 5000 evaluations.
            Vector result = pso.optimize(20, 250, 5.0, ct);
            values[rep] = fn.evaluate(result);
            totalTime += System.currentTimeMillis() - t0;
            
            if (rep == REPEATS - 1) lastHistory = ct.getHistory();
        }

        double mean = mean(values);
        double std  = stdDev(values, mean);
        System.out.printf("mean=%.4f  std=%.4f  time=%dms%n", mean, std, totalTime / REPEATS);
        return new AlgorithmResult("Hybrid PSO (Swarm+ASA)", fn.toString(), mean, std, totalTime / REPEATS, lastHistory);
    }

    // ── Adaptive SA ──────────────────────────────────────────────────────────
    private static AlgorithmResult runAdaptiveSA(ObjectiveFunction fn) throws Exception {
        System.out.print("    Adaptive SA (8-thread)  ... ");
        double[] values = new double[REPEATS];
        long totalTime = 0;
        List<Double> lastHistory = null;
        Random rng = new Random(99);

        ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS);

        for (int rep = 0; rep < REPEATS; rep++) {
            long t0 = System.currentTimeMillis();
            ConvergenceTracker tracker = new ConvergenceTracker(50);

            // Multi-threaded: 8 agents explore in parallel, take the best
            List<Future<Double>> futures = new ArrayList<>();
            List<ConvergenceTracker> trackers = new ArrayList<>();

            for (int t = 0; t < NUM_THREADS; t++) {
                final Vector start = randomVector(fn.getDimension(), 5.0, rng);
                final ConvergenceTracker ct = new ConvergenceTracker(50);
                trackers.add(ct);
                AdaptiveOptimizer opt = new AdaptiveOptimizer(fn, H);
                futures.add(pool.submit(() -> {
                    Vector result = opt.optimize(start, ITERATIONS, INIT_TEMP, INIT_LR, ct);
                    return fn.evaluate(result);
                }));
            }

            double bestThisRep = Double.NEGATIVE_INFINITY;
            ConvergenceTracker bestTracker = trackers.get(0);
            for (int t = 0; t < NUM_THREADS; t++) {
                double val = futures.get(t).get();
                if (val > bestThisRep) {
                    bestThisRep  = val;
                    bestTracker  = trackers.get(t);
                }
            }

            values[rep]  = bestThisRep;
            totalTime   += System.currentTimeMillis() - t0;
            if (rep == REPEATS - 1) lastHistory = bestTracker.getHistory();
        }

        pool.shutdown();

        double mean = mean(values);
        double std  = stdDev(values, mean);
        System.out.printf("mean=%.4f  std=%.4f  time=%dms%n", mean, std, totalTime / REPEATS);
        return new AlgorithmResult("Adaptive SA (8-thread)", fn.toString(), mean, std, totalTime / REPEATS, lastHistory);
    }

    // ── Classic SA (fixed cooling) ────────────────────────────────────────────
    private static AlgorithmResult runClassicSA(ObjectiveFunction fn) throws Exception {
        System.out.print("    Classic SA (1-thread)   ... ");
        double[] values = new double[REPEATS];
        long totalTime = 0;
        List<Double> lastHistory = null;
        Random rng = new Random(42);
        Optimizer opt = new Optimizer(fn, INIT_LR, H);

        for (int rep = 0; rep < REPEATS; rep++) {
            long t0 = System.currentTimeMillis();
            Vector start = randomVector(fn.getDimension(), 5.0, rng);
            Vector result = opt.optimize(start, ITERATIONS, INIT_TEMP, 0.999);
            values[rep] = fn.evaluate(result);
            totalTime  += System.currentTimeMillis() - t0;

            if (rep == REPEATS - 1) {
                // Build a simple flat history list for chart
                List<Double> hist = new ArrayList<>();
                for (int i = 0; i <= 100; i++) hist.add(values[rep]);
                lastHistory = hist;
            }
        }

        double mean = mean(values);
        double std  = stdDev(values, mean);
        System.out.printf("mean=%.4f  std=%.4f  time=%dms%n", mean, std, totalTime / REPEATS);
        return new AlgorithmResult("Classic SA (1-thread)", fn.toString(), mean, std, totalTime / REPEATS, lastHistory);
    }

    // ── Random Search baseline ─────────────────────────────────────────────────
    private static AlgorithmResult runRandomSearch(ObjectiveFunction fn) {
        System.out.print("    Random Search (baseline) ... ");
        double[] values = new double[REPEATS];
        long totalTime = 0;
        List<Double> lastHistory = null;

        for (int rep = 0; rep < REPEATS; rep++) {
            long t0 = System.currentTimeMillis();
            ConvergenceTracker ct = new ConvergenceTracker(50);
            RandomSearchOptimizer rs = new RandomSearchOptimizer(fn);
            // Give Random Search the same total evaluations budget
            Vector result = rs.optimize(ITERATIONS * NUM_THREADS, 5.0, ct);
            values[rep] = fn.evaluate(result);
            totalTime  += System.currentTimeMillis() - t0;
            if (rep == REPEATS - 1) lastHistory = ct.getHistory();
        }

        double mean = mean(values);
        double std  = stdDev(values, mean);
        System.out.printf("mean=%.4f  std=%.4f  time=%dms%n", mean, std, totalTime / REPEATS);
        return new AlgorithmResult("Random Search (baseline)", fn.toString(), mean, std, totalTime / REPEATS, lastHistory);
    }

    // ── Utility helpers ───────────────────────────────────────────────────────
    private static Vector randomVector(int dim, double radius, Random rng) {
        Vector v = new Vector(dim);
        for (int i = 0; i < dim; i++) v.set(i, (rng.nextDouble() * 2 - 1) * radius);
        return v;
    }

    private static double mean(double[] a) {
        double s = 0; for (double v : a) s += v; return s / a.length;
    }

    private static double stdDev(double[] a, double mean) {
        double s = 0; for (double v : a) s += (v - mean) * (v - mean);
        return Math.sqrt(s / a.length);
    }
}
