package org.example.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.enums.Position;
import org.example.model.AttributeValues;
import org.example.model.PlayerCard;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChemistryCalculator {

    private static final Map<String, Double> statsMultPL;
    private static final Map<String, Double> statsMultGK;
    private static final Map<String, Map<String, Double>> csStatsPL;
    private static final Map<String, Map<String, Double>> csStatsGK;
    private static final Map<String, List<String>> statsMapPL = new HashMap<>();
    private static final Map<String, List<String>> statsMapGK = new HashMap<>();
    private static final Map<Position, Map<String, Double>> posStatsFactor;

    static {
        List<String> stats = List.of("acceleration", "sprintspeed");
        statsMapPL.put("pace", stats);
        stats = List.of("agility","balance","reactions","ballcontrol","dribbling","composure");
        statsMapPL.put("dribblingp", stats);
        stats = List.of("positioning","finishing","shotpower","longshotsaccuracy","volleys","penalties");
        statsMapPL.put("shooting", stats);
        stats = List.of("vision","crossing","freekickaccuracy","shortpassing","longpassing","curve");
        statsMapPL.put("passing", stats);
        stats = List.of("interceptions","headingaccuracy","marking","standingtackle","slidingtackle");
        statsMapPL.put("defending", stats);
        stats = List.of("jumping","stamina","strength","aggression");
        statsMapPL.put("heading", stats);

        stats = List.of("gkdiving");
        statsMapGK.put("gkdiving", stats);
        stats = List.of("gkkicking");
        statsMapGK.put("gkkicking", stats);
        stats = List.of("gkhandling");
        statsMapGK.put("gkhandling", stats);
        stats = List.of("gkreflexes");
        statsMapGK.put("gkreflexes", stats);
        stats = List.of("gkpositioning");
        statsMapGK.put("gkpositioning", stats);
        stats = List.of("acceleration", "sprintspeed");
        statsMapGK.put("speed", stats);

        ObjectMapper objectMapper = new ObjectMapper();

        File file = new File("./src/main/java/org/example/resources/statsMultPL.json");
        TypeReference<Map<String, Double>> typeRef1 = new TypeReference<>() {};
        try {
            statsMultPL = objectMapper.readValue(file, typeRef1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        file = new File("./src/main/java/org/example/resources/statsMultGK.json");
        try {
            statsMultGK = objectMapper.readValue(file, typeRef1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        file = new File("./src/main/java/org/example/resources/csStats.json");
        TypeReference<Map<String, Map<String, Double>>> typeRef2 = new TypeReference<>() {};
        try {
            csStatsPL = objectMapper.readValue(file, typeRef2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        file = new File("./src/main/java/org/example/resources/csStatsGK.json");
        try {
            csStatsGK = objectMapper.readValue(file, typeRef2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        file = new File("./src/main/java/org/example/resources/posStatsFactor.json");
        TypeReference<Map<Position, Map<String, Double>>> typeRef3 = new TypeReference<>() {};
        try {
            posStatsFactor = objectMapper.readValue(file, typeRef3);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Integer> createChemStyleStats(String type, Map<String, AttributeValues> baseStats, Integer playerChem, boolean isGK) {
        Map<String, Map<String, Double>> csStats;
        Map<String, Double> statsMult;
        Map<String, List<String>> statsMap;
        Map<String, Integer> calculatedChemStats = new HashMap<>();

        if (isGK) {
            csStats = csStatsGK;
            statsMult = statsMultGK;
            statsMap = statsMapGK;
        } else {
            csStats = csStatsPL;
            statsMult = statsMultPL;
            statsMap = statsMapPL;
        }
        if (type != null && type.length() > 0) {
            type = type.toLowerCase();
        }
        if (!csStats.containsKey(type)) {
            return calculatedChemStats;
        }

        for (Map.Entry<String, AttributeValues> stat : baseStats.entrySet()) {
            if ((isGK || !stat.getValue().getType().equals("main")) && stat.getValue().getChem_change() == 1) {
                Double diffStat = 0.0;
                Double diffStatMod = csStats.get(type).get(stat.getKey());
                if (diffStatMod == null) {
                    continue;
                }
                switch (playerChem) {
                    case 1 -> diffStat = diffStatMod;
                    case 2 -> {
                        if (diffStatMod == 1) diffStat = 2.0;
                        if (diffStatMod == 2) diffStat = 4.0;
                        if (diffStatMod == 3) diffStat = 8.0;
                    }
                    case 3 -> {
                        if (diffStatMod == 1) diffStat = 4.0;
                        if (diffStatMod == 2) diffStat = 8.0;
                        if (diffStatMod == 3) diffStat = 15.0;
                    }
                }
                Integer currentStat = stat.getValue().getValue();
                int totalStat = (int) (currentStat + diffStat);
                if (totalStat > 99) {
                    totalStat = 99;
                }
                calculatedChemStats.put(stat.getKey(), totalStat);
            }
        }

        for (Map.Entry<String, List<String>> statEntry : statsMap.entrySet()) {
            double totalScore = 0.0;
            for (String statName : statEntry.getValue()) {
                totalScore += calculatedChemStats.get(statName) * statsMult.get(statName);
            }
            int baseScore = baseStats.values().stream().filter(a -> a.getId().equals(statEntry.getKey())).findFirst().get().getValue();
            int diffScore = 0;
            if (playerChem > 0) {
                diffScore = roundHalf(totalScore - baseScore);
                if (diffScore < 0) {
                    diffScore = 0;
                }
            }
            calculatedChemStats.put(statEntry.getKey(), baseScore + diffScore);
        }
        return calculatedChemStats;
    }

    public static double getPlayerRatingPerPosition(Map<String, Integer> baseStats, Position position) {
        Map<String, Double> posAttributeFactors = posStatsFactor.get(position);
        double finalRating = 0.0;
        for (Map.Entry<String, Double> attribute : posAttributeFactors.entrySet()) {
            finalRating += attribute.getValue() * baseStats.get(attribute.getKey());
        }
        return finalRating;
    }

    public static int roundHalf(double num) {
        if (num % 1 >= 0.5) {
            return (int) Math.round(num);
        } else {
            return (int) Math.floor(num);
        }
    }
}
