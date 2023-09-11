package org.example;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class TeamMakerWithRating {

    private static final Map<Integer, Integer> availablePlayers = new HashMap<>();
    private static List<Integer> players = new ArrayList<>();
    private static double ratingToAchieve = 88.0;
    private static int newPlayers = 0;
    private static long totalTeams = 0;
    private static long currentTeamCounter = 0;
    private static long startTime = 0;
    public static void main(String[] args) {
        availablePlayers.put(75,38);
        availablePlayers.put(76,16);
        availablePlayers.put(77,44);
        availablePlayers.put(78,78);
        availablePlayers.put(79,84);
        availablePlayers.put(80,55);
        availablePlayers.put(81,8);
        availablePlayers.put(82,10);
        availablePlayers.put(83,4);
        availablePlayers.put(84,19);
        availablePlayers.put(85,6);
        availablePlayers.put(86,12);
        availablePlayers.put(87,11);
        availablePlayers.put(88,7);
        availablePlayers.put(89,9);
        availablePlayers.put(90,13);
        availablePlayers.put(91,23);
        availablePlayers.put(92,16);
        availablePlayers.put(93,11);
        availablePlayers.put(94,14);
        availablePlayers.put(95,24);

        List<Integer> ratingsToTry = IntStream.rangeClosed(74, 95)
                .boxed()
                .toList();
        players = List.of(93,91,89,85);
        newPlayers = 11 - players.size();
        totalTeams = (long) Math.pow(ratingsToTry.size(), newPlayers);

        startTime = System.nanoTime();
        List<Map<Integer, Integer>> permutations = generatePermutations(ratingsToTry, 11 - players.size());
        permutations.sort(new WeightedSumComparatorAscending());
//        for (Map<Integer, Integer> frequencyMap : permutations) {
//            for (Map.Entry<Integer, Integer> entry : frequencyMap.entrySet()) {
//                    System.out.print(entry.getKey() + "-" + entry.getValue() + " ");
//            }
//            System.out.println();
//        }

        writeListToFile(permutations, "output.txt");
    }

    public static void writeListToFile(List<Map<Integer, Integer>> list, String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (Map<Integer, Integer> element : list) {
                for(Map.Entry<Integer, Integer> entry : element.entrySet()) {
                    writer.write(entry.getKey() + "-" + entry.getValue() + " ");
                }
                writer.newLine(); // Add a newline after each element
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static double getTeamRating(List<Integer> players) {
        int sumRatings = 0;
        for (int player : players) {
            sumRatings += player;
        }
        double avg = sumRatings / 11.0;
        double doubleRating = sumRatings;
        for (int player : players) {
            if (player > avg) {
                doubleRating += player - avg;
            }
        }
        long roundedTotal = Math.round(doubleRating);
        double rating = roundedTotal / 11.0;
        return Math.floor(rating);

    }

    public static List<Map<Integer, Integer>> generatePermutations(List<Integer> numbers, int k) {
        List<Map<Integer, Integer>> permutations = new ArrayList<>();
        generatePermutationsHelper(numbers, k, new ArrayList<>(), permutations);
        return permutations;
    }

    private static double calculateETA(double percentage) {
        double percentageLeft = 100 - percentage;
        long elapsedTime = System.nanoTime() - startTime;
        return ((percentageLeft * elapsedTime) / percentage) / 1000000000;
    }

    private static void generatePermutationsHelper(List<Integer> numbers, int k, List<Integer> current, List<Map<Integer, Integer>> permutations) {
        if (k == 0) {
            currentTeamCounter++;
            double percentage = currentTeamCounter / (double) totalTeams * 100.0;
            double eta = calculateETA(percentage);
            if (currentTeamCounter % 10000000 == 0) {
                System.out.println("Percentage completed : " + percentage + " Teams : " + currentTeamCounter + " ETA: " + eta +" s");
            }
            List<Integer> newTeam = new ArrayList<>(players);
            newTeam.addAll(current);
            double rating = getTeamRating(newTeam);
            if (rating == ratingToAchieve) {
                Map<Integer, Integer> frequencyMap = new HashMap<>();
                // Count the frequency of each element in the list
                for (int element : newTeam.subList(newTeam.size() - newPlayers, newTeam.size())) {
                    frequencyMap.put(element, frequencyMap.getOrDefault(element, 0) + 1);
                }
                boolean isValid = availablePlayers
                        .keySet()
                        .stream()
                        .allMatch(key-> availablePlayers.getOrDefault(key, 0) >= frequencyMap.getOrDefault(key, 0));
                if (isValid) {
                    permutations.add(frequencyMap);
                }
            }
            return;
        }

        for (int i = 0; i < numbers.size(); i++) {
            current.add(numbers.get(i));
            generatePermutationsHelper(numbers, k - 1, current, permutations);
            current.remove(current.size() - 1); // Backtrack to explore other possibilities
        }
    }

    static class WeightedSumComparatorAscending implements Comparator<Map<Integer, Integer>> {
        @Override
        public int compare(Map<Integer, Integer> map1, Map<Integer, Integer> map2) {
            int weightedSum1 = calculateWeightedSum(map1);
            int weightedSum2 = calculateWeightedSum(map2);
            // Compare based on weighted sum in ascending order
            return Integer.compare(weightedSum1, weightedSum2);
        }
        private int calculateWeightedSum(Map<Integer, Integer> map) {
            int sum = 0;
            for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                sum += entry.getKey() * entry.getValue();
            }
            return sum;
        }
    }
}
