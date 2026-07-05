package com.optimization;

import java.util.Random;

/**
 * A lightweight Multi-Layer Perceptron (MLP) built from scratch in Java.
 * Used as a Surrogate Model to approximate the Objective Function landscape.
 */
public class NeuralNetwork {
    private final int inputSize;
    private final int hiddenSize;
    
    // Weights and biases
    private final double[][] w1; // Input to Hidden
    private final double[] b1;   // Hidden Biases
    private final double[] w2;   // Hidden to Output
    private double b2;           // Output Bias
    
    // Gradients & Cache for Backpropagation
    private final double[] hiddenLayer;
    
    private static final double LEARNING_RATE = 0.01;
    
    public NeuralNetwork(int inputSize, int hiddenSize, long seed) {
        this.inputSize = inputSize;
        this.hiddenSize = hiddenSize;
        Random random = new Random(seed);
        
        w1 = new double[inputSize][hiddenSize];
        b1 = new double[hiddenSize];
        w2 = new double[hiddenSize];
        
        hiddenLayer = new double[hiddenSize];
        
        // He Initialization
        double scale1 = Math.sqrt(2.0 / inputSize);
        for (int i = 0; i < inputSize; i++) {
            for (int j = 0; j < hiddenSize; j++) {
                w1[i][j] = random.nextGaussian() * scale1;
            }
        }
        
        double scale2 = Math.sqrt(2.0 / hiddenSize);
        for (int j = 0; j < hiddenSize; j++) {
            w2[j] = random.nextGaussian() * scale2;
            b1[j] = 0.0;
        }
        b2 = 0.0;
    }
    
    /**
     * ReLU Activation
     */
    private double relu(double x) {
        return Math.max(0, x);
    }
    
    /**
     * Derivative of ReLU
     */
    private double reluDerivative(double x) {
        return x > 0 ? 1.0 : 0.0;
    }
    
    /**
     * Forward pass to predict the fitness value.
     */
    public double predict(Vector input) {
        // Input to Hidden
        for (int j = 0; j < hiddenSize; j++) {
            double sum = b1[j];
            for (int i = 0; i < inputSize; i++) {
                sum += input.get(i) * w1[i][j];
            }
            hiddenLayer[j] = relu(sum);
        }
        
        // Hidden to Output (Linear Activation for regression)
        double output = b2;
        for (int j = 0; j < hiddenSize; j++) {
            output += hiddenLayer[j] * w2[j];
        }
        
        return output;
    }
    
    /**
     * Train the network using Mean Squared Error (MSE) and Backpropagation.
     */
    public void train(Vector input, double actualValue) {
        double predicted = predict(input);
        
        // MSE Loss derivative with respect to output
        // Loss = (predicted - actual)^2
        // dL/dOutput = 2 * (predicted - actual)
        double dLoss_dOutput = 2 * (predicted - actualValue);
        
        // --- Backpropagation ---
        
        // Gradients for Output Layer
        double dLoss_db2 = dLoss_dOutput;
        double[] dLoss_dw2 = new double[hiddenSize];
        for (int j = 0; j < hiddenSize; j++) {
            dLoss_dw2[j] = dLoss_dOutput * hiddenLayer[j];
        }
        
        // Gradients for Hidden Layer
        double[] dLoss_dHidden = new double[hiddenSize];
        for (int j = 0; j < hiddenSize; j++) {
            // Recompute pre-activation sum for derivative
            double preAct = b1[j];
            for (int i = 0; i < inputSize; i++) {
                preAct += input.get(i) * w1[i][j];
            }
            
            dLoss_dHidden[j] = dLoss_dOutput * w2[j] * reluDerivative(preAct);
        }
        
        // Update Output Layer Weights & Biases
        b2 -= LEARNING_RATE * dLoss_db2;
        for (int j = 0; j < hiddenSize; j++) {
            w2[j] -= LEARNING_RATE * dLoss_dw2[j];
        }
        
        // Update Hidden Layer Weights & Biases
        for (int j = 0; j < hiddenSize; j++) {
            b1[j] -= LEARNING_RATE * dLoss_dHidden[j];
            for (int i = 0; i < inputSize; i++) {
                w1[i][j] -= LEARNING_RATE * (dLoss_dHidden[j] * input.get(i));
            }
        }
    }
}
