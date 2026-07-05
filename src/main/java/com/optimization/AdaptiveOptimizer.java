package com.optimization;

import java.util.Random;

/**
 * Adaptive Simulated Annealing Optimizer
 *
 * NOVEL CONTRIBUTION: Unlike standard SA which uses a fixed cooling schedule,
 * this engine dynamically monitors the acceptance ratio of uphill vs downhill
 * moves and adjusts temperature in real time to maintain an optimal exploration-
 * exploitation balance. This is inspired by the theory of Adaptive SA (Ingber, 1993)
 * and is implemented here from scratch.
 *
 * Key insight: If acceptance rate is too high → temperature is still too hot (cool faster).
 *              If acceptance rate is too low  → temperature is too cold (reheat slightly).
 * Target acceptance rate: ~23% (theoretically optimal for continuous domains).
 */
public class AdaptiveOptimizer {
    private final ObjectiveFunction function;
    private final double h;
    private final Random random;

    // Adaptive control
    private static final double TARGET_ACCEPTANCE_RATE = 0.23;
    private static final double ADAPTATION_STRENGTH    = 0.05;
    private static final int    ADAPTATION_WINDOW      = 50;

    public AdaptiveOptimizer(ObjectiveFunction function, double h) {
        this.function = function;
        this.h = h;
        this.random = new Random();
    }

    /**
     * In-place gradient calculation (cache-friendly, no heap allocation per step).
     */
    private void calculateGradientInPlace(Vector current, Vector gradientOut) {
        for (int i = 0; i < current.getDimension(); i++) {
            double orig = current.get(i);
            current.set(i, orig + h);
            double fwd = function.evaluate(current);
            current.set(i, orig - h);
            double bwd = function.evaluate(current);
            current.set(i, orig);
            gradientOut.set(i, (fwd - bwd) / (2 * h));
        }
    }

    /**
     * Run Adaptive SA from a given start point.
     * Returns the best vector found, and populates the ConvergenceTracker.
     */
    public Vector optimize(Vector startPoint, int maxIterations,
                           double initialTemperature, double initialLearningRate,
                           ConvergenceTracker tracker) {
        int dim = startPoint.getDimension();
        Vector current  = startPoint.copy();
        Vector gradient = new Vector(dim);
        double temperature   = initialTemperature;
        double learningRate  = initialLearningRate;

        Vector bestSeen    = current.copy();
        double bestValue   = function.evaluate(bestSeen);

        // Adaptive state
        int acceptedInWindow = 0;
        int trialInWindow    = 0;

        for (int iter = 0; iter < maxIterations; iter++) {

            // ── Gradient step ─────────────────────────────────────────
            calculateGradientInPlace(current, gradient);

            // Candidate = current + lr * gradient + temperature noise
            Vector candidate = current.copy();
            candidate.addInPlace(gradient, learningRate);
            if (temperature > 1e-6) {
                // Scale noise by 1/sqrt(dim) so it doesn't drown signal in high dimensions
                double noiseScale = temperature / Math.sqrt(dim);
                candidate.addNoiseInPlace(noiseScale, random);
            }

            double currentVal   = function.evaluate(current);
            double candidateVal = function.evaluate(candidate);
            double delta        = candidateVal - currentVal;

            // ── Metropolis acceptance criterion ───────────────────────
            boolean accepted = false;
            if (delta >= 0) {
                accepted = true;
            } else if (temperature > 1e-10) {
                double prob = Math.exp(delta / temperature);
                accepted = random.nextDouble() < prob;
            }

            if (accepted) {
                current = candidate;
                acceptedInWindow++;
                if (candidateVal > bestValue) {
                    bestValue = candidateVal;
                    bestSeen  = current.copy();
                }
            }
            trialInWindow++;

            // ── Adaptive temperature control every ADAPTATION_WINDOW steps ──
            if (trialInWindow >= ADAPTATION_WINDOW) {
                double actualRate = (double) acceptedInWindow / trialInWindow;

                if (actualRate > TARGET_ACCEPTANCE_RATE) {
                    // Too many accepts → too hot → cool down faster
                    temperature *= (1.0 - ADAPTATION_STRENGTH);
                } else {
                    // Too few accepts → too cold → slight reheat
                    temperature *= (1.0 + ADAPTATION_STRENGTH * 0.3);
                }

                // Also adapt learning rate proportionally
                learningRate = Math.max(1e-6, learningRate * 0.999);

                // Reset window counters
                acceptedInWindow = 0;
                trialInWindow    = 0;
            }

            // Track convergence snapshot
            if (tracker != null) {
                tracker.record(iter, bestValue);
            }
        }

        return bestSeen;
    }
}
