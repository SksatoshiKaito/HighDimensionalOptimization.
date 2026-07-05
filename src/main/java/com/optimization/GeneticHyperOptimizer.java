package com.optimization;

import java.util.Random;

/**
 * APEX SINGULARITY: Genetic Hyperparameter Optimizer
 *
 * A Genetic Algorithm that evolves the hyperparameters of an optimizer
 * (e.g., population size, mutation rate, crossover rate, step sizes)
 * to find the best configuration for a given function landscape.
 *
 * This is "Meta-Optimization" — optimizing the optimizer itself.
 * Concept used in AutoML and Neural Architecture Search (NAS).
 */
public class GeneticHyperOptimizer {

    /**
     * Hyperparameter genome encoding:
     * gene[0] = DE mutation factor F         (range 0.2 - 1.5)
     * gene[1] = DE crossover rate CR         (range 0.1 - 0.95)
     * gene[2] = QPSO contraction coeff β     (range 0.5 - 1.2)
     * gene[3] = SA initial temperature       (range 0.1 - 10.0)
     * gene[4] = SA cooling rate              (range 0.9 - 0.999)
     * gene[5] = SA gradient step size H      (range 1e-6 - 1e-3)
     * gene[6] = RL learning rate             (range 0.01 - 0.5)
     * gene[7] = RL epsilon decay             (range 0.9 - 0.999)
     */
    public static class Genome {
        public final double[] genes;
        public double fitness;

        public Genome(double[] genes) {
            this.genes = genes.clone();
            this.fitness = Double.NEGATIVE_INFINITY;
        }

        public Genome copy() {
            Genome g = new Genome(genes.clone());
            g.fitness = fitness;
            return g;
        }
    }

    private final ObjectiveFunction targetFunction;
    private final Random rng;
    private final int populationSize;
    private final int generations;

    // Gene bounds [min, max]
    private static final double[][] BOUNDS = {
        {0.2, 1.5},    // F
        {0.1, 0.95},   // CR
        {0.5, 1.2},    // beta
        {0.5, 8.0},    // SA temp
        {0.92, 0.999}, // SA cool
        {1e-6, 1e-3},  // SA step H
        {0.01, 0.4},   // RL lr
        {0.92, 0.999}  // RL eps decay
    };
    private static final int NUM_GENES = BOUNDS.length;
    private static final double MUTATION_RATE = 0.2;
    private static final double CROSSOVER_RATE = 0.7;

    public GeneticHyperOptimizer(ObjectiveFunction fn, int popSize, int generations, long seed) {
        this.targetFunction = fn;
        this.populationSize = popSize;
        this.generations = generations;
        this.rng = new Random(seed);
    }

    /** Run GA and return best hyperparameter genome */
    public Genome optimize() {
        Genome[] population = initializePopulation();
        evaluateAll(population);

        for (int gen = 0; gen < generations; gen++) {
            Genome[] offspring = new Genome[populationSize];

            for (int i = 0; i < populationSize; i++) {
                // Tournament selection
                Genome parent1 = tournament(population);
                Genome parent2 = tournament(population);

                // Uniform crossover
                Genome child = crossover(parent1, parent2);

                // Gaussian mutation
                mutate(child);

                offspring[i] = child;
            }

            evaluateAll(offspring);

            // Elitism: keep best half of old + new
            Genome[] combined = new Genome[populationSize * 2];
            System.arraycopy(population, 0, combined, 0, populationSize);
            System.arraycopy(offspring, 0, combined, populationSize, populationSize);
            population = selectBest(combined, populationSize);
        }

        // Return best genome
        Genome best = population[0];
        for (Genome g : population) {
            if (g.fitness > best.fitness) best = g;
        }
        return best;
    }

    private Genome[] initializePopulation() {
        Genome[] pop = new Genome[populationSize];
        for (int i = 0; i < populationSize; i++) {
            double[] genes = new double[NUM_GENES];
            for (int j = 0; j < NUM_GENES; j++) {
                genes[j] = BOUNDS[j][0] + rng.nextDouble() * (BOUNDS[j][1] - BOUNDS[j][0]);
            }
            pop[i] = new Genome(genes);
        }
        return pop;
    }

    private void evaluateAll(Genome[] population) {
        for (Genome g : population) {
            if (g.fitness == Double.NEGATIVE_INFINITY) {
                g.fitness = evaluateGenome(g);
            }
        }
    }

    /**
     * Evaluate genome fitness by running a quick Island+QPSO trial with these hyperparameters.
     * Uses very short budget to keep GA fast.
     */
    private double evaluateGenome(Genome g) {
        try {
            int dim = targetFunction.getDimension();
            double beta = g.genes[2];

            // Quick 3-particle, 30-iteration QPSO trial with evolved beta
            QuantumIsland qi = new QuantumIsland(targetFunction, 3, 5.0, rng.nextLong(), 30, beta);
            Vector result = qi.optimize(3, 30, 5.0, null);
            return targetFunction.evaluate(result);
        } catch (Exception e) {
            return Double.NEGATIVE_INFINITY;
        }
    }

    private Genome tournament(Genome[] population) {
        int a = rng.nextInt(populationSize);
        int b = rng.nextInt(populationSize);
        return population[a].fitness > population[b].fitness ? population[a] : population[b];
    }

    private Genome crossover(Genome p1, Genome p2) {
        double[] childGenes = new double[NUM_GENES];
        for (int j = 0; j < NUM_GENES; j++) {
            childGenes[j] = (rng.nextDouble() < CROSSOVER_RATE) ? p1.genes[j] : p2.genes[j];
        }
        return new Genome(childGenes);
    }

    private void mutate(Genome g) {
        for (int j = 0; j < NUM_GENES; j++) {
            if (rng.nextDouble() < MUTATION_RATE) {
                double range = BOUNDS[j][1] - BOUNDS[j][0];
                g.genes[j] += rng.nextGaussian() * range * 0.1;
                g.genes[j] = Math.max(BOUNDS[j][0], Math.min(BOUNDS[j][1], g.genes[j]));
            }
        }
        g.fitness = Double.NEGATIVE_INFINITY; // Mark for re-evaluation
    }

    private Genome[] selectBest(Genome[] pool, int n) {
        java.util.Arrays.sort(pool, (a, b) -> Double.compare(b.fitness, a.fitness));
        Genome[] best = new Genome[n];
        System.arraycopy(pool, 0, best, 0, n);
        return best;
    }

    /** Decode genome to a readable hyperparameter summary */
    public static String decodeGenome(Genome g) {
        return String.format(
            "DE[F=%.3f CR=%.3f] QPSO[β=%.3f] SA[T=%.2f cool=%.4f H=%.2e] RL[lr=%.3f εdecay=%.4f]",
            g.genes[0], g.genes[1], g.genes[2],
            g.genes[3], g.genes[4], g.genes[5],
            g.genes[6], g.genes[7]
        );
    }
}
