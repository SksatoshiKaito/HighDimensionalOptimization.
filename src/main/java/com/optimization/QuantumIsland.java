package com.optimization;

import java.util.Random;

/**
 * An isolated ecosystem running Quantum-Inspired Swarm Optimization (QPSO).
 * Particles collapse into new positions based on a quantum delta potential well
 * centered around their personal best and the island's global best.
 */
public class QuantumIsland {
    private final ObjectiveFunction function;
    private final QuantumParticle[] swarm;
    private final Random random;
    private final int dim;
    
    private Vector islandBestPosition;
    private double islandBestValue;
    
    // QPSO Contraction-Expansion (CE) coefficient bounds
    private double ALPHA_START = 1.0;
    private double ALPHA_END = 0.5;
    
    private int totalEpochs;
    private int currentEpoch;
    
    public QuantumIsland(ObjectiveFunction function, int populationSize, double searchRadius, long seed, int totalEpochs) {
        this.function = function;
        this.dim = function.getDimension();
        this.random = new Random(seed);
        this.swarm = new QuantumParticle[populationSize];
        this.totalEpochs = totalEpochs;
        this.currentEpoch = 0;
        
        this.islandBestValue = Double.NEGATIVE_INFINITY;
        this.islandBestPosition = new Vector(dim);
        
        // Initialize population
        for (int i = 0; i < populationSize; i++) {
            swarm[i] = new QuantumParticle(dim, searchRadius, random);
            double val = function.evaluate(swarm[i].getPosition());
            swarm[i].updateBest(val);
            if (val > islandBestValue) {
                islandBestValue = val;
                islandBestPosition = swarm[i].getPosition().copy();
            }
        }
    }
    
    /** Constructor with custom contraction coefficient beta (for meta-optimization) */
    public QuantumIsland(ObjectiveFunction function, int populationSize, double searchRadius, long seed, int totalEpochs, double beta) {
        this(function, populationSize, searchRadius, seed, totalEpochs);
        this.ALPHA_START = beta;
        this.ALPHA_END = beta * 0.5;
    }

    /** Convenience: run optimization and return best position */
    public Vector optimize(int populationPerIsland, int iterations, double searchRadius, ConvergenceTracker tracker) {
        evolve(iterations);
        return islandBestPosition.copy();
    }

    public void evolve(int epochs) {
        int pop = swarm.length;
        for (int e = 0; e < epochs; e++) {
            currentEpoch++;
            // Linearly decrease alpha for convergence
            double alpha = ALPHA_START - (ALPHA_START - ALPHA_END) * ((double) currentEpoch / totalEpochs);
            
            // Calculate Mean Best Position (mbest)
            Vector mbest = new Vector(dim);
            for (int i = 0; i < pop; i++) {
                mbest.addInPlace(swarm[i].getBestPosition(), 1.0);
            }
            for (int d = 0; d < dim; d++) {
                mbest.set(d, mbest.get(d) / pop);
            }
            
            for (int i = 0; i < pop; i++) {
                QuantumParticle current = swarm[i];
                Vector pos = current.getPosition();
                Vector pBest = current.getBestPosition();
                
                for (int d = 0; d < dim; d++) {
                    double fi = random.nextDouble();
                    // Local attractor (p)
                    double p = (fi * pBest.get(d) + (1.0 - fi) * islandBestPosition.get(d));
                    
                    double u = random.nextDouble();
                    double L = alpha * Math.abs(mbest.get(d) - pos.get(d));
                    
                    double newPos;
                    if (random.nextDouble() > 0.5) {
                        newPos = p + L * Math.log(1.0 / u);
                    } else {
                        newPos = p - L * Math.log(1.0 / u);
                    }
                    pos.set(d, newPos);
                }
                
                // Evaluate
                double val = function.evaluate(pos);
                current.updateBest(val);
                if (val > islandBestValue) {
                    islandBestValue = val;
                    islandBestPosition = pos.copy();
                }
            }
        }
    }
    
    public Vector getIslandBestPosition() { return islandBestPosition.copy(); }
    public double getIslandBestValue() { return islandBestValue; }
    
    public void acceptMigrant(Vector migrantPos, double migrantVal) {
        if (migrantVal > islandBestValue) {
            islandBestValue = migrantVal;
            islandBestPosition = migrantPos.copy();
            
            // Replace a random particle with the migrant
            int r = random.nextInt(swarm.length);
            for(int d=0; d<dim; d++) {
                swarm[r].getPosition().set(d, migrantPos.get(d));
            }
            swarm[r].updateBest(migrantVal);
        }
    }
}
