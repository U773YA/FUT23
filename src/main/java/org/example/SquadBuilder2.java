package org.example;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.example.enums.Position;
import org.example.model.CardInput;
import org.example.model.CardScore;
import org.example.model.Manager;
import org.example.model.PlayerCard;
import org.example.model.Tactic;
import org.example.model.Team;
import org.example.util.ChemistryCalculator;
import org.example.util.InputData;
import org.example.util.Scraper;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
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

public class SquadBuilder2 extends InputData {

    private static final List<Double> ETAList = new ArrayList<>();
    private static List<Integer> wishListPlayerIds = new ArrayList<>();
    private static Map<String, List<Manager>> nationManagerMap = new HashMap<>();
    private static Map<Integer, List<Manager>> leagueManagerMap = new HashMap<>();
    public static Map<Integer, PlayerCard> dbPlayerCardMap = new HashMap<>();

    public SquadBuilder2 () {

    }

    public void build() throws Exception {
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
                dbPlayerCardMap = gson.fromJson(reader, new TypeToken<HashMap<Integer, PlayerCard>>(){}.getType());
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
                double score;
                try {
                    score = getPlayerScore(cardInput.getPosition(), chemAttributes, playerCard, style);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                scoresList.add(new CardScore(cardInput.getCardId(), score, style, cardInput.getPosition()));
            });
            CardScore bestScore = scoresList.stream().sorted(Comparator.comparing(CardScore::getScore).reversed()).limit(1).toList().get(0);
            System.out.println(cardInput.getName() + " -> " + bestScore.getChemStyle() + ", " + bestScore.getScore() + ", " + bestScore.getPosition());
            playerCard.chemScores.add(0, getPlayerScore(cardInput.getPosition(), ChemistryCalculator.createChemStyleStats(bestScore.getChemStyle(), playerCard.getBaseAttributes(), 0, cardInput.getPosition() == GK), playerCard, bestScore.getChemStyle()));
            playerCard.chemScores.add(1, getPlayerScore(cardInput.getPosition(), ChemistryCalculator.createChemStyleStats(bestScore.getChemStyle(), playerCard.getBaseAttributes(), 1, cardInput.getPosition() == GK), playerCard, bestScore.getChemStyle()));
            playerCard.chemScores.add(2, getPlayerScore(cardInput.getPosition(), ChemistryCalculator.createChemStyleStats(bestScore.getChemStyle(), playerCard.getBaseAttributes(), 2, cardInput.getPosition() == GK), playerCard,bestScore.getChemStyle()));
            playerCard.chemScores.add(3, bestScore.getScore());
            playerCard.chemistry = bestScore.getChemStyle();
            playerPositionMap.computeIfAbsent(cardInput.getPosition(), k -> new ArrayList<>()).add(bestScore);
            playerCardMap.put(cardInput.getCardId(), playerCard);
        }

        if (shouldConsiderPlayerPositionMods) {
            List<CardScore> playerScores = playerPositionMap.values().stream().flatMap(List::stream).toList();
            for (CardScore playerScore : playerScores) {
                PlayerCard playerCard = playerCardMap.get(playerScore.getCardId());
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

            wishListPlayerIds = wishListPlayers.stream().map(CardInput::getCardId).toList();

            for (List<CardScore> list : playerPositionMap.values()) {
//                if (list.get(0).getPosition() == CF) {
//                    System.out.println();
//                }
                Position position = list.get(0).getPosition();
                list.sort(Comparator.comparing(CardScore::getScore).reversed());

                Set<Integer> playersToRemove = new HashSet<>();
                for (int i = 0; i < list.size(); i++) {
                    for (int j = 0; j < list.size(); j++) {
                        if (Objects.equals(playerCardMap.get(list.get(i).getCardId()).getId(), playerCardMap.get(list.get(j).getCardId()).getId())) {
//                            if (list.get(i).getCardId() % 100000 == 54164) {
//                                System.out.println();
//                            }
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
                    if ((mandatoryPlayers.contains(key % 100000) || wishListPlayerIds.contains(key % 100000)) && !listPlayers.contains(key) &&
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
                System.out.print(playerCard.getCardInput().getName() + " (" + cardScore.getChemStyle() + ") " + "-" +String.format("%.2f", playerCard.chemScores.get(0)) + "," +String.format("%.2f", playerCard.chemScores.get(1)) + ","+String.format("%.2f", playerCard.chemScores.get(2)) + ","+String.format("%.2f", playerCard.chemScores.get(3)) + ", ");
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

        tacticList.forEach(tactic -> {
            if (!playerPositionMap.keySet().containsAll(tactic.getPositions())) {
                return;
            }
            AtomicLong tacticTeams = new AtomicLong(1);
            Map<Position, Integer> positionFrequencies = getFrequency(tactic.getPositions());
            positionFrequencies.forEach((key, value) -> tacticTeams.updateAndGet(v -> v * combination(playerPositionMap.get(key).size(), value)));
            tactic.setTeamCount(tacticTeams.get());
            allTeamsCount += tacticTeams.get();
        });
        System.out.println("\nAll possible teams : " + allTeamsCount + "\n");

        for (Map.Entry<Integer, PlayerCard> playerCard : playerCardMap.entrySet()) {
            if (!dbPlayerCardMap.containsKey(playerCard.getKey())) {
                dbPlayerCardMap.put(playerCard.getKey(), playerCard.getValue());
            }
        }
        Gson gson3 = new Gson();
        try (Writer writer = new FileWriter("playerCardMap.json")) {
            gson3.toJson(dbPlayerCardMap, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        AtomicLong currentTacticCounter = new AtomicLong();
        List<Team> sortedTeamsByScore = new ArrayList<>();
        List<Team> finalSortedTeamsByScore = sortedTeamsByScore;
        tacticList.forEach(tactic -> {
            if (!playerPositionMap.keySet().containsAll(tactic.getPositions())) {
                return;
            }
            System.out.println("Computing for tactic (" + tactic.getTeamCount() + ") : " + tactic.getName() + " ... ");
            long tacticStartTime = System.nanoTime();
            Team team = new Team(tacticList.indexOf(tactic), new ArrayList<>());
            constructTeam(0, tactic, team);
            long tacticElapsedTime = System.nanoTime() - tacticStartTime;
            currentTacticCounter.set(teamCounter - currentTacticCounter.get());
            System.out.println("Completed in " + tacticElapsedTime / 1000000 + " ms with " + currentTacticCounter + " teams");
            currentTacticCounter.set(teamCounter);
            Team bestTeamForTactic;
            if (!possibleTeams.isEmpty()) {
                bestTeamForTactic = possibleTeams.stream().max(Comparator.comparing(Team::getTotalRating)).get();
                finalSortedTeamsByScore.add(bestTeamForTactic);
                bestTeamForTactic.setSubstitutes(tacticList, playerPositionMap, playerCardMap);
                System.out.print(bestTeamForTactic.toString(playerCardMap, tacticList, playerPositionMap) + "\t\t");
                System.out.print(bestTeamForTactic.getChemistry() + "\t");
                System.out.print(bestTeamForTactic.getTotalRating() + "\t");
                System.out.print(bestTeamForTactic.getScore() + "\t");
                System.out.println(bestTeamForTactic.getTeamRating(playerCardMap));
            }
            possibleTeams = new ArrayList<>();
        });

        System.out.println("\nTeams sorted by overall score: ");
        sortedTeamsByScore = sortedTeamsByScore.stream().sorted(Comparator.comparing(Team::getTotalRating).reversed())
                .collect(Collectors.toList());

        List<Integer> tacticsPrinted = new ArrayList<>();
        List<Team> teamsPrinted = new ArrayList<>();
        for (Team team : sortedTeamsByScore) {
            if (!tacticsPrinted.contains(team.getTactic())) {
                team.setSubstitutes(tacticList, playerPositionMap, playerCardMap);
                System.out.print(team.toString(playerCardMap, tacticList, playerPositionMap) + "\t\t");
                System.out.print(team.getChemistry() + "\t");
                System.out.print(team.getTotalRating() + "\t");
                System.out.print(team.getScore() + "\t");
                System.out.println(team.getTeamRating(playerCardMap));
                tacticsPrinted.add(team.getTactic());
                teamsPrinted.add(team);
            }
        }

        if (!shouldConsiderLoans) {
            Set<Integer> playersConsidered = playerPositionMap.values()
                    .stream()
                    .flatMap(List::stream)
                    .toList()
                    .stream()
                    .map(p  -> p.getCardId() % 100000)
                    .collect(Collectors.toSet());
            Set<Integer> importantPlayers = new HashSet<>();
            List<Team> importantTeams = teamsPrinted.stream().limit(6).toList();
            Set<Integer> finalImportantPlayers = importantPlayers;
            importantTeams.forEach(team -> {
                finalImportantPlayers.addAll(team.getPlayers());
                finalImportantPlayers.addAll(team.getSubstitutes());
            });
            importantPlayers = importantPlayers.stream().map(p -> p % 100000).collect(Collectors.toSet());
            System.out.println("\nImportance of players");
            Map<Integer, Integer> playerImportance = calculateFrequency(importantTeams.stream()
                    .map(i -> {
                        List<Integer> integers = i.getPlayers().stream().map(p -> p % 100000).collect(Collectors.toList());
                        integers.addAll(i.getSubstitutes().stream().map(p -> p % 100000).toList());
                        return integers;
                    })
                    .collect(Collectors.toList()), importantPlayers);

            List<Map.Entry<Integer, Integer>> sortedList = new ArrayList<>(playerImportance.entrySet());
            sortedList.sort((entry1, entry2) -> {
                // Sort in descending order based on the second integer value
                int sortValue = entry2.getValue().compareTo(entry1.getValue());
                if (sortValue == 0) {
                    AtomicInteger count1 = new AtomicInteger();
                    AtomicInteger count2 = new AtomicInteger();
                    importantTeams.stream().filter(team -> team.getPlayers().stream().map(p -> p % 100000).toList().contains(entry1.getKey())).forEach(p -> count1.set(count1.get() + 1));
                    importantTeams.stream().filter(team -> team.getPlayers().stream().map(p -> p % 100000).toList().contains(entry2.getKey())).forEach(p -> count2.set(count2.get() + 1));
                    sortValue = Integer.compare(count2.get(), count1.get());
                }
                return sortValue;
            });
            for (Map.Entry<Integer, Integer> importance : sortedList) {
                System.out.print(playerCardMap.get(importance.getKey()).getCardInput().getName());
                System.out.println(" - " + importance.getValue());
            }

            System.out.println("\nPlayers that can be thrown away: ");
            List<PlayerCard> playersToBeThrown = playerCardMap.entrySet().stream()
                    .filter(entry -> entry.getKey() < 100000)
                    .filter(entry -> !playerImportance.containsKey(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .sorted(Comparator.comparing(PlayerCard::getRating).reversed())
                    .toList();
            playersToBeThrown.forEach(playerCard -> {
                boolean isUseless = !playersConsidered.contains(playerCard.getCardInput().getCardId());
                System.out.print(playerCard.getCardInput().getName() + " ");
                System.out.println(playerCard.getRating() + " " + (isUseless ? "X" : ""));
            });
        }

        long elapsedTime = System.nanoTime() - startTime;
        System.out.println("\nTime taken = " + elapsedTime / 1000000000 + " s");
    }

    private static void constructTeam(int position, Tactic tactic, Team team) {
        if (position >= 11) {
            teamCounter++;
            double percentage = teamCounter / (double) allTeamsCount * 100.0;
            double eta = calculateETA(percentage);
            ETAList.add(eta);
            if (teamCounter % 1000000 == 0) {
                System.out.println("Percentage completed : " + percentage + " Teams : " + teamCounter + " ETA: " + eta +" s");
            }
            List<Integer> teamPlayerCardIds = team.getPlayers().stream().map(p -> p % 100000).toList();
            if (!mandatoryPlayers.isEmpty() && !new HashSet<>(teamPlayerCardIds).containsAll(mandatoryPlayers)) {
                return;
            }
            if (!wishListPlayerIds.isEmpty() && !new HashSet<>(teamPlayerCardIds).containsAll(wishListPlayerIds)) {
                return;
            }
            List<String> countryList = team.getPlayers().stream().map(p -> playerCardMap.get(p).getNation()).toList();
//            List<Integer> leagueList = team.getPlayers().stream().map(p -> playerCardMap.get(p).getLeagueId()).toList();
//            if (Collections.frequency(countryList, "England") < 3) {
//                return;
//            }
//            if (new HashSet<>(team.getPlayers().stream().map(p -> p % 100000).toList())
//                    .containsAll(List.of(54259,54226,54042,54058,54180,54240,53921,54252,54210,54164,54275))) {
//                System.out.println();
//            }
            validateTeams(team, shouldSaveHeapSpace);
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
                    List<Integer> existingPlayers = team.getPlayers();
                    if (existingPlayers.size() > position + i) {
                        existingPlayers.set(position + i, player);
                    } else {
                        existingPlayers.add(position + i, player);
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
                List<Integer> playerIds = team.getPlayers();
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
                constructTeam(position + size, tactic, team);
                for (int i = size - 1; i >= 0; i--) {
                    team.getPlayers().remove(position + i);
                }
//                team.setModifiedPlayers(team.getModifiedPlayers() - modifiedCount);
            });
        } else {
            AtomicReference<List<Integer>> playerIds = new AtomicReference<>(team.getPlayers());
            players.stream().filter(player -> !playerIds.get().contains(player.getCardId()))
                    .filter(player -> {
                        Integer id = playerCardMap.get(player.getCardId()).getId();
                        return team.getPlayers().stream().map(p -> playerCardMap.get(p).getId()).noneMatch(p -> Objects.equals(p, id));
                    }).forEach(player -> {
                        PlayerCard playerCard = playerCardMap.get(player.getCardId());
                        String name = playerCard.getCardInput().getName();
//                        boolean isModifiedPlayer = !Objects.equals(positionName, playerCard.getPosition());
                        List<Integer> existingPlayers = team.getPlayers();
                        if (existingPlayers.size() > position) {
                            existingPlayers.set(position, player.getCardId());
                        } else {
                            existingPlayers.add(position, player.getCardId());
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
                        playerIds.set(team.getPlayers());
                        List<Integer> idList = playerCardMap.entrySet()
                                .stream()
                                .filter(entry -> playerIds.get().contains(entry.getKey()))
                                .map(entry -> entry.getValue().getId())
                                .toList();
                        if (containsDuplicates(idList)) {
                            existingPlayers.remove(position);
                            return;
                        }
                        constructTeam(position + 1, tactic, team);
                        existingPlayers.remove(position);
//                        if (isModifiedPlayer) {
//                            team.setModifiedPlayers(team.getModifiedPlayers() - 1);
//                        }
                    });
        }
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

    private static double calculateTeamTotalRating(List<Integer> playerList, List<Integer> substitutesList) {
        double totalTeamRating = 0;
        for (Integer playerIndex : playerList) {
            PlayerCard playerCard = playerCardMap.get(playerIndex);
            totalTeamRating += playerCard.chemScores.get(3);
        }
        for (Integer sub : substitutesList) {
            PlayerCard playerCard = playerCardMap.get(sub);
            totalTeamRating += playerCard.chemScores.get(0);
        }
        return totalTeamRating;
    }

    private static <T> boolean containsDuplicates(Collection<T> collection) {
        Set<T> uniques = new HashSet<>();
        Set<T> set = collection.stream()
                .filter(e -> !uniques.add(e))
                .collect(Collectors.toSet());
        return !set.isEmpty();
    }

    private static void validateTeams(Team team, boolean shouldCalculateFullChemistry) {
        List<Integer> playerList = team.getPlayers();
        Map<String, Integer> nationMap = new HashMap<>();
        Map<Integer, Integer> leagueMap = new HashMap<>();
        Map<Integer, Integer> clubMap = new HashMap<>();
        for (Integer playerIndex : playerList) {
            PlayerCard playerCard = playerCardMap.get(playerIndex);
            nationMap.merge(playerCard.getNation(), 1, Integer::sum);
            if (playerCard.getClubId().equals(112658)) {   // ICON club id = 112658
                nationMap.merge(playerCard.getNation(), 1, Integer::sum);
            }
            leagueMap.merge(playerCard.getLeagueId(), 1, Integer::sum);
            if (playerCard.getClubId().equals(114605) || playerCard.getCardInput().getName().contains("PREMIUM")) {   // Hero club id = 114605
                leagueMap.merge(playerCard.getLeagueId(), 1, Integer::sum);
            }
            clubMap.merge(playerCard.getClubId(), 1, Integer::sum);
        }
        List<Team> allTeams = new ArrayList<>();
        for (Map.Entry<String, Integer> nationEntry : nationMap.entrySet()) {
            if (Arrays.asList(1, 4, 7).contains(nationEntry.getValue())) {
                List<Manager> managers = nationManagerMap.get(nationEntry.getKey());
                if (managers != null) {
                    for (Manager manager : managers) {
                        nationMap.merge(manager.getNation(), 1, Integer::sum);
                        leagueMap.merge(manager.getLeague(), 1, Integer::sum);
                        int chemistry = calculateChemistry(playerList, nationMap, leagueMap, clubMap, shouldCalculateFullChemistry);
                        Team newTeam = new Team(team.getTactic(), new ArrayList<>(team.getPlayers()));
                        double rating = calculateTeamTotalRating(newTeam.getPlayers(), newTeam.getSubstitutes());
                        newTeam.setChemistry(chemistry);
                        newTeam.setTotalRating(rating);
                        newTeam.setScore();
                        newTeam.setManager(manager);
                        allTeams.add(newTeam);
                        nationMap.merge(manager.getNation(), -1, Integer::sum);
                        leagueMap.merge(manager.getLeague(), -1, Integer::sum);
                    }
                }
            }
        }
        for (Map.Entry<Integer, Integer> leagueEntry : leagueMap.entrySet()) {
            if (Arrays.asList(2, 4, 7).contains(leagueEntry.getValue())) {
                List<Manager> managers = leagueManagerMap.get(leagueEntry.getKey());
                if (managers != null) {
                    for (Manager manager : managers) {
                        nationMap.merge(manager.getNation(), 1, Integer::sum);
                        leagueMap.merge(manager.getLeague(), 1, Integer::sum);
                        int chemistry = calculateChemistry(playerList, nationMap, leagueMap, clubMap, shouldCalculateFullChemistry);
                        Team newTeam = new Team(team.getTactic(), new ArrayList<>(team.getPlayers()));
                        double rating = calculateTeamTotalRating(newTeam.getPlayers(), newTeam.getSubstitutes());
                        newTeam.setChemistry(chemistry);
                        newTeam.setTotalRating(rating);
                        newTeam.setScore();
                        newTeam.setManager(manager);
                        allTeams.add(newTeam);
                        nationMap.merge(manager.getNation(), -1, Integer::sum);
                        leagueMap.merge(manager.getLeague(), -1, Integer::sum);
                    }
                }
            }
        }
        Optional<Team> optBestTeam = allTeams.stream().max(Comparator.comparing(Team::getChemistry));
        if (optBestTeam.isPresent()) {
            Team bestTeam = optBestTeam.get();
            if (bestTeam.getChemistry() == 33) {
                possibleTeams.add(bestTeam);
                if (possibleTeams.size() % 100000 == 0) {
                    System.out.println("No. of teams : " + possibleTeams.size());
                }
            }
        }
    }

    private static int calculateChemistry(List<Integer> playerList, Map<String, Integer> nationMap, Map<Integer, Integer> leagueMap, Map<Integer, Integer> clubMap, boolean shouldCalculateFullChemistry) {
        int totalChemistry = 0;
        for (Integer playerIndex : playerList) {
            int chem = 0;
            PlayerCard playerCard = playerCardMap.get(playerIndex);
            if (playerCard.getClubId().equals(114605) || playerCard.getClubId().equals(112658) || playerCard.getCardInput().getName().contains("PREMIUM")) {
                chem = 3;
                totalChemistry = totalChemistry + chem;
                continue;
            }
            int nationSize = nationMap.get(playerCard.getNation());
            int clubSize = clubMap.get(playerCard.getClubId());
            int leagueSize = leagueMap.get(playerCard.getLeagueId());
            chem = chem + (nationSize < 2 ? 0 : nationSize < 5 ? 1 : nationSize < 8 ? 2 : 3);
            chem = chem + (clubSize < 2 ? 0 : clubSize < 4 ? 1 : clubSize < 7 ? 2 : 3);
            chem = chem + (leagueSize < 3 ? 0 : leagueSize < 5 ? 1 : leagueSize < 8 ? 2 : 3);
            int playerChem = Math.min(chem, 3);
            if (shouldCalculateFullChemistry && playerChem < 3) {
                break;
            }
            totalChemistry = totalChemistry + playerChem;
        }
        return totalChemistry;
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
            return  (((score)) + accelerateScore + (playerCard.getHeight() / 18.2) + bodyTypeScore + iconHeroScore) / 10.0;
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


    private static long factorial(int n) {
        if (n <= 2) {
            return n;
        }
        return n * factorial(n - 1);
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

    private static double calculateETA(double percentage) {
        double percentageLeft = 100 - percentage;
        long elapsedTime = System.nanoTime() - startTime;
        return ((percentageLeft * elapsedTime) / percentage) / 1000000000;
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
        for (int i = start; i <= end && end -i + 1 >= r - index; i++) {
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

    private static Map<Integer, Integer> calculateFrequency(List<List<Integer>> teamPlayers, Set<Integer> importantPlayers) {
        Map<Integer, Integer> frequencyMap = new HashMap<>();
        // Iterate over each sublist in the main list
        for (List<Integer> sublist : teamPlayers) {
            // Iterate over each integer in the sublist
            for (Integer number : sublist) {
                // Check if the integer exists in list A
                if (importantPlayers.contains(number)) {
                    // Update the frequency map
                    frequencyMap.put(number, frequencyMap.getOrDefault(number, 0) + 1);
                }
            }
        }

        return frequencyMap;
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
