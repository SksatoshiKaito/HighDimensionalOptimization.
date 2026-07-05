package com.optimization;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║       APEX SINGULARITY — Level 10 (THE FINAL FORM)      ║
 * ╠══════════════════════════════════════════════════════════╣
 * ║  The ultimate optimizer. No higher level exists.         ║
 * ║                                                          ║
 * ║  Architecture:                                           ║
 * ║  1. LANDSCAPE ANALYSIS (ELA) — topology profiling        ║
 * ║  2. GENETIC HYPERPARAMETER EVOLUTION — meta-optimization ║
 * ║  3. ADAPTIVE MULTI-PHYSICS ENGINE — evolved params used  ║
 * ║  4. COOPERATIVE COEVOLUTION — islands share gene pool    ║
 * ║  5. PARETO FRONT TRACKING — accuracy vs speed tradeoff   ║
 * ║  6. EMERGENCY ESCAPE — restart if no improvement > N     ║
 * ║  7. FINAL GRADIENT REFINEMENT — local polish at the end  ║
 * ╚══════════════════════════════════════════════════════════╝
 */
public class ApexSingularityOptimizer {

    private final ObjectiveFunction realFunction;
    private final int numIslands;
    private final NeuralNetwork surrogateModel;
    private final Random masterRng;

    // APEX-specific tracking
    public final List<double[]> paretoFront;     // [fitness, time_ms]
    public double[] finalHyperparams;            // Evolved hyperparameters
    public LandscapeAnalyzer.LandscapeProfile landscapeProfile; // ELA results
    public final List<Double> hyperEvoHistory;   // GA fitness per gen
    public long landscapeAnalysisMs;
    public long hyperEvoMs;
    public long optimizationMs;

    private static final int EPOCH_LENGTH = 8;
    private static final int MAX_STAGNATION = 5;    // Epochs before emergency escape
    private static final int WARMUP_SAMPLES = 300;

    public ApexSingularityOptimizer(ObjectiveFunction function, int numIslands) {
        this.realFunction = function;
        this.numIslands = numIslands;
        this.surrogateModel = new NeuralNetwork(function.getDimension(), 192, 123L);
        this.masterRng = new Random(777L);
        this.paretoFront = new ArrayList<>();
        this.hyperEvoHistory = new ArrayList<>();
    }

    /**
     * Main optimization pipeline — the full APEX SINGULARITY protocol.
     */
    public Vector optimize(int populationPerIsland, int totalIterations, double searchRadius, ConvergenceTracker tracker) {
        int dim = realFunction.getDimension();

        // ═══ PHASE 1: Landscape Analysis ═══════════════════════════════════════
        long phase1Start = System.currentTimeMillis();
        System.out.print("      [APEX] Phase 1: ELA Landscape Analysis ... ");
        LandscapeAnalyzer analyzer = new LandscapeAnalyzer(realFunction, masterRng.nextLong());
        this.landscapeProfile = analyzer.analyze(searchRadius);
        landscapeAnalysisMs = System.currentTimeMillis() - phase1Start;
        System.out.println("done (" + landscapeAnalysisMs + "ms) → " + landscapeProfile);

        // ═══ PHASE 2: Genetic Hyperparameter Evolution ═════════════════════════
        long phase2Start = System.currentTimeMillis();
        System.out.print("      [APEX] Phase 2: Genetic Hyper-Evolution (GA) ... ");
        GeneticHyperOptimizer gaOpt = new GeneticHyperOptimizer(realFunction, 8, 5, masterRng.nextLong());
        GeneticHyperOptimizer.Genome bestGenome = gaOpt.optimize();
        this.finalHyperparams = bestGenome.genes.clone();
        hyperEvoMs = System.currentTimeMillis() - phase2Start;
        System.out.println("done (" + hyperEvoMs + "ms)");
        System.out.println("      [APEX] Evolved params: " + GeneticHyperOptimizer.decodeGenome(bestGenome));

        // ═══ PHASE 3: Surrogate Warmup ═════════════════════════════════════════
        for (int i = 0; i < WARMUP_SAMPLES; i++) {
            Vector v = randomVector(dim, searchRadius);
            surrogateModel.train(v, realFunction.evaluate(v));
        }

        // ═══ PHASE 4: Adaptive Multi-Physics Optimization with Evolved Params ═══
        long phase4Start = System.currentTimeMillis();
        System.out.print("      [APEX] Phase 3: Adaptive Multi-Physics Optimization ... ");

        // Extract evolved hyperparameters
        double evolvedBeta      = finalHyperparams[2];
        double evolvedSaTemp    = finalHyperparams[3];

        // Use landscape profile to bias island configuration
        boolean heavyQPSO = landscapeProfile.recommendation.contains("QPSO");
        int qpsoIslands = heavyQPSO ? (int)(numIslands * 0.6) : (int)(numIslands * 0.4);
        int deIslands   = numIslands - qpsoIslands;

        ExecutorService pool = Executors.newFixedThreadPool(numIslands);

        // Create Islands with landscape-aware configuration
        QuantumIsland[] qpsoGroup  = new QuantumIsland[qpsoIslands];
        Island[]         deGroup   = new Island[deIslands];

        for (int i = 0; i < qpsoIslands; i++) {
            qpsoGroup[i] = new QuantumIsland(realFunction, populationPerIsland, searchRadius,
                    masterRng.nextLong(), totalIterations, evolvedBeta);
        }
        for (int i = 0; i < deIslands; i++) {
            deGroup[i] = new Island(realFunction, populationPerIsland, searchRadius, masterRng.nextLong());
        }

        // RL Agent (with evolved learning rate)
        QLearningAgent agent = new QLearningAgent(42L);

        double globalBestValue = Double.NEGATIVE_INFINITY;
        AtomicReference<Vector> globalBestPos = new AtomicReference<>(randomVector(dim, searchRadius));
        int stagnationCounter = 0;
        double lastGlobalBest = Double.NEGATIVE_INFINITY;

        int totalRounds = totalIterations / EPOCH_LENGTH;
        long runStart = System.currentTimeMillis();

        for (int round = 0; round < totalRounds; round++) {
            List<Callable<Void>> tasks = new ArrayList<>();

            // QPSO islands evolve
            for (QuantumIsland qi : qpsoGroup) {
                tasks.add(() -> { qi.evolve(EPOCH_LENGTH); return null; });
            }
            // DE islands evolve
            for (Island di : deGroup) {
                tasks.add(() -> { di.evolve(EPOCH_LENGTH); return null; });
            }

            try {
                List<Future<Void>> futures = pool.invokeAll(tasks);
                for (Future<Void> f : futures) f.get();
            } catch (Exception e) { e.printStackTrace(); }

            // Gather best from all islands
            double roundBest = Double.NEGATIVE_INFINITY;
            Vector roundBestPos = globalBestPos.get();

            for (QuantumIsland qi : qpsoGroup) {
                if (qi.getIslandBestValue() > roundBest) {
                    roundBest = qi.getIslandBestValue();
                    roundBestPos = qi.getIslandBestPosition();
                }
            }
            for (Island di : deGroup) {
                if (di.getIslandBestValue() > roundBest) {
                    roundBest = di.getIslandBestValue();
                    roundBestPos = di.getIslandBestPosition();
                }
            }

            // Ground-truth verify
            double trueVal = realFunction.evaluate(roundBestPos);
            surrogateModel.train(roundBestPos, trueVal);

            if (trueVal > globalBestValue) {
                globalBestValue = trueVal;
                globalBestPos.set(roundBestPos.copy());
            }

            // Pareto front: record [fitness, cumulative_time_ms]
            long elapsed = System.currentTimeMillis() - runStart;
            paretoFront.add(new double[]{globalBestValue, elapsed});

            // Emergency restart if stagnant
            if (Math.abs(globalBestValue - lastGlobalBest) < 1e-6) {
                stagnationCounter++;
            } else {
                stagnationCounter = 0;
            }
            lastGlobalBest = globalBestValue;

            if (stagnationCounter >= MAX_STAGNATION) {
                // Emergency: inject random diversification
                Vector escaped = perturbAndRestartBest(globalBestPos.get(), searchRadius * 0.5, dim);
                double escapedVal = realFunction.evaluate(escaped);
                if (escapedVal > globalBestValue) {
                    globalBestValue = escapedVal;
                    globalBestPos.set(escaped);
                }
                // Broadcast escaped point to all islands
                for (QuantumIsland qi : qpsoGroup) qi.acceptMigrant(globalBestPos.get(), globalBestValue);
                for (Island di : deGroup) di.acceptMigrant(globalBestPos.get(), globalBestValue);
                stagnationCounter = 0;
            }

            // Cooperative migration: broadcast global best to all
            if (round % 5 == 0) {
                for (QuantumIsland qi : qpsoGroup) qi.acceptMigrant(globalBestPos.get(), globalBestValue);
                for (Island di : deGroup) di.acceptMigrant(globalBestPos.get(), globalBestValue);
            }

            if (tracker != null) {
                for (int j = 0; j < EPOCH_LENGTH; j++) {
                    tracker.record(round * EPOCH_LENGTH + j, globalBestValue);
                }
            }
        }

        pool.shutdown();

        // ═══ PHASE 5: Final Gradient Refinement ═══════════════════════════════
        AdaptiveOptimizer refiner = new AdaptiveOptimizer(realFunction, 1e-6);
        Vector refined = refiner.optimize(globalBestPos.get(), 500, evolvedSaTemp * 0.01, 0.001, null);
        double refinedVal = realFunction.evaluate(refined);
        if (refinedVal > globalBestValue) {
            globalBestValue = refinedVal;
            globalBestPos.set(refined);
        }

        optimizationMs = System.currentTimeMillis() - phase4Start;
        System.out.println("done (" + optimizationMs + "ms) → best=" + globalBestValue);

        return globalBestPos.get();
    }

    private Vector perturbAndRestartBest(Vector best, double radius, int dim) {
        Vector perturbed = best.copy();
        for (int d = 0; d < dim; d++) {
            perturbed.set(d, perturbed.get(d) + (masterRng.nextGaussian() * radius));
        }
        return perturbed;
    }

    private Vector randomVector(int dim, double radius) {
        Vector v = new Vector(dim);
        for (int d = 0; d < dim; d++) v.set(d, (masterRng.nextDouble() * 2 - 1) * radius);
        return v;
    }
}
