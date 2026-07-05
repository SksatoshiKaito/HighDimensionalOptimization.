package com.optimization;

import java.util.Random;

/**
 * Represents a single entity (particle) in Swarm Intelligence.
 * It remembers its current position, its velocity, and the best position it has ever found.
 */
public class Particle {
    private Vector position;
    private Vector velocity;
    private Vector bestPosition;
    private double bestValue;

    public Particle(int dimension, double searchRadius, Random random) {
        position = new Vector(dimension);
        velocity = new Vector(dimension);
        
        for (int i = 0; i < dimension; i++) {
            position.set(i, (random.nextDouble() * 2 - 1) * searchRadius);
            velocity.set(i, (random.nextDouble() * 2 - 1) * (searchRadius * 0.1));
        }
        
        bestPosition = position.copy();
        bestValue = Double.NEGATIVE_INFINITY;
    }

    public Vector getPosition() { return position; }
    public Vector getVelocity() { return velocity; }
    public Vector getBestPosition() { return bestPosition; }
    public double getBestValue() { return bestValue; }

    public void updateBest(double currentValue) {
        if (currentValue > bestValue) {
            bestValue = currentValue;
            bestPosition = position.copy();
        }
    }
}
