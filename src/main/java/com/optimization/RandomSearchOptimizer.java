package com.optimization;

import java.util.Random;

/**
 * Baseline 1: Pure Random Search
 * Randomly samples the space — no gradient, no learning.
 * Used as a baseline to show how much better our adaptive engine is.
 */
public class RandomSearchOptimizer {
    private final ObjectiveFunction function;
    private final Random random;

    public RandomSearchOptimizer(ObjectiveFunction function) {
        this.function = function;
        this.random = new Random();
    }

    public Vector optimize(int maxEvaluations, double searchRadius, ConvergenceTracker tracker) {
        int dim = function.getDimension();
        Vector best = randomVector(dim, searchRadius);
        double bestVal = function.evaluate(best);

        for (int i = 0; i < maxEvaluations; i++) {
            Vector candidate = randomVector(dim, searchRadius);
            double val = function.evaluate(candidate);
            if (val > bestVal) {
                bestVal = val;
                best = candidate.copy();
            }
            if (tracker != null) tracker.record(i, bestVal);
        }
        return best;
    }

    private Vector randomVector(int dim, double radius) {
        Vector v = new Vector(dim);
        for (int i = 0; i < dim; i++) {
            v.set(i, (random.nextDouble() * 2 - 1) * radius);
        }
        return v;
    }
}
