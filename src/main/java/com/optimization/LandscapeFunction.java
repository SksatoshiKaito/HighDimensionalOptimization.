package com.optimization;

/**
 * Step 2: Objective Function
 * A 2D function: f(x, y) = -(x^2 + y^2) + 10 + 2*sin(3x) + 2*sin(3y)
 * Global max is near (0,0) and local maxima exist due to sine waves.
 */
public class LandscapeFunction implements ObjectiveFunction {
    @Override
    public double evaluate(Vector v) {
        if (v.getDimension() != 2) {
            throw new IllegalArgumentException("LandscapeFunction is 2-dimensional.");
        }
        double x = v.get(0);
        double y = v.get(1);
        
        // Base parabola + sine waves to create local peaks
        return -(x * x + y * y) + 10.0 + 2.0 * Math.sin(3.0 * x) + 2.0 * Math.sin(3.0 * y);
    }

    @Override
    public int getDimension() {
        return 2;
    }
}
