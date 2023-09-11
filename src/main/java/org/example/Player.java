package org.example;

public class Player {
    private String name;
    private String position;
    private int score;
    private double fitness;

    public Player(String name, String position, int score) {
        this.name = name;
        this.position = position;
        this.score = score;
        this.fitness = 0.0;
    }

    public String getName() {
        return name;
    }

    public String getPosition() {
        return position;
    }

    public int getScore() {
        return score;
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }
}
