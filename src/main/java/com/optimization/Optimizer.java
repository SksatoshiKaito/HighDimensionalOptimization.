package com.optimization;

import java.util.Random;

/**
 * Step 3 & 4: Gradient Ascent Algorithm & Simulated Annealing
 */
public class Optimizer {
    private final ObjectiveFunction function;
    private final double learningRate;
    private final double h; // Small step for numerical derivative
    private final Random random;

    public Optimizer(ObjectiveFunction function, double learningRate, double h) {
        this.function = function;
        this.learningRate = learningRate;
        this.h = h;
        this.random = new Random();
    }

    /**
     * Level 3 Memory Optimization: In-place gradient calculation to prevent huge memory pressure.
     */
    public void calculateGradientInPlace(Vector current, Vector gradientOutput) {
        int dim = current.getDimension();
        
        for (int i = 0; i < dim; i++) {
            double originalValue = current.get(i);
            
            // Forward step
            current.set(i, originalValue + h);
            double forwardEval = function.evaluate(current);
            
            // Backward step
            current.set(i, originalValue - h);
            double backwardEval = function.evaluate(current);
            
            // Restore original value
            current.set(i, originalValue);
            
            double derivative = (forwardEval - backwardEval) / (2 * h);
            gradientOutput.set(i, derivative);
        }
    }

    /**
     * Optimizes starting from an initial point.
     * Includes Simulated Annealing logic (decaying random noise).
     */
    public Vector optimize(Vector startPoint, int maxIterations, double initialTemperature, double coolingRate) {
        Vector current = startPoint.copy();
        Vector gradient = new Vector(current.getDimension());
        double temperature = initialTemperature;

        for (int iter = 0; iter < maxIterations; iter++) {
            // Memory Optimized Gradient Calculation
            calculateGradientInPlace(current, gradient);
            
            // Memory Optimized Update
            current.addInPlace(gradient, learningRate);
            
            if (temperature > 0.0001) {
                current.addNoiseInPlace(temperature, random);
            }
            
            // Cool down
            temperature *= coolingRate;
        }
        
        return current;
    }
}
