package com.optimization;

import java.util.Random;

/**
 * APEX SINGULARITY: Landscape Topology Analyzer
 *
 * Analyzes the fitness landscape of an objective function to extract
 * topological properties: modality, ruggedness, basin depth.
 * This information is used by the ApexOptimizer to select the best strategy.
 *
 * Inspired by Exploratory Landscape Analysis (ELA) from academic research.
 */
public class LandscapeAnalyzer {

    public static class LandscapeProfile {
        public final double modality;      // 0=unimodal, 1=heavily multimodal
        public final double ruggedness;    // 0=smooth, 1=very rugged
        public final double basinWidth;    // Estimated basin of attraction width
        public final double funnelStrength;// 0=flat, 1=strong funnel toward optimum
        public final String recommendation;// Best strategy for this landscape

        public LandscapeProfile(double modality, double ruggedness,
                                double basinWidth, double funnelStrength) {
            this.modality = modality;
            this.ruggedness = ruggedness;
            this.basinWidth = basinWidth;
            this.funnelStrength = funnelStrength;
            this.recommendation = computeRecommendation();
        }

        private String computeRecommendation() {
            if (modality > 0.6 && ruggedness > 0.5) return "DE_QPSO_HEAVY";    // Multi-modal rugged: DE + heavy quantum
            if (modality > 0.4) return "QPSO_DE_BALANCED";                     // Multi-modal smooth: QPSO + DE
            if (ruggedness > 0.6) return "SA_RESTART";                         // Unimodal rugged: SA with restarts
            return "SA_GRADIENT";                                               // Unimodal smooth: fine SA/gradient
        }

        @Override
        public String toString() {
            return String.format("Modality=%.3f Ruggedness=%.3f BasinWidth=%.3f Funnel=%.3f => %s",
                    modality, ruggedness, basinWidth, funnelStrength, recommendation);
        }
    }

    private final ObjectiveFunction function;
    private final Random rng;
    private static final int SAMPLE_SIZE = 500;

    public LandscapeAnalyzer(ObjectiveFunction function, long seed) {
        this.function = function;
        this.rng = new Random(seed);
    }

    /**
     * Perform Exploratory Landscape Analysis (ELA) via random walk + sampling.
     */
    public LandscapeProfile analyze(double searchRadius) {
        int dim = function.getDimension();
        double[] samples = new double[SAMPLE_SIZE];
        Vector[] positions = new Vector[SAMPLE_SIZE];

        // 1. Sample random points
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            positions[i] = randomVector(dim, searchRadius);
            samples[i] = function.evaluate(positions[i]);
        }

        // 2. Compute modality (via local optima counting using neighbor comparison)
        int localOptima = 0;
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            boolean isLocalMax = true;
            // Compare with 5 nearby random neighbors
            for (int k = 0; k < 5; k++) {
                Vector neighbor = positions[i].copy();
                int d = rng.nextInt(dim);
                neighbor.set(d, neighbor.get(d) + (rng.nextDouble() - 0.5) * 0.5);
                if (function.evaluate(neighbor) > samples[i]) {
                    isLocalMax = false;
                    break;
                }
            }
            if (isLocalMax) localOptima++;
        }
        double modality = Math.min(1.0, (double) localOptima / (SAMPLE_SIZE * 0.2));

        // 3. Compute ruggedness via consecutive difference autocorrelation (random walk)
        double[] walkValues = new double[200];
        Vector current = randomVector(dim, searchRadius);
        walkValues[0] = function.evaluate(current);
        for (int i = 1; i < 200; i++) {
            Vector next = current.copy();
            for (int d = 0; d < dim; d++) {
                next.set(d, next.get(d) + (rng.nextGaussian() * 0.1));
            }
            walkValues[i] = function.evaluate(next);
            current = next;
        }
        double ruggedness = computeAutocorrelation(walkValues);

        // 4. Basin width estimation: how far from best before degradation
        double bestVal = Double.NEGATIVE_INFINITY;
        Vector bestPos = positions[0];
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            if (samples[i] > bestVal) { bestVal = samples[i]; bestPos = positions[i]; }
        }
        double basinWidth = estimateBasinWidth(bestPos, bestVal, searchRadius);

        // 5. Funnel strength: correlation between distance-to-best and fitness
        double funnelStrength = computeFunnelStrength(positions, samples, bestPos);

        return new LandscapeProfile(modality, ruggedness, basinWidth, funnelStrength);
    }

    /** Autocorrelation at lag 1 of a walk — high = smooth, low = rugged */
    private double computeAutocorrelation(double[] walk) {
        int n = walk.length;
        double mean = 0;
        for (double v : walk) mean += v;
        mean /= n;
        double num = 0, denom = 0;
        for (int i = 0; i < n - 1; i++) {
            num += (walk[i] - mean) * (walk[i + 1] - mean);
            denom += (walk[i] - mean) * (walk[i] - mean);
        }
        double autocorr = denom == 0 ? 0 : num / denom;
        // Ruggedness = 1 - |autocorrelation| (low autocorr = high ruggedness)
        return Math.max(0, Math.min(1.0, 1.0 - Math.abs(autocorr)));
    }

    /** Estimate how wide the basin of attraction is */
    private double estimateBasinWidth(Vector bestPos, double bestVal, double searchRadius) {
        int dim = function.getDimension();
        int inBasin = 0;
        double threshold = bestVal * 0.8;
        for (int i = 0; i < 100; i++) {
            Vector perturbed = bestPos.copy();
            double dist = rng.nextDouble() * searchRadius;
            for (int d = 0; d < dim; d++) {
                perturbed.set(d, perturbed.get(d) + rng.nextGaussian() * dist / Math.sqrt(dim));
            }
            if (function.evaluate(perturbed) > threshold) inBasin++;
        }
        return inBasin / 100.0;
    }

    /** Funnel strength: negative correlation of distance-to-best with fitness = strong funnel */
    private double computeFunnelStrength(Vector[] positions, double[] samples, Vector best) {
        double n = positions.length;
        double[] dists = new double[(int)n];
        double meanDist = 0, meanFit = 0;
        for (int i = 0; i < n; i++) {
            dists[i] = euclideanDistance(positions[i], best);
            meanDist += dists[i];
            meanFit += samples[i];
        }
        meanDist /= n; meanFit /= n;
        double cov = 0, varD = 0, varF = 0;
        for (int i = 0; i < n; i++) {
            cov += (dists[i] - meanDist) * (samples[i] - meanFit);
            varD += (dists[i] - meanDist) * (dists[i] - meanDist);
            varF += (samples[i] - meanFit) * (samples[i] - meanFit);
        }
        double corr = (varD == 0 || varF == 0) ? 0 : cov / Math.sqrt(varD * varF);
        // Negative correlation = fitness decreases with distance = strong funnel
        return Math.max(0, Math.min(1.0, -corr));
    }

    private double euclideanDistance(Vector a, Vector b) {
        double sum = 0;
        for (int d = 0; d < a.getDimension(); d++) {
            double diff = a.get(d) - b.get(d);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    private Vector randomVector(int dim, double radius) {
        Vector v = new Vector(dim);
        for (int i = 0; i < dim; i++) v.set(i, (rng.nextDouble() * 2 - 1) * radius);
        return v;
    }
}
