package com.optimization;

/**
 * Level 2: Generalization - The Rastrigin Function
 * This function can scale to any dimension. It has many local maxima.
 * Since our engine looks for maxima, we will invert it (usually its global minimum is 0).
 * f(x) = - (10 * n + sum(x_i^2 - 10 * cos(2 * pi * x_i)))
 * Global maximum is 0 at (0, 0, ..., 0).
 */
public class HighDimensionalRastrigin implements ObjectiveFunction {
    private final int dimension;

    public HighDimensionalRastrigin(int dimension) {
        this.dimension = dimension;
    }

    @Override
    public double evaluate(Vector v) {
        if (v.getDimension() != dimension) {
            throw new IllegalArgumentException("Vector dimension mismatch");
        }
        
        double sum = 0.0;
        for (int i = 0; i < dimension; i++) {
            double xi = v.get(i);
            sum += (xi * xi) - 10.0 * Math.cos(2.0 * Math.PI * xi);
        }
        
        return -(10.0 * dimension + sum);
    }

    @Override
    public int getDimension() { return dimension; }

    @Override
    public String toString() { return "Rastrigin-" + dimension + "D"; }
}
