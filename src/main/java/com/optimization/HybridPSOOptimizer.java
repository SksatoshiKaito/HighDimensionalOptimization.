package com.optimization;

import java.util.Random;

/**
 * Hybrid Swarm-Gradient Optimizer (Level 5 Architecture)
 * 
 * Phase 1: Global Exploration using Particle Swarm Optimization (PSO).
 * Phase 2: Local Exploitation using Adaptive Simulated Annealing (ASA).
 * 
 * This ensures we don't get trapped in local minima (handled by swarm intelligence)
 * and still achieve high precision (handled by gradient/ASA).
 */
public class HybridPSOOptimizer {
    private final ObjectiveFunction function;
    private final Random random;
    
    // PSO Hyperparameters
    private static final double W = 0.5;  // Inertia weight
    private static final double C1 = 1.5; // Cognitive (personal best) weight
    private static final double C2 = 1.5; // Social (global best) weight

    public HybridPSOOptimizer(ObjectiveFunction function) {
        this.function = function;
        this.random = new Random();
    }

    public Vector optimize(int swarmSize, int maxIterations, double searchRadius, ConvergenceTracker tracker) {
        int dim = function.getDimension();
        Particle[] swarm = new Particle[swarmSize];
        
        Vector globalBestPosition = new Vector(dim);
        double globalBestValue = Double.NEGATIVE_INFINITY;

        // Initialize Swarm
        for (int i = 0; i < swarmSize; i++) {
            swarm[i] = new Particle(dim, searchRadius, random);
            double val = function.evaluate(swarm[i].getPosition());
            swarm[i].updateBest(val);
            
            if (val > globalBestValue) {
                globalBestValue = val;
                globalBestPosition = swarm[i].getPosition().copy();
            }
        }

        // Phase 1: PSO Exploration (allocate 80% of iterations)
        int psoIters = (int)(maxIterations * 0.8);
        for (int iter = 0; iter < psoIters; iter++) {
            for (Particle p : swarm) {
                Vector pos = p.getPosition();
                Vector vel = p.getVelocity();
                Vector pBest = p.getBestPosition();

                for (int d = 0; d < dim; d++) {
                    double r1 = random.nextDouble();
                    double r2 = random.nextDouble();
                    
                    // Velocity update: v = w*v + c1*r1*(pbest - pos) + c2*r2*(gbest - pos)
                    double newV = W * vel.get(d) 
                                + C1 * r1 * (pBest.get(d) - pos.get(d)) 
                                + C2 * r2 * (globalBestPosition.get(d) - pos.get(d));
                    vel.set(d, newV);
                    
                    // Position update: pos = pos + v
                    pos.set(d, pos.get(d) + newV);
                }

                double val = function.evaluate(pos);
                p.updateBest(val);
                if (val > globalBestValue) {
                    globalBestValue = val;
                    globalBestPosition = pos.copy();
                }
            }
            if (tracker != null) tracker.record(iter, globalBestValue);
        }

        // Phase 2: Adaptive Local Exploitation (allocate 20% of iterations)
        // We take the global best from PSO and feed it into our Adaptive SA engine.
        int asaIters = maxIterations - psoIters;
        AdaptiveOptimizer asa = new AdaptiveOptimizer(function, 1e-5);
        
        // Start ASA from the best location found by the swarm
        // Lower temperature since we are already near a good minimum
        Vector finalResult = asa.optimize(globalBestPosition, asaIters, 1.0, 0.001, null);
        double finalVal = function.evaluate(finalResult);
        
        if (finalVal > globalBestValue) {
            globalBestValue = finalVal;
            globalBestPosition = finalResult.copy();
        }
        
        // Just record the final result at the end of the iterations
        if (tracker != null) {
            for (int i = psoIters; i < maxIterations; i++) {
                tracker.record(i, globalBestValue);
            }
        }

        return globalBestPosition;
    }
}
