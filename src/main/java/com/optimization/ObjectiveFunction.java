package com.optimization;

/**
 * Interface representing the objective function to be maximized.
 */
public interface ObjectiveFunction {
    double evaluate(Vector v);
    int getDimension();
}
