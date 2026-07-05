package com.optimization;

/**
 * Ackley Function: A classic benchmark with a nearly flat outer region
 * and a large hole at the center. Global maximum is 0 at the origin.
 */
public class AckleyFunction implements ObjectiveFunction {
    private final int dimension;
    private static final double A = 20.0;
    private static final double B = 0.2;
    private static final double C = 2 * Math.PI;

    public AckleyFunction(int dimension) {
        this.dimension = dimension;
    }

    @Override
    public double evaluate(Vector v) {
        double sumSq = 0.0;
        double sumCos = 0.0;
        for (int i = 0; i < dimension; i++) {
            double xi = v.get(i);
            sumSq += xi * xi;
            sumCos += Math.cos(C * xi);
        }
        double term1 = -A * Math.exp(-B * Math.sqrt(sumSq / dimension));
        double term2 = -Math.exp(sumCos / dimension);
        // Negate so global maximum = 0 (engine maximizes)
        return -(term1 + term2 + A + Math.E);
    }

    @Override
    public int getDimension() { return dimension; }

    @Override
    public String toString() { return "Ackley-" + dimension + "D"; }
}
