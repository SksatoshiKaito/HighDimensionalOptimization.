package com.optimization;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Level 8: Neuro-Quantum Architecture (Surrogate-Assisted Optimization).
 * 
 * Intercepts calls to the objective function using a Deep Learning Surrogate Model.
 * Islands optimize against the fast Neural Network proxy.
 * Only promising points are evaluated on the computationally expensive real function,
 * which then acts as ground-truth to train the Neural Network further.
 */
public class SurrogateOptimizer {
    private final ObjectiveFunction realFunction;
    private final int numIslands;
    private final NeuralNetwork surrogateModel;
    
    // Thread-safety for training the surrogate model
    private final ReentrantReadWriteLock nnLock = new ReentrantReadWriteLock();
    
    // Hyperparameters
    private static final int MIGRATION_INTERVAL = 10; 
    
    public SurrogateOptimizer(ObjectiveFunction function, int numIslands) {
        this.realFunction = function;
        this.numIslands = numIslands;
        // MLP with 64 hidden neurons
        this.surrogateModel = new NeuralNetwork(function.getDimension(), 64, 42L);
    }
    
    /**
     * A proxy function that the Islands will use.
     */
    private class ProxyFunction implements ObjectiveFunction {
        private double globalBestSeen = Double.NEGATIVE_INFINITY;
        
        @Override
        public double evaluate(Vector v) {
            double prediction;
            nnLock.readLock().lock();
            try {
                prediction = surrogateModel.predict(v);
            } finally {
                nnLock.readLock().unlock();
            }
            
            // If the Neural Network thinks this is a really good point (better than our current best proxy, 
            // or just generally high), we evaluate the TRUE function and train the NN.
            // For benchmarking logic where all functions are fast, we'll force evaluate 10% of the time randomly 
            // and 100% of the time if prediction > globalBestSeen to simulate the real-world scenario.
            
            if (prediction > globalBestSeen || Math.random() < 0.1) {
                double trueValue = realFunction.evaluate(v);
                if (trueValue > globalBestSeen) {
                    globalBestSeen = trueValue;
                }
                
                // Train the NN with the true value
                nnLock.writeLock().lock();
                try {
                    surrogateModel.train(v, trueValue);
                } finally {
                    nnLock.writeLock().unlock();
                }
                return trueValue;
            }
            
            return prediction;
        }

        @Override
        public int getDimension() {
            return realFunction.getDimension();
        }
    }
    
    public Vector optimize(int populationPerIsland, int totalIterations, double searchRadius, ConvergenceTracker tracker) {
        ExecutorService pool = Executors.newFixedThreadPool(numIslands);
        ProxyFunction proxy = new ProxyFunction();
        
        Island[] deIslands = new Island[numIslands / 2];
        QuantumIsland[] qIslands = new QuantumIsland[numIslands - (numIslands / 2)];
        
        Random masterRng = new Random();
        
        // Initial Warmup: Train the Neural Network on a few random real points
        for (int i = 0; i < 100; i++) {
            Vector v = new Vector(realFunction.getDimension());
            for (int d = 0; d < v.getDimension(); d++) {
                v.set(d, (masterRng.nextDouble() * 2 - 1) * searchRadius);
            }
            double trueVal = realFunction.evaluate(v);
            surrogateModel.train(v, trueVal);
        }
        
        // Initialize Islands using the PROXY function
        for (int i = 0; i < deIslands.length; i++) {
            deIslands[i] = new Island(proxy, populationPerIsland, searchRadius, masterRng.nextLong());
        }
        for (int i = 0; i < qIslands.length; i++) {
            qIslands[i] = new QuantumIsland(proxy, populationPerIsland, searchRadius, masterRng.nextLong(), totalIterations);
        }
        
        int migrationRounds = totalIterations / MIGRATION_INTERVAL;
        double globalBestValue = Double.NEGATIVE_INFINITY;
        Vector globalBestPos = new Vector(realFunction.getDimension());
        
        for (int round = 0; round < migrationRounds; round++) {
            List<Callable<Void>> tasks = new ArrayList<>();
            
            for (Island isl : deIslands) {
                tasks.add(() -> { isl.evolve(MIGRATION_INTERVAL); return null; });
            }
            for (QuantumIsland qIsl : qIslands) {
                tasks.add(() -> { qIsl.evolve(MIGRATION_INTERVAL); return null; });
            }
            
            try {
                List<Future<Void>> futures = pool.invokeAll(tasks);
                for(Future<Void> f : futures) f.get();
            } catch (Exception e) { e.printStackTrace(); }
            
            // Migration
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
            
            // Evaluate global best on the REAL function to ensure truth
            globalBestValue = realFunction.evaluate(globalBestPos);
            
            // Broadcast
            for (Island isl : deIslands) isl.acceptMigrant(globalBestPos, globalBestValue);
            for (QuantumIsland qIsl : qIslands) qIsl.acceptMigrant(globalBestPos, globalBestValue);
            
            if (tracker != null) {
                for(int j=0; j<MIGRATION_INTERVAL; j++) {
                    tracker.record(round * MIGRATION_INTERVAL + j, globalBestValue);
                }
            }
        }
        
        pool.shutdown();
        
        // Final Polish directly on the real function
        AdaptiveOptimizer asa = new AdaptiveOptimizer(realFunction, 1e-5);
        Vector finalResult = asa.optimize(globalBestPos, 200, 1.0, 0.001, null);
        double finalVal = realFunction.evaluate(finalResult);
        
        if (finalVal > globalBestValue) {
            globalBestValue = finalVal;
            globalBestPos = finalResult.copy();
        }
        
        return globalBestPos;
    }
}
