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
 * Level 9: OMEGA Architecture (RL-HH + Surrogate Model).
 * 
 * The ultimate optimizer. It uses a Surrogate Neural Network for fast evaluations
 * AND a Reinforcement Learning Q-Agent to dynamically swap the core physics engine
 * (DE, QPSO, or SA) of each meta-island on the fly based on landscape topography.
 */
public class OmegaOptimizer {
    private final ObjectiveFunction realFunction;
    private final int numIslands;
    private final NeuralNetwork surrogateModel;
    private final QLearningAgent qAgent;
    
    private final ReentrantReadWriteLock nnLock = new ReentrantReadWriteLock();
    private static final int EPOCH_LENGTH = 10;
    
    public OmegaOptimizer(ObjectiveFunction function, int numIslands) {
        this.realFunction = function;
        this.numIslands = numIslands;
        this.surrogateModel = new NeuralNetwork(function.getDimension(), 128, 42L);
        this.qAgent = new QLearningAgent(42L);
    }
    
    private class ProxyFunction implements ObjectiveFunction {
        private double globalBestSeen = Double.NEGATIVE_INFINITY;
        @Override
        public double evaluate(Vector v) {
            double prediction;
            nnLock.readLock().lock();
            try { prediction = surrogateModel.predict(v); } 
            finally { nnLock.readLock().unlock(); }
            
            if (prediction > globalBestSeen || Math.random() < 0.1) {
                double trueValue = realFunction.evaluate(v);
                if (trueValue > globalBestSeen) globalBestSeen = trueValue;
                
                nnLock.writeLock().lock();
                try { surrogateModel.train(v, trueValue); } 
                finally { nnLock.writeLock().unlock(); }
                return trueValue;
            }
            return prediction;
        }
        @Override
        public int getDimension() { return realFunction.getDimension(); }
    }
    
    // A Meta-Island that can change its physics engine dynamically
    private class MetaIsland {
        private Island deEngine;
        private QuantumIsland qpsoEngine;
        private Vector currentBestPos;
        private double currentBestVal;
        
        public MetaIsland(ObjectiveFunction fn, int popSize, double radius, long seed, int maxIters) {
            deEngine = new Island(fn, popSize, radius, seed);
            qpsoEngine = new QuantumIsland(fn, popSize, radius, seed, maxIters);
            currentBestVal = Double.NEGATIVE_INFINITY;
            currentBestPos = new Vector(fn.getDimension());
        }
        
        public void executeAction(int action, int epochs, ObjectiveFunction fn) {
            if (action == QLearningAgent.ACTION_DE) {
                deEngine.evolve(epochs);
                if (deEngine.getIslandBestValue() > currentBestVal) {
                    currentBestVal = deEngine.getIslandBestValue();
                    currentBestPos = deEngine.getIslandBestPosition();
                    qpsoEngine.acceptMigrant(currentBestPos, currentBestVal); // Sync
                }
            } else if (action == QLearningAgent.ACTION_QPSO) {
                qpsoEngine.evolve(epochs);
                if (qpsoEngine.getIslandBestValue() > currentBestVal) {
                    currentBestVal = qpsoEngine.getIslandBestValue();
                    currentBestPos = qpsoEngine.getIslandBestPosition();
                    deEngine.acceptMigrant(currentBestPos, currentBestVal); // Sync
                }
            } else if (action == QLearningAgent.ACTION_SA) {
                AdaptiveOptimizer asa = new AdaptiveOptimizer(fn, 1e-4);
                Vector res = asa.optimize(currentBestPos, epochs * 10, 0.5, 0.01, null);
                double val = fn.evaluate(res);
                if (val > currentBestVal) {
                    currentBestVal = val;
                    currentBestPos = res;
                    deEngine.acceptMigrant(res, val);
                    qpsoEngine.acceptMigrant(res, val);
                }
            }
        }
        
        public void acceptMigrant(Vector pos, double val) {
            if (val > currentBestVal) {
                currentBestVal = val;
                currentBestPos = pos.copy();
                deEngine.acceptMigrant(pos, val);
                qpsoEngine.acceptMigrant(pos, val);
            }
        }
    }
    
    public Vector optimize(int populationPerIsland, int totalIterations, double searchRadius, ConvergenceTracker tracker) {
        ExecutorService pool = Executors.newFixedThreadPool(numIslands);
        ProxyFunction proxy = new ProxyFunction();
        Random masterRng = new Random();
        
        // Warmup Neural Network
        for (int i = 0; i < 200; i++) {
            Vector v = new Vector(realFunction.getDimension());
            for (int d = 0; d < v.getDimension(); d++) v.set(d, (masterRng.nextDouble() * 2 - 1) * searchRadius);
            surrogateModel.train(v, realFunction.evaluate(v));
        }
        
        MetaIsland[] islands = new MetaIsland[numIslands];
        for (int i = 0; i < numIslands; i++) {
            islands[i] = new MetaIsland(proxy, populationPerIsland, searchRadius, masterRng.nextLong(), totalIterations);
        }
        
        int migrationRounds = totalIterations / EPOCH_LENGTH;
        double globalBestValue = Double.NEGATIVE_INFINITY;
        Vector globalBestPos = new Vector(realFunction.getDimension());
        
        // RL State Management
        int[] currentStates = new int[numIslands];
        double[] previousFitness = new double[numIslands];
        
        for (int i = 0; i < numIslands; i++) {
            currentStates[i] = QLearningAgent.STATE_IMPROVING;
            previousFitness[i] = Double.NEGATIVE_INFINITY;
        }
        
        for (int round = 0; round < migrationRounds; round++) {
            List<Callable<Void>> tasks = new ArrayList<>();
            int[] actionsTaken = new int[numIslands];
            
            for (int i = 0; i < numIslands; i++) {
                final int idx = i;
                final int action = qAgent.selectAction(currentStates[idx]);
                actionsTaken[idx] = action;
                
                tasks.add(() -> { 
                    islands[idx].executeAction(action, EPOCH_LENGTH, proxy); 
                    return null; 
                });
            }
            
            try {
                List<Future<Void>> futures = pool.invokeAll(tasks);
                for(Future<Void> f : futures) f.get();
            } catch (Exception e) { e.printStackTrace(); }
            
            // Evaluate, Reward, and Migrate
            for (int i = 0; i < numIslands; i++) {
                double newFitness = islands[i].currentBestVal;
                
                // Calculate Reward
                double reward = 0;
                if (newFitness > previousFitness[i] + 1e-4) reward = 10.0;      // Big improvement
                else if (newFitness > previousFitness[i]) reward = 1.0;         // Small improvement
                else reward = -2.0;                                             // Wasted time (penalty)
                
                int nextState = qAgent.determineState(newFitness, previousFitness[i], globalBestValue);
                
                // RL Agent learns from this epoch
                qAgent.learn(currentStates[i], actionsTaken[i], reward, nextState);
                
                currentStates[i] = nextState;
                previousFitness[i] = newFitness;
                
                if (newFitness > globalBestValue) {
                    globalBestValue = newFitness;
                    globalBestPos = islands[i].currentBestPos.copy();
                }
            }
            
            // Ground truth sync
            globalBestValue = realFunction.evaluate(globalBestPos);
            
            for (MetaIsland isl : islands) isl.acceptMigrant(globalBestPos, globalBestValue);
            
            if (tracker != null) {
                for(int j=0; j<EPOCH_LENGTH; j++) tracker.record(round * EPOCH_LENGTH + j, globalBestValue);
            }
        }
        
        pool.shutdown();
        return globalBestPos;
    }
}
