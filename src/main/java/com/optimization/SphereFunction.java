package com.optimization;

/**
 * Sphere Function: Simplest convex benchmark.
 * f(x) = -sum(x_i^2). Global maximum = 0 at origin.
 */
public class SphereFunction implements ObjectiveFunction {
    private final int dimension;

    public SphereFunction(int dimension) { this.dimension = dimension; }

    @Override
    public double evaluate(Vector v) {
        double sum = 0.0;
        for (int i = 0; i < dimension; i++) sum += v.get(i) * v.get(i);
        return -sum;
    }

    @Override
    public int getDimension() { return dimension; }

    @Override
    public String toString() { return "Sphere-" + dimension + "D"; }
}
