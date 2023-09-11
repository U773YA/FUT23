package org.example;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.example.enums.Position;
import org.example.model.CardInput;
import org.example.model.CardScore;
import org.example.model.Manager;
import org.example.model.PlayerCard;
import org.example.model.Tactic;
import org.example.model.TeamPlayer;
import org.example.model.VariationTeam;
import org.example.util.ChemistryCalculator;
import org.example.util.InputData;
import org.example.util.Scraper;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.example.enums.Position.CAM;
import static org.example.enums.Position.CB;
import static org.example.enums.Position.CDM;
import static org.example.enums.Position.CF;
import static org.example.enums.Position.CM;
import static org.example.enums.Position.GK;
import static org.example.enums.Position.ST;

public class CustomTeamBuilder extends InputData {

    private static List<VariationTeam> possibleTeams = new ArrayList<>();
    private static final Map<String, List<Manager>> nationManagerMap = new HashMap<>();
    private static final Map<Integer, List<Manager>> leagueManagerMap = new HashMap<>();
    public static Map<Integer, PlayerCard> dbPlayerCardMap = new HashMap<>();

    public static void main(String[] args) throws Exception {
        List<CardInput> wishListPlayers = populateWishList();
        playerCardInputList.addAll(wishListPlayers);
        populatePlayerInput();
        populateTactics();
        populateManagers();
        for (Manager manager : managerList) {
            nationManagerMap.computeIfAbsent(manager.getNation(), k -> new ArrayList<>()).add(manager);
            leagueManagerMap.computeIfAbsent(manager.getLeague(), k -> new ArrayList<>()).add(manager);
        }
        populatePositionMap();
        if (shouldConsiderLoans) {
            populateLoans();
        }

        File file = new File("playerCardMap.json");
        if (file.exists()) {
            Gson gson = new Gson();
            try (Reader reader = new FileReader(file)) {
                dbPlayerCardMap = gson.fromJson(reader, new TypeToken<HashMap<Integer, PlayerCard>>() {
                }.getType());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // handle the case when the file does not exist
            System.out.println("File 'playerCardMap.json' does not exist.");
        }

        Scraper scraper = new Scraper();
        for (CardInput cardInput : playerCardInputList) {
            PlayerCard playerCard;
            if (dbPlayerCardMap.containsKey(cardInput.getCardId())) {
                playerCard = dbPlayerCardMap.get(cardInput.getCardId());
            } else {
                playerCard = scraper.getCardData(cardInput.getCardId(), cardInput.getPosition());
            }
            if (!playerCard.getNation().equalsIgnoreCase("England")) {
                continue;
            }
            playerCard.setCardInput(cardInput);
            if (!Objects.equals(playerCard.getRating(), playerCard.getCardInput().getRating())) {
                System.out.println("Mismatched rated card: " + playerCard.getCardInput().getName());
            }
            List<CardScore> scoresList = new ArrayList<>();
            chemStyles.forEach(style -> {
                if (playerCard.getCardInput().isChemAttached() && style.equals("basic")) {
                    return;
                }
                Map<String, Integer> chemAttributes = ChemistryCalculator.createChemStyleStats(style, playerCard.getBaseAttributes(), 3, cardInput.getPosition() == GK);
//                System.out.println(style);
                Double score = null;
                try {
                    score = getPlayerScore(cardInput.getPosition(), chemAttributes, playerCard, style);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                scoresList.add(new CardScore(cardInput.getCardId(), score, style, cardInput.getPosition()));
//                for (Position altPos : playerCard.getAltPositions()) {
//                    Double altScore = getPlayerScore(altPos, chemAttributes, playerCard);
//                    if (altScore > score) {
//                        scoresList.add(new CardScore(cardInput.getCardId(), altScore, style, altPos));
//                    }
//                }
            });
            CardScore bestScore = scoresList.stream().sorted(Comparator.comparing(CardScore::getScore).reversed()).limit(1).toList().get(0);
            System.out.println(cardInput.getName() + " -> " + bestScore.getChemStyle() + ", " + bestScore.getScore() + ", " + bestScore.getPosition());
//            if (bestScore.getPosition() != cardInput.getPosition()) {
//                System.out.println("Altering position of player " + cardInput.getName() + " from " + cardInput.getPosition() + " to " + bestScore.getPosition());
//                List<Position> altPositions = playerCard.getAltPositions();
//                altPositions.remove(bestScore.getPosition());
//                altPositions.add(playerCard.getCardInput().getPosition());
//                playerCard.getCardInput().setPosition(bestScore.getPosition());
//            }
            playerCard.chemScores.add(0, getPlayerScore(cardInput.getPosition(), ChemistryCalculator.createChemStyleStats(bestScore.getChemStyle(), playerCard.getBaseAttributes(), 0, cardInput.getPosition() == GK), playerCard, bestScore.getChemStyle()));
            playerCard.chemScores.add(1, getPlayerScore(cardInput.getPosition(), ChemistryCalculator.createChemStyleStats(bestScore.getChemStyle(), playerCard.getBaseAttributes(), 1, cardInput.getPosition() == GK), playerCard, bestScore.getChemStyle()));
            playerCard.chemScores.add(2, getPlayerScore(cardInput.getPosition(), ChemistryCalculator.createChemStyleStats(bestScore.getChemStyle(), playerCard.getBaseAttributes(), 2, cardInput.getPosition() == GK), playerCard, bestScore.getChemStyle()));
            playerCard.chemScores.add(3, bestScore.getScore());
            playerCard.chemistry = bestScore.getChemStyle();
//            if (bestScore.getScore() >= 85.0) {
            playerPositionMap.computeIfAbsent(cardInput.getPosition(), k -> new ArrayList<>()).add(bestScore);
//            }
            playerCardMap.put(cardInput.getCardId(), playerCard);
        }

//        for (List<CardScore> list : playerPositionMap.values()) {
//            list.sort((c1, c2) -> -Double.compare(c1.getScore(), c2.getScore()));
//            if (list.size() > playersToConsider) {
//                list.subList(playersToConsider, list.size()).clear();
//            }
//        }

        if (shouldConsiderPlayerPositionMods) {
            List<CardScore> playerScores = playerPositionMap.values().stream().flatMap(List::stream).toList();
            for (CardScore playerScore : playerScores) {
                PlayerCard playerCard = playerCardMap.get(playerScore.getCardId());
                if (playerCard.getCardInput().getCardId().equals(53222)) {
                    System.out.println();
                }
                for (int i = 0; i < playerCard.getAltPositions().size(); i++) {
                    Position altPos = playerCard.getAltPositions().get(i);
                    List<CardScore> scoresList = new ArrayList<>();
                    chemStyles.forEach(style -> {
                        if (playerCard.getCardInput().isChemAttached() && style.equals("basic")) {
                            return;
                        }
                        Map<String, Integer> chemAttributes = ChemistryCalculator.createChemStyleStats(style, playerCard.getBaseAttributes(), 3, altPos == GK);
                        Double score;
                        try {
                            score = getPlayerScore(altPos, chemAttributes, playerCard, style);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        scoresList.add(new CardScore(playerScore.getCardId(), score, style, altPos));
                    });
                    CardScore bestScore = scoresList.stream().sorted(Comparator.comparing(CardScore::getScore).reversed()).limit(1).toList().get(0);
                    PlayerCard newPlayerCard = new PlayerCard();
                    CardInput playerCardInput = playerCard.getCardInput();
                    Integer newCardId = playerCardInput.getCardId() + (i + 1) * 100000;
                    bestScore.setCardId(newCardId);
                    CardInput cardInput = new CardInput(newCardId, playerCardInput.getName(), altPos, playerCardInput.getRating());
                    newPlayerCard.setCardInput(cardInput);
                    newPlayerCard.setNation(playerCard.getNation());
                    newPlayerCard.setSkills(playerCard.getSkills());
                    newPlayerCard.setWeakFoot(playerCard.getWeakFoot());
                    newPlayerCard.setAttackingWorkRate(playerCard.getAttackingWorkRate());
                    newPlayerCard.setDefensiveWorkRate(playerCard.getDefensiveWorkRate());
                    newPlayerCard.setId(playerCard.getId());
                    newPlayerCard.setClubId(playerCard.getClubId());
                    newPlayerCard.setLeagueId(playerCard.getLeagueId());
                    newPlayerCard.setRating(playerCard.getRating());
                    newPlayerCard.setBaseAttributes(playerCard.getBaseAttributes());
                    newPlayerCard.accelerateMap = playerCard.accelerateMap;
                    newPlayerCard.setHeight(playerCard.getHeight());
                    newPlayerCard.setBodyType(playerCard.getBodyType());
                    newPlayerCard.chemScores.add(0, getPlayerScore(altPos, ChemistryCalculator.createChemStyleStats(bestScore.getChemStyle(), newPlayerCard.getBaseAttributes(), 0, altPos == GK), newPlayerCard, bestScore.getChemStyle()));
                    newPlayerCard.chemScores.add(1, getPlayerScore(altPos, ChemistryCalculator.createChemStyleStats(bestScore.getChemStyle(), newPlayerCard.getBaseAttributes(), 1, altPos == GK), newPlayerCard, bestScore.getChemStyle()));
                    newPlayerCard.chemScores.add(2, getPlayerScore(altPos, ChemistryCalculator.createChemStyleStats(bestScore.getChemStyle(), newPlayerCard.getBaseAttributes(), 2, altPos == GK), newPlayerCard, bestScore.getChemStyle()));
                    newPlayerCard.chemScores.add(3, bestScore.getScore());
                    newPlayerCard.chemistry = bestScore.getChemStyle();
                    playerPositionMap.computeIfAbsent(altPos, k -> new ArrayList<>()).add(bestScore);
                    playerCardMap.put(newCardId, newPlayerCard);
                }
            }

            for (List<CardScore> list : playerPositionMap.values()) {
                Position position = list.get(0).getPosition();
                list.sort(Comparator.comparing(CardScore::getScore).reversed());

                Set<Integer> playersToRemove = new HashSet<>();
                for (int i = 0; i < list.size(); i++) {
                    for (int j = 0; j < list.size(); j++) {
                        if (Objects.equals(playerCardMap.get(list.get(i).getCardId()).getId(), playerCardMap.get(list.get(j).getCardId()).getId())) {
                            if (list.get(i).getScore() > list.get(j).getScore()) {
                                playersToRemove.add(j);
                            } else if (list.get(i).getScore() < list.get(j).getScore()) {
                                playersToRemove.add(i);
                            }
                        }
                    }
                }
                Set<Integer> sortedSet = new TreeSet<>(Comparator.reverseOrder());
                sortedSet.addAll(playersToRemove);
                list = removeElementsByIndex(list, sortedSet);

                double bestPlayerScore = list.get(0).getScore();
                list.forEach(cardScore -> cardScore.setScore((cardScore.getScore() / bestPlayerScore) * 100.0));

                playerPositionMap.put(position, list);
            }
//        }

//        List<CardScore> cardScoreList = new ArrayList<>(playerPositionMap.values().stream().flatMap(List::stream).toList());
//        cardScoreList.sort(Comparator.comparing(CardScore::getScore).reversed());
//        List<CardScore> sortedCardScoreList = new ArrayList<>(cardScoreList);
//        sortedCardScoreList.subList(76, sortedCardScoreList.size()).clear();
//        playerPositionMap = new HashMap<>();
//        sortedCardScoreList.forEach(cardScore -> {
//            playerPositionMap.computeIfAbsent(cardScore.getPosition(), k -> new ArrayList<>()).add(cardScore);
//        });
//        for (Position position: Arrays.asList(CB, RWB, ST)) {
//            if (playerPositionMap.get(position).size() < 3) {
//                playerPositionMap.get(position).addAll(cardScoreList.stream()
//                        .filter(cardScore -> cardScore.getPosition().equals(position) &&
//                                !playerPositionMap.get(position).stream().map(CardScore::getCardId).toList()
//                                        .contains(cardScore.getCardId()))
//                        .limit(1)
//                        .toList());
//            }
//        }

            for (List<CardScore> list : playerPositionMap.values()) {
                Position position = list.get(0).getPosition();
                if (list.get(0).getPosition().equals(ST)) {
                    System.out.print("");
                }
                list.sort(Comparator.comparing(CardScore::getScore).reversed());

                Set<Integer> playersToRemove = new HashSet<>();
                for (int i = 0; i < list.size(); i++) {
                    for (int j = 0; j < list.size(); j++) {
                        if (Objects.equals(playerCardMap.get(list.get(i).getCardId()).getId(), playerCardMap.get(list.get(j).getCardId()).getId())) {
                            if (list.get(i).getScore() > list.get(j).getScore()) {
                                playersToRemove.add(j);
                            } else if (list.get(i).getScore() < list.get(j).getScore()) {
                                playersToRemove.add(i);
                            }
                        }
                    }
                }
                Set<Integer> sortedSet = new TreeSet<>(Comparator.reverseOrder());
                sortedSet.addAll(playersToRemove);
                list = removeElementsByIndex(list, sortedSet);

                if (list.size() > playersToConsider) {
                    list.subList(playersToConsider, list.size()).clear();
                }

                List<Integer> listPlayers = list.stream()
                        .map(CardScore::getCardId)
                        .toList();

                for (Map.Entry<Integer, PlayerCard> entry : playerCardMap.entrySet()) {
                    int key = entry.getKey();
                    PlayerCard value = entry.getValue();
                    if ((mandatoryPlayers.contains(key % 100000)) && !listPlayers.contains(key) &&
                            value.getCardInput().getPosition().equals(list.get(0).getPosition())) {
                        CardScore cardScore = new CardScore(value.getCardInput().getCardId(), value.chemScores.get(3),
                                value.chemistry, value.getCardInput().getPosition());
                        list.add(cardScore);
                    }
                }

                playerPositionMap.put(position, list);
            }
        }

        System.out.println("\nBest players in each position: ");
        playerPositionMap.forEach((key, value) -> {
            System.out.print(key + " -> ");
            value.forEach(cardScore -> {
                PlayerCard playerCard = playerCardMap.get(cardScore.getCardId());
                System.out.print(playerCard.getCardInput().getName() + " (" + cardScore.getChemStyle() + ") " + "-" + String.format("%.2f", playerCard.chemScores.get(0)) + "," + String.format("%.2f", playerCard.chemScores.get(1)) + "," + String.format("%.2f", playerCard.chemScores.get(2)) + "," + String.format("%.2f", playerCard.chemScores.get(3)) + ", ");
            });
            System.out.println();
            System.out.println();
        });

        getCombinations(playerPositionMap.get(CB).stream().map(CardScore::getCardId).collect(Collectors.toList()), playerPositionMap.get(CB).size(), 2, cb2);
        getCombinations(playerPositionMap.get(CB).stream().map(CardScore::getCardId).collect(Collectors.toList()), playerPositionMap.get(CB).size(), 3, cb3);
        getCombinations(playerPositionMap.get(CM).stream().map(CardScore::getCardId).collect(Collectors.toList()), playerPositionMap.get(CM).size(), 2, cm2);
        getCombinations(playerPositionMap.get(ST).stream().map(CardScore::getCardId).collect(Collectors.toList()), playerPositionMap.get(ST).size(), 2, st2);
        if (playerPositionMap.containsKey(CF)) {
            getCombinations(playerPositionMap.get(CF).stream().map(CardScore::getCardId).collect(Collectors.toList()), playerPositionMap.get(CF).size(), 2, cf2);
        }
        getCombinations(playerPositionMap.get(CDM).stream().map(CardScore::getCardId).collect(Collectors.toList()), playerPositionMap.get(CDM).size(), 2, cdm2);
        getCombinations(playerPositionMap.get(CAM).stream().map(CardScore::getCardId).collect(Collectors.toList()), playerPositionMap.get(CAM).size(), 2, cam2);
        getCombinations(playerPositionMap.get(CM).stream().map(CardScore::getCardId).collect(Collectors.toList()), playerPositionMap.get(CM).size(), 3, cm3);

        for (Tactic tactic : tacticList
//                .stream()
//                .filter(t -> Arrays.asList("4-1-3-2","4-1-2-1-2(2)","4-1-4-1","4-3-3(2)","4-5-1(2)","4-4-2")
//                        .contains(t.getName()))
//                .toList()
        ) {
            build(tactic);
        }
    }

    public static void build(Tactic tacticToBeConsidered) {

        if (!playerPositionMap.keySet().containsAll(tacticToBeConsidered.getPositions())) {
            return;
        }
        AtomicLong tacticTeams = new AtomicLong(1);
        Map<Position, Integer> positionFrequencies = getFrequency(tacticToBeConsidered.getPositions());
        positionFrequencies.forEach((key, value) -> tacticTeams.updateAndGet(v -> v * combination(playerPositionMap.get(key).size(), value)));
        tacticToBeConsidered.setTeamCount(tacticTeams.get());
        allTeamsCount += tacticTeams.get();
        System.out.println("\nAll possible teams : " + allTeamsCount + "\n");

        AtomicLong currentTacticCounter = new AtomicLong();
        List<VariationTeam> sortedTeamsByScore;
//        List<VariationTeam> finalSortedTeamsByScore = sortedTeamsByScore;
        if (!playerPositionMap.keySet().containsAll(tacticToBeConsidered.getPositions())) {
            return;
        }
        System.out.println("Computing for tactic (" + tacticToBeConsidered.getTeamCount() + ") : " + tacticToBeConsidered.getName() + " ... ");
        long tacticStartTime = System.nanoTime();
        VariationTeam team = new VariationTeam(tacticList.indexOf(tacticToBeConsidered), new ArrayList<>());
        constructVariationTeam(0, tacticToBeConsidered, team);
        long tacticElapsedTime = System.nanoTime() - tacticStartTime;
        currentTacticCounter.set(teamCounter - currentTacticCounter.get());
        System.out.println("Completed in " + tacticElapsedTime / 1000000 + " ms with " + currentTacticCounter + " teams");
        currentTacticCounter.set(teamCounter);
//            if (possibleTeams.size() > 0) {
//                finalSortedTeamsByScore.add(possibleTeams.stream().max(Comparator.comparing(VariationTeam::getScore)).get());
//            }
//            possibleTeams = new ArrayList<>();

        System.out.println("\nTeams sorted by overall score: ");
        sortedTeamsByScore = possibleTeams.stream().sorted(Comparator.comparing(VariationTeam::getTotalRating).reversed())
                .limit(10)
                .collect(Collectors.toList());

        for (VariationTeam variationTeam : sortedTeamsByScore) {
            variationTeam.setSubstitutes(tacticList, playerPositionMap, playerCardMap);
            System.out.print(variationTeam.toString(playerCardMap, tacticList, playerPositionMap) + "\t\t");
            System.out.print(variationTeam.getChemistry() + "\t");
            System.out.print(variationTeam.getTotalRating() + "\t");
            System.out.println(variationTeam.getScore());
        }

        possibleTeams = new ArrayList<>();


//        System.out.println("\nVariant teams with better scores: ");
//        for (Team team : sortedTeamsByScore) {
//            List<Team> variantTeams = createVariantTeams(team);
//            for (Team variantTeam : variantTeams) {
//                variantTeam.setSubstitutes(tacticList, playerPositionMap,playerCardMap);
//                System.out.print(variantTeam.toString(playerCardMap, tacticList, playerPositionMap) + "\t\t");
//                System.out.print(variantTeam.getChemistry() + "\t");
//                System.out.print(variantTeam.getTotalRating() + "\t");
//                System.out.println(variantTeam.getScore());
//            }
//        }

//        if (!shouldConsiderLoans) {
//            Set<Integer> importantPlayers = new HashSet<>();
//            sortedTeamsByScore.stream().limit(6).forEach(team -> {
//                importantPlayers.addAll(team.getPlayers());
//                importantPlayers.addAll(team.getSubstitutes());
//            });
//            System.out.println("\nPlayers that can be thrown away: ");
//            List<PlayerCard> playersToBeThrown = playerCardMap.entrySet().stream()
//                    .filter(entry -> entry.getKey() < 100000)
//                    .filter(entry -> !importantPlayers.contains(entry.getKey()))
//                    .map(Map.Entry::getValue)
//                    .sorted(Comparator.comparing(PlayerCard::getRating).reversed())
//                    .toList();
//            playersToBeThrown.forEach(playerCard -> {
//                System.out.print(playerCard.getCardInput().getName() + " ");
//                System.out.println(playerCard.getRating());
//            });
//        }


        long elapsedTime = System.nanoTime() - startTime;
        System.out.println("\nTime taken = " + elapsedTime / 1000000000 + " s");

    }

    public static double getPlayerScore(Position position, Map<String, Integer> attributes, PlayerCard playerCard, String style) throws Exception {
        if (attributes.isEmpty()) {
            return 0.0;
        }
        AtomicReference<Double> attributesScore = new AtomicReference<>(0.0);
        positionAttributesMap.get(position).forEach(attr ->
                attributesScore.updateAndGet(v -> v + attributes.get(attr)));
        if (position.equals(GK)) {
            double accelerateScore = 0.0;
            String acclStyle = playerCard.accelerateMap.entrySet().stream()
                    .filter(stringListEntry -> stringListEntry.getValue().contains(style))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .get();
            switch (acclStyle) {
                case "Lengthy" -> accelerateScore = 90.0;
                case "Explosive" -> accelerateScore = 99.56;
                case "Controlled" -> accelerateScore = 100.00;
            }
            double bodyTypeScore = getBodyTypeModifier(playerCard.getBodyType());
            double iconHeroScore = playerCard.getClubId() == 112658 ? 100 : playerCard.getClubId() == 114605 ? 95 : 90;
            double score = playerCard.getRating() + attributesScore.get();
            return (((score)) + accelerateScore + (playerCard.getHeight() / 18.2) + bodyTypeScore + iconHeroScore) / 10.0;
        } else {
            double positionRating = ChemistryCalculator.getPlayerRatingPerPosition(attributes, position);
            double accelerateScore = 0.0;
            String acclStyle = playerCard.accelerateMap.entrySet().stream()
                    .filter(stringListEntry -> stringListEntry.getValue().contains(style))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .get();
            switch (acclStyle) {
                case "Lengthy" -> accelerateScore = 90.0;
                case "Explosive" -> accelerateScore = 99.56;
                case "Controlled" -> accelerateScore = 100.00;
            }
            double score = positionRating + attributesScore.get();
            double bodyTypeScore = getBodyTypeModifier(playerCard.getBodyType());
            double iconHeroScore = playerCard.getClubId() == 112658 ? 100 : playerCard.getClubId() == 114605 ? 95 : 90;

            return ((score)
                    + (attributes.get("pace") * 2.0) + (attributes.get("strength") + attributes.get("aggression")) / 2.0
                    + (playerCard.getSkills() + playerCard.getWeakFoot()) * 10 + accelerateScore + (playerCard.getHeight() / 18.2)
                    + ((attributes.get("reactions") + attributes.get("composure")) / 2.0)
                    + ((attributes.get("shortpassing") + attributes.get("longpassing")) / 2.0)
                    + bodyTypeScore + iconHeroScore
            ) / 16.0;
        }
    }

    private static void getCombinations(List<Integer> arr, int n, int r, List<List<Integer>> store) {
        List<Integer> data = new ArrayList<>();
        for (int i = 0; i < r; i++) {
            data.add(i, 0);
        }
        combinationUntil(arr, data, 0, n - 1, 0, r, store);
    }

    private static void combinationUntil(List<Integer> arr, List<Integer> data, int start, int end, int index, int r, List<List<Integer>> store) {
        if (index == r) {
            List<Integer> item = new ArrayList<>();
            for (int j = 0; j < r; j++) {
                item.add(data.get(j));
            }
            store.add(item);
            return;
        }
        for (int i = start; i <= end && end - i + 1 >= r - index; i++) {
            data.set(index, arr.get(i));
            combinationUntil(arr, data, i + 1, end, index + 1, r, store);
        }
    }

    private static Map<Position, Integer> getFrequency(List<Position> positions) {
        Map<Position, Integer> frequencies = new HashMap<>();
        positions.forEach(position -> {
            frequencies.merge(position, 1, Integer::sum);
        });
        return frequencies;
    }

    private static long combination(int m, int n) {
        if (m == n) {
            return 1;
        }
        if (m < n) {
            return 0;
        }
        return factorial(m) / (factorial(m - n) * factorial(n));
    }

    private static long factorial(int n) {
        if (n <= 2) {
            return n;
        }
        return n * factorial(n - 1);
    }

    private static void constructVariationTeam(int position, Tactic tactic, VariationTeam team) {
        if (position >= 11) {
            teamCounter++;
            double percentage = teamCounter / (double) allTeamsCount * 100.0;
            if (teamCounter % 1000000 == 0) {
                System.out.println("Percentage completed : " + percentage + " VariationTeams : " + teamCounter + " ETA: " + calculateETA(percentage) + " s");
            }
            List<Integer> teamPlayerCardIds = team.getPlayers().stream().map(p -> p.getPlayerId() % 100000).toList();
            if (!mandatoryPlayers.isEmpty() && !new HashSet<>(teamPlayerCardIds).containsAll(mandatoryPlayers)) {
                return;
            }
            List<String> countryList = team.getPlayers().stream().map(p -> playerCardMap.get(p.getPlayerId()).getNation()).toList();
            if (Collections.frequency(countryList, "England") < 6) {
                return;
            }
            validateTeams(team);
            return;
        }
        Position positionName = tactic.getPositions().get(position);
        List<CardScore> players = playerPositionMap.get(positionName);
        List<List<Integer>> duplicatesList = combinationList(positionName, tactic.getPositions(), position);
        if (duplicatesList != null && !duplicatesList.isEmpty()) {
            int size = duplicatesList.get(0).size();
            duplicatesList.forEach(item -> {
                int modifiedCount = 0;
                for (int i = 0; i < size; i++) {
                    int player = item.get(i);
                    PlayerCard playerCard = playerCardMap.get(player);
                    String name = playerCard.getCardInput().getName();
//                    boolean isModifiedPlayer = !Objects.equals(positionName, playerCard.getPosition());
                    List<TeamPlayer> existingPlayers = team.getPlayers();
                    if (existingPlayers.size() > position + i) {
                        existingPlayers.set(position + i, new TeamPlayer(player));
                    } else {
                        existingPlayers.add(position + i, new TeamPlayer(player));
                    }
//                    if (isModifiedPlayer) {
//                        team.setModifiedPlayers(team.getModifiedPlayers() + 1);
//                        modifiedCount++;
//                        if (team.getModifiedPlayers() > modifiedPlayers) {
//                            team.setModifiedPlayers(team.getModifiedPlayers() - 1);
//                            modifiedCount--;
//                            return;
//                        }
//                    }
                }
                List<Integer> playerIds = team.getPlayers().stream().map(TeamPlayer::getPlayerId).toList();
                if (containsDuplicates(playerIds)) {
                    for (int i = size - 1; i >= 0; i--) {
                        team.getPlayers().remove(position + i);
                    }
                    return;
                }
                List<Integer> idList = playerCardMap.entrySet()
                        .stream()
                        .filter(entry -> playerIds.contains(entry.getKey()))
                        .map(entry -> entry.getValue().getId())
                        .toList();
                if (containsDuplicates(idList)) {
                    for (int i = size - 1; i >= 0; i--) {
                        team.getPlayers().remove(position + i);
                    }
                    return;
                }
                constructVariationTeam(position + size, tactic, team);
                for (int i = size - 1; i >= 0; i--) {
                    team.getPlayers().remove(position + i);
                }
//                team.setModifiedPlayers(team.getModifiedPlayers() - modifiedCount);
            });
        } else {
            AtomicReference<List<Integer>> playerIds = new AtomicReference<>(team.getPlayers().stream().map(TeamPlayer::getPlayerId).toList());
            players.stream().filter(player -> !playerIds.get().contains(player.getCardId()))
                    .filter(player -> {
                        Integer id = playerCardMap.get(player.getCardId()).getId();
                        return team.getPlayers().stream().map(p -> playerCardMap.get(p.getPlayerId()).getId()).noneMatch(p -> Objects.equals(p, id));
                    }).forEach(player -> {
                        PlayerCard playerCard = playerCardMap.get(player.getCardId());
                        String name = playerCard.getCardInput().getName();
//                        boolean isModifiedPlayer = !Objects.equals(positionName, playerCard.getPosition());
                        List<TeamPlayer> existingPlayers = team.getPlayers();
                        if (existingPlayers.size() > position) {
                            existingPlayers.set(position, new TeamPlayer(player.getCardId()));
                        } else {
                            existingPlayers.add(position, new TeamPlayer(player.getCardId()));
                        }
//                        if (isModifiedPlayer) {
//                            team.setModifiedPlayers(team.getModifiedPlayers() + 1);
//                            if (team.getModifiedPlayers() > modifiedPlayers) {
//                                team.setModifiedPlayers(team.getModifiedPlayers() - 1);
//                                return;
//                            }
//                        }
                        if (containsDuplicates(team.getPlayers())) {
                            existingPlayers.remove(position);
                            return;
                        }
                        playerIds.set(team.getPlayers().stream().map(TeamPlayer::getPlayerId).toList());
                        List<Integer> idList = playerCardMap.entrySet()
                                .stream()
                                .filter(entry -> playerIds.get().contains(entry.getKey()))
                                .map(entry -> entry.getValue().getId())
                                .toList();
                        if (containsDuplicates(idList)) {
                            existingPlayers.remove(position);
                            return;
                        }
                        constructVariationTeam(position + 1, tactic, team);
                        existingPlayers.remove(position);
//                        if (isModifiedPlayer) {
//                            team.setModifiedPlayers(team.getModifiedPlayers() - 1);
//                        }
                    });
        }
    }

    private static void validateTeams(VariationTeam team) {
//        if (team.getPlayerCalculation() == 497658) {
//            System.out.println("STOP");
//        }
        List<TeamPlayer> playerList = team.getPlayers();
        List<Integer> playerIds = playerList.stream().map(TeamPlayer::getPlayerId).toList();
        Map<String, Integer> nationMap = new HashMap<>();
        Map<Integer, Integer> leagueMap = new HashMap<>();
        Map<Integer, Integer> clubMap = new HashMap<>();
        playerList.forEach(playerIndex -> {
            PlayerCard playerCard = playerCardMap.get(playerIndex.getPlayerId());
            nationMap.merge(playerCard.getNation(), 1, Integer::sum);
            if (playerCard.getClubId().equals(112658)) {   // ICON club id = 112658
                nationMap.merge(playerCard.getNation(), 1, Integer::sum);
            }
            leagueMap.merge(playerCard.getLeagueId(), 1, Integer::sum);
            if (playerCard.getClubId().equals(114605) || playerCard.getCardInput().getName().contains("PREMIUM")) {  // Hero club id = 114605
                leagueMap.merge(playerCard.getLeagueId(), 1, Integer::sum);
            }
            clubMap.merge(playerCard.getClubId(), 1, Integer::sum);
        });
        Manager prevManager = null;
        List<VariationTeam> allTeams = new ArrayList<>();
        for (Manager manager : managerList) {
            playerIds = playerIds.stream().map(p -> p % 100000).collect(Collectors.toList());
            if (prevManager != null) {
                nationMap.merge(prevManager.getNation(), -1, Integer::sum);
                leagueMap.merge(prevManager.getLeague(), -1, Integer::sum);
            }
            nationMap.merge(manager.getNation(), 1, Integer::sum);
            leagueMap.merge(manager.getLeague(), 1, Integer::sum);
            prevManager = manager;
            if (new HashSet<>(playerList.stream().map(TeamPlayer::getPlayerId).toList()).containsAll(List.of(54259, 154226, 154042, 54058, 54180, 54240, 53921, 54252, 354210, 54164, 54275)) &&
                    manager.getName().equals("E. TEN HAG")) {
                System.out.println();
            }
            int chemistry = calculateChemistry(playerList, nationMap, leagueMap, clubMap);
            VariationTeam newTeam = new VariationTeam(team.getTactic(), new ArrayList<>(team.getPlayers()));
            double rating = calculateTeamTotalRating(newTeam.getPlayers(), newTeam.getSubstitutes());
            newTeam.setChemistry(chemistry);
            newTeam.setTotalRating(rating);
            newTeam.setScore();
            newTeam.setManager(manager);
            allTeams.add(newTeam);
//            if (chemistry == 33) {
//                teamHashMap.put(teamKey.toString(), newTeam);
//                possibleTeams.add(newTeam);
//                if (possibleTeams.size() % 10000 == 0) {
//                    System.out.println("No. of teams : " + possibleTeams.size());
//                }
//            }
        }
        VariationTeam bestTeam = allTeams.stream().max(Comparator.comparing(VariationTeam::getChemistry)).get();

//        Team bestTeam = allTeams.stream().max(Comparator.comparing(Team::getChemistry)).get();
        possibleTeams.add(bestTeam);
        if (possibleTeams.size() % 1000000 == 0) {
            System.out.println("No. of teams : " + possibleTeams.size());
        }
    }

    private static int calculateChemistry(List<TeamPlayer> playerList, Map<String, Integer> nationMap, Map<Integer, Integer> leagueMap, Map<Integer, Integer> clubMap) {
        int totalChemistry = 0;
        for (TeamPlayer player : playerList) {
            int chem = 0;
            PlayerCard playerCard = playerCardMap.get(player.getPlayerId());
            if (playerCard.getClubId().equals(114605) || playerCard.getClubId().equals(112658) || playerCard.getCardInput().getName().contains("PREMIUM")) {
                chem = 3;
                totalChemistry = totalChemistry + chem;
                player.setChemistry(chem);
                continue;
            }
            int nationSize = nationMap.get(playerCard.getNation());
            int clubSize = clubMap.get(playerCard.getClubId());
            int leagueSize = leagueMap.get(playerCard.getLeagueId());
            chem = chem + (nationSize < 2 ? 0 : nationSize < 5 ? 1 : nationSize < 8 ? 2 : 3);
            chem = chem + (clubSize < 2 ? 0 : clubSize < 4 ? 1 : clubSize < 7 ? 2 : 3);
            chem = chem + (leagueSize < 3 ? 0 : leagueSize < 5 ? 1 : leagueSize < 8 ? 2 : 3);
            int playerChem = Math.min(chem, 3);
            totalChemistry = totalChemistry + playerChem;
            player.setChemistry(playerChem);
        }
        return totalChemistry;
    }

    private static double calculateTeamTotalRating(List<TeamPlayer> playerList, List<Integer> substitutesList) {
        double totalTeamRating = 0;
        for (TeamPlayer player : playerList) {
            PlayerCard playerCard = playerCardMap.get(player.getPlayerId());
            totalTeamRating += playerCard.chemScores.get(player.getChemistry());
        }
        for (Integer sub : substitutesList) {
            PlayerCard playerCard = playerCardMap.get(sub);
            totalTeamRating += playerCard.chemScores.get(0);
        }
        return totalTeamRating;
    }

    private static List<List<Integer>> combinationList(Position positionName, List<Position> positions, int position) {
        if (duplicatePositions.contains(positionName)) {
            if (position < 9 && positions.get(position + 1).equals(positionName) && positions.get(position + 2).equals(positionName)) {
                switch (positionName) {
                    case CB -> {
                        return cb3;
                    }
                    case CM -> {
                        return cm3;
                    }
                }
            } else if (position < 10 && positions.get(position + 1).equals(positionName)) {
                switch (positionName) {
                    case CB -> {
                        return cb2;
                    }
                    case CM -> {
                        return cm2;
                    }
                    case ST -> {
                        return st2;
                    }
                    case CF -> {
                        return cf2;
                    }
                    case CDM -> {
                        return cdm2;
                    }
                    case CAM -> {
                        return cam2;
                    }
                }
            }
        }
        return null;
    }

    private static double calculateVariationTeamTotalRating(List<Integer> playerList, List<Integer> substitutesList) {
        double totalVariationTeamRating = 0;
        for (Integer playerIndex : playerList) {
            PlayerCard playerCard = playerCardMap.get(playerIndex);
            totalVariationTeamRating += playerCard.chemScores.get(3);
        }
        for (Integer sub : substitutesList) {
            PlayerCard playerCard = playerCardMap.get(sub);
            totalVariationTeamRating += playerCard.chemScores.get(0);
        }
        return totalVariationTeamRating;
    }

    private static <T> boolean containsDuplicates(Collection<T> collection) {
        Set<T> uniques = new HashSet<>();
        Set<T> set = collection.stream()
                .filter(e -> !uniques.add(e))
                .collect(Collectors.toSet());
        return set.size() > 0;
    }

    private static double calculateETA(double percentage) {
        double percentageLeft = 100 - percentage;
        long elapsedTime = System.nanoTime() - startTime;
        return ((percentageLeft * elapsedTime) / percentage) / 1000000000;
    }

    public static List<CardScore> removeElementsByIndex(List<CardScore> list, Set<Integer> indicesToRemove) {
        List<CardScore> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (!indicesToRemove.contains(i)) {
                result.add(list.get(i));
            }
        }
        return result;
    }
}
