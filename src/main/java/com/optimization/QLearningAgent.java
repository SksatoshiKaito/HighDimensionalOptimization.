package com.optimization;

import java.util.Random;

/**
 * Level 9: Reinforcement Learning Hyper-Heuristic (Q-Learning Agent).
 * 
 * Instead of manually choosing an optimization algorithm (DE, QPSO, SA),
 * this AI agent learns which mathematical physics engine to apply
 * based on the current state of the optimization landscape.
 */
public class QLearningAgent {
    
    // States
    public static final int STATE_IMPROVING = 0;
    public static final int STATE_STAGNANT = 1;
    public static final int STATE_CONVERGED = 2;
    private static final int NUM_STATES = 3;
    
    // Actions (Algorithms to apply)
    public static final int ACTION_DE = 0;    // Differential Evolution (Exploration)
    public static final int ACTION_QPSO = 1;  // Quantum Tunneling (Escaping Local Minima)
    public static final int ACTION_SA = 2;    // Simulated Annealing (Fine-tuning Exploitation)
    private static final int NUM_ACTIONS = 3;
    
    // Q-Table
    private final double[][] qTable;
    
    // RL Hyperparameters
    private static final double LEARNING_RATE = 0.1;
    private static final double DISCOUNT_FACTOR = 0.9;
    private double epsilon = 1.0; // Exploration rate
    private static final double EPSILON_DECAY = 0.99;
    private static final double EPSILON_MIN = 0.05;
    
    private final Random random;
    
    public QLearningAgent(long seed) {
        this.random = new Random(seed);
        this.qTable = new double[NUM_STATES][NUM_ACTIONS];
        
        // Initialize Q-Table with small random values
        for (int s = 0; s < NUM_STATES; s++) {
            for (int a = 0; a < NUM_ACTIONS; a++) {
                qTable[s][a] = random.nextDouble() * 0.1;
            }
        }
    }
    
    /**
     * Epsilon-Greedy Action Selection
     */
    public int selectAction(int state) {
        if (random.nextDouble() < epsilon) {
            // Explore: Random action
            return random.nextInt(NUM_ACTIONS);
        } else {
            // Exploit: Best known action for this state
            int bestAction = 0;
            double bestValue = qTable[state][0];
            for (int a = 1; a < NUM_ACTIONS; a++) {
                if (qTable[state][a] > bestValue) {
                    bestValue = qTable[state][a];
                    bestAction = a;
                }
            }
            return bestAction;
        }
    }
    
    /**
     * Update Q-Table using the Bellman Equation
     */
    public void learn(int state, int action, double reward, int nextState) {
        // Find max Q value for the next state
        double maxNextQ = qTable[nextState][0];
        for (int a = 1; a < NUM_ACTIONS; a++) {
            if (qTable[nextState][a] > maxNextQ) {
                maxNextQ = qTable[nextState][a];
            }
        }
        
        // Bellman Equation
        double currentQ = qTable[state][action];
        double newQ = currentQ + LEARNING_RATE * (reward + DISCOUNT_FACTOR * maxNextQ - currentQ);
        qTable[state][action] = newQ;
        
        // Decay Epsilon
        if (epsilon > EPSILON_MIN) {
            epsilon *= EPSILON_DECAY;
        }
    }
    
    /**
     * Determine current state based on improvement history
     */
    public int determineState(double currentFitness, double previousFitness, double globalBest) {
        // Max value is 0.0
        if (Math.abs(currentFitness - 0.0) < 1e-4 || Math.abs(globalBest - 0.0) < 1e-4) {
            return STATE_CONVERGED;
        }
        
        double improvement = currentFitness - previousFitness;
        if (improvement > 1e-6) {
            return STATE_IMPROVING;
        } else {
            return STATE_STAGNANT;
        }
    }
}
