package com.optimization;

import java.util.Random;

/**
 * A particle in Quantum-Inspired Swarm Optimization (QPSO).
 * Unlike standard PSO, it has NO velocity. It only has a position
 * and remembers its personal best position based on the quantum wave function collapse.
 */
public class QuantumParticle {
    private Vector position;
    private Vector bestPosition;
    private double bestValue;

    public QuantumParticle(int dimension, double searchRadius, Random random) {
        position = new Vector(dimension);
        for (int i = 0; i < dimension; i++) {
            position.set(i, (random.nextDouble() * 2 - 1) * searchRadius);
        }
        bestPosition = position.copy();
        bestValue = Double.NEGATIVE_INFINITY;
    }

    public Vector getPosition() { return position; }
    public Vector getBestPosition() { return bestPosition; }
    public double getBestValue() { return bestValue; }

    public void updateBest(double currentValue) {
        if (currentValue > bestValue) {
            bestValue = currentValue;
            bestPosition = position.copy();
        }
    }
}
