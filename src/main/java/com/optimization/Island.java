package com.optimization;

import java.util.Random;

/**
 * Represents a single isolated ecosystem (Island).
 * Runs a localized Swarm with Differential Evolution (DE) crossover.
 */
public class Island {
    private final ObjectiveFunction function;
    private final Particle[] swarm;
    private final Random random;
    private final int dim;
    
    private Vector islandBestPosition;
    private double islandBestValue;
    
    // DE & PSO parameters
    private static final double F = 0.8;  // Differential weight
    private static final double CR = 0.9; // Crossover probability
    private static final double W = 0.5;
    private static final double C1 = 1.0;
    
    public Island(ObjectiveFunction function, int populationSize, double searchRadius, long seed) {
        this.function = function;
        this.dim = function.getDimension();
        this.random = new Random(seed);
        this.swarm = new Particle[populationSize];
        
        this.islandBestValue = Double.NEGATIVE_INFINITY;
        this.islandBestPosition = new Vector(dim);
        
        // Initialize population
        for (int i = 0; i < populationSize; i++) {
            swarm[i] = new Particle(dim, searchRadius, random);
            double val = function.evaluate(swarm[i].getPosition());
            swarm[i].updateBest(val);
            if (val > islandBestValue) {
                islandBestValue = val;
                islandBestPosition = swarm[i].getPosition().copy();
            }
        }
    }
    
    /**
     * Evolve this island for a certain number of epochs.
     * Uses a Hybrid of DE (Differential Evolution) and PSO.
     */
    public void evolve(int epochs) {
        int pop = swarm.length;
        for (int e = 0; e < epochs; e++) {
            for (int i = 0; i < pop; i++) {
                Particle current = swarm[i];
                Vector pos = current.getPosition();
                Vector vel = current.getVelocity();
                
                // Select 3 random distinct particles for DE
                int r1 = random.nextInt(pop);
                int r2 = random.nextInt(pop);
                int r3 = random.nextInt(pop);
                while(r1 == i) r1 = random.nextInt(pop);
                while(r2 == i || r2 == r1) r2 = random.nextInt(pop);
                while(r3 == i || r3 == r2 || r3 == r1) r3 = random.nextInt(pop);
                
                Vector x1 = swarm[r1].getPosition();
                Vector x2 = swarm[r2].getPosition();
                Vector x3 = swarm[r3].getPosition();
                
                Vector trial = new Vector(dim);
                int R = random.nextInt(dim); // Force at least one crossover
                
                for (int d = 0; d < dim; d++) {
                    // DE Mutation & Crossover
                    if (random.nextDouble() < CR || d == R) {
                        trial.set(d, x1.get(d) + F * (x2.get(d) - x3.get(d)));
                    } else {
                        // PSO-style momentum fallback
                        double newV = W * vel.get(d) + C1 * random.nextDouble() * (current.getBestPosition().get(d) - pos.get(d));
                        vel.set(d, newV);
                        trial.set(d, pos.get(d) + newV);
                    }
                }
                
                // Selection
                double trialVal = function.evaluate(trial);
                if (trialVal > current.getBestValue()) {
                    current.updateBest(trialVal);
                    // Update current position to trial (in-place replacement for simplicity)
                    for(int d=0; d<dim; d++) pos.set(d, trial.get(d));
                    
                    if (trialVal > islandBestValue) {
                        islandBestValue = trialVal;
                        islandBestPosition = trial.copy();
                    }
                }
            }
        }
    }
    
    public Vector getIslandBestPosition() { return islandBestPosition.copy(); }
    public double getIslandBestValue() { return islandBestValue; }
    
    /**
     * Accept a migrant from another island if it's better than our worst.
     */
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
