package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class GeneticAlgorithm {
    private static final int POPULATION_SIZE = 50;
    private static final int MAX_GENERATIONS = 100;
    private static final double MUTATION_RATE = 0.1;

    private static final int TARGET_VALUE = 100;
    private static final int GENOME_LENGTH = 8;

    public static void main(String[] args) {
        List<Chromosome> population = initializePopulation();

        int generation = 0;
        while (generation < MAX_GENERATIONS) {
            evaluatePopulation(population);
            System.out.println("Generation: " + generation + "  Best Fitness: " + getBestFitness(population));

            if (isTerminationConditionMet(population)) {
                break;
            }

            List<Chromosome> newPopulation = new ArrayList<>();
            while (newPopulation.size() < POPULATION_SIZE) {
                Chromosome parent1 = selectParent(population);
                Chromosome parent2 = selectParent(population);

                Chromosome offspring = crossover(parent1, parent2);
                mutate(offspring);

                newPopulation.add(offspring);
            }

            population = newPopulation;
            generation++;
        }

        Chromosome bestChromosome = getBestChromosome(population);
        System.out.println("Best Solution: " + Arrays.toString(bestChromosome.getGenes()));
    }

    private static List<Chromosome> initializePopulation() {
        List<Chromosome> population = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            Chromosome chromosome = new Chromosome();
            chromosome.initialize();
            population.add(chromosome);
        }
        return population;
    }

    private static void evaluatePopulation(List<Chromosome> population) {
        for (Chromosome chromosome : population) {
            int fitness = calculateFitness(chromosome);
            chromosome.setFitness(fitness);
        }
    }

    private static int calculateFitness(Chromosome chromosome) {
        int sum = 0;
        for (int gene : chromosome.getGenes()) {
            sum += gene;
        }
        return Math.abs(TARGET_VALUE - sum);
    }

    private static boolean isTerminationConditionMet(List<Chromosome> population) {
        return getBestFitness(population) == 0;
    }

    private static Chromosome selectParent(List<Chromosome> population) {
        Random random = new Random();
        int index = random.nextInt(population.size());
        return population.get(index);
    }

    private static Chromosome crossover(Chromosome parent1, Chromosome parent2) {
        Chromosome offspring = new Chromosome();

        int crossoverPoint = new Random().nextInt(GENOME_LENGTH);
        for (int i = 0; i < GENOME_LENGTH; i++) {
            if (i < crossoverPoint) {
                offspring.setGene(i, parent1.getGene(i));
            } else {
                offspring.setGene(i, parent2.getGene(i));
            }
        }

        return offspring;
    }

    private static void mutate(Chromosome chromosome) {
        Random random = new Random();
        for (int i = 0; i < GENOME_LENGTH; i++) {
            if (random.nextDouble() < MUTATION_RATE) {
                chromosome.setGene(i, random.nextInt(10));
            }
        }
    }

    private static int getBestFitness(List<Chromosome> population) {
        int bestFitness = Integer.MAX_VALUE;
        for (Chromosome chromosome : population) {
            if (chromosome.getFitness() < bestFitness) {
                bestFitness = chromosome.getFitness();
            }
        }
        return bestFitness;
    }

    private static Chromosome getBestChromosome(List<Chromosome> population) {
        Chromosome bestChromosome = null;
        int bestFitness = Integer.MAX_VALUE;
        for (Chromosome chromosome : population) {
            if (chromosome.getFitness() < bestFitness) {
                bestChromosome = chromosome;
                bestFitness = chromosome.getFitness();
            }
        }
        return bestChromosome;
    }

    static class Chromosome {
        private int[] genes;
        private int fitness;

        public Chromosome() {
            this.genes = new int[GENOME_LENGTH];
            this.fitness = 0;
        }

        public void initialize() {
            Random random = new Random();
            for (int i = 0; i < GENOME_LENGTH; i++) {
                genes[i] = random.nextInt(10);
            }
        }

        public int getGene(int index) {
            return genes[index];
        }

        public void setGene(int index, int value) {
            genes[index] = value;
        }

        public int[] getGenes() {
            return genes;
        }

        public int getFitness() {
            return fitness;
        }

        public void setFitness(int fitness) {
            this.fitness = fitness;
        }
    }
}
