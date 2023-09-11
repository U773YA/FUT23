package org.example;

import java.util.Arrays;
import java.util.List;

public class TeamRatingCalculate {

    public static void main(String[] args) {
        List<Integer> players = Arrays.asList(93,92,91,90,86,84,84, 78,78,78,78);
        System.out.println(getTeamRating(players));
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
}