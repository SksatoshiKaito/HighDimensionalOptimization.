package com.optimization;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Level 7: Heterogeneous Distributed Island Model (State-of-the-Art / Quantum-Inspired)
 * 
 * Mixes classic DE Islands with Quantum-Inspired (QPSO) Islands.
 * They run concurrently and trade their best particles, blending 
 * physical momentum with quantum wave function collapse.
 */
public class DistributedIslandOptimizer {
    private final ObjectiveFunction function;
    private final int numIslands;
    
    // Hyperparameters
    private static final int MIGRATION_INTERVAL = 25; // Epochs before migration
    
    public DistributedIslandOptimizer(ObjectiveFunction function, int numIslands) {
        this.function = function;
        this.numIslands = numIslands;
    }
    
    public Vector optimize(int populationPerIsland, int totalIterations, double searchRadius, ConvergenceTracker tracker) {
        ExecutorService pool = Executors.newFixedThreadPool(numIslands);
        
        Island[] deIslands = new Island[numIslands / 2];
        QuantumIsland[] qIslands = new QuantumIsland[numIslands - (numIslands / 2)];
        
        Random masterRng = new Random();
        
        // Initialize DE Islands
        for (int i = 0; i < deIslands.length; i++) {
            deIslands[i] = new Island(function, populationPerIsland, searchRadius, masterRng.nextLong());
        }
        // Initialize Quantum Islands
        for (int i = 0; i < qIslands.length; i++) {
            qIslands[i] = new QuantumIsland(function, populationPerIsland, searchRadius, masterRng.nextLong(), totalIterations);
        }
        
        int migrationRounds = totalIterations / MIGRATION_INTERVAL;
        double globalBestValue = Double.NEGATIVE_INFINITY;
        Vector globalBestPos = new Vector(function.getDimension());
        
        for (int round = 0; round < migrationRounds; round++) {
            List<Callable<Void>> tasks = new ArrayList<>();
            
            // Run DE Islands
            for (Island isl : deIslands) {
                tasks.add(() -> { isl.evolve(MIGRATION_INTERVAL); return null; });
            }
            // Run Quantum Islands
            for (QuantumIsland qIsl : qIslands) {
                tasks.add(() -> { qIsl.evolve(MIGRATION_INTERVAL); return null; });
            }
            
            try {
                List<Future<Void>> futures = pool.invokeAll(tasks);
                for(Future<Void> f : futures) f.get();
            } catch (Exception e) { e.printStackTrace(); }
            
            // Migration Phase: Find global best among all islands
            for (Island isl : deIslands) {
                if (isl.getIslandBestValue() > globalBestValue) {
                    globalBestValue = isl.getIslandBestValue();
                    globalBestPos = isl.getIslandBestPosition();
                }
            }
            for (QuantumIsland qIsl : qIslands) {
                if (qIsl.getIslandBestValue() > globalBestValue) {
                    globalBestValue = qIsl.getIslandBestValue();
                    globalBestPos = qIsl.getIslandBestPosition();
                }
            }
            
            // Broadcast global best back to all islands
            for (Island isl : deIslands) isl.acceptMigrant(globalBestPos, globalBestValue);
            for (QuantumIsland qIsl : qIslands) qIsl.acceptMigrant(globalBestPos, globalBestValue);
            
            if (tracker != null) {
                for(int j=0; j<MIGRATION_INTERVAL; j++) {
                    tracker.record(round * MIGRATION_INTERVAL + j, globalBestValue);
                }
            }
        }
        
        pool.shutdown();
        
        // Final Polish with Adaptive SA
        AdaptiveOptimizer asa = new AdaptiveOptimizer(function, 1e-5);
        Vector finalResult = asa.optimize(globalBestPos, 200, 1.0, 0.001, null);
        double finalVal = function.evaluate(finalResult);
        
        if (finalVal > globalBestValue) {
            globalBestValue = finalVal;
            globalBestPos = finalResult.copy();
        }
        
        return globalBestPos;
    }
}
