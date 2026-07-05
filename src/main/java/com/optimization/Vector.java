package com.optimization;

import java.util.Arrays;

/**
 * Step 1 & Level 3: Core Mathematical Vector Design with Memory Optimization
 */
public class Vector {
    private final double[] coordinates;

    public Vector(int dimension) {
        this.coordinates = new double[dimension];
    }

    public Vector(double... coordinates) {
        this.coordinates = Arrays.copyOf(coordinates, coordinates.length);
    }

    public int getDimension() {
        return coordinates.length;
    }

    public double get(int index) {
        return coordinates[index];
    }

    public void set(int index, double value) {
        coordinates[index] = value;
    }

    /**
     * Adds another vector to this vector and returns a new Vector.
     */
    public Vector add(Vector other) {
        if (this.getDimension() != other.getDimension()) {
            throw new IllegalArgumentException("Dimensions must match");
        }
        Vector result = new Vector(this.getDimension());
        for (int i = 0; i < this.getDimension(); i++) {
            result.set(i, this.get(i) + other.get(i));
        }
        return result;
    }

    /**
     * Subtracts another vector from this vector and returns a new Vector.
     */
    public Vector subtract(Vector other) {
        if (this.getDimension() != other.getDimension()) {
            throw new IllegalArgumentException("Dimensions must match");
        }
        Vector result = new Vector(this.getDimension());
        for (int i = 0; i < this.getDimension(); i++) {
            result.set(i, this.get(i) - other.get(i));
        }
        return result;
    }

    /**
     * Multiplies the vector by a scalar value.
     */
    public Vector multiply(double scalar) {
        Vector result = new Vector(this.getDimension());
        for (int i = 0; i < this.getDimension(); i++) {
            result.set(i, this.get(i) * scalar);
        }
        return result;
    }

    /**
     * Computes the dot product of two vectors.
     */
    public double dotProduct(Vector other) {
        if (this.getDimension() != other.getDimension()) {
            throw new IllegalArgumentException("Dimensions must match");
        }
        double sum = 0.0;
        for (int i = 0; i < this.getDimension(); i++) {
            sum += this.get(i) * other.get(i);
        }
        return sum;
    }

    /**
     * Returns a copy of the vector.
     */
    public Vector copy() {
        return new Vector(this.coordinates);
    }

    /**
     * Level 3 Memory Optimization: Adds values directly to avoid creating new objects.
     */
    public void addInPlace(Vector other, double scalar) {
        for (int i = 0; i < coordinates.length; i++) {
            coordinates[i] += other.get(i) * scalar;
        }
    }
    
    /**
     * Adds random noise in place to save memory allocations.
     */
    public void addNoiseInPlace(double temperature, java.util.Random random) {
        for (int i = 0; i < coordinates.length; i++) {
            coordinates[i] += (random.nextDouble() * 2 - 1) * temperature;
        }
    }

    @Override
    public String toString() {
        if (coordinates.length > 5) {
            return String.format("(%.4f, %.4f, ... %d more dims ...)", coordinates[0], coordinates[1], coordinates.length - 2);
        }
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < coordinates.length; i++) {
            sb.append(String.format("%.4f", coordinates[i]));
            if (i < coordinates.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}
