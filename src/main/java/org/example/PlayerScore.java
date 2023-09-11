package org.example;

import org.example.enums.Position;
import org.example.model.CardInput;
import org.example.model.CardScore;
import org.example.model.PlayerCard;
import org.example.util.ChemistryCalculator;
import org.example.util.Scraper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static org.example.enums.Position.CAM;
import static org.example.enums.Position.CB;
import static org.example.enums.Position.CDM;
import static org.example.enums.Position.CF;
import static org.example.enums.Position.CM;
import static org.example.enums.Position.GK;
import static org.example.enums.Position.LB;
import static org.example.enums.Position.LM;
import static org.example.enums.Position.LW;
import static org.example.enums.Position.LWB;
import static org.example.enums.Position.RB;
import static org.example.enums.Position.RM;
import static org.example.enums.Position.RW;
import static org.example.enums.Position.RWB;
import static org.example.enums.Position.ST;

public class PlayerScore {

    protected static final List<String> chemStyles = List.of("hunter", "anchor", /*"basic",*/ "cat", "sniper","finisher","deadeye","marksman","hawk","artist","architect","powerhouse","maestro","engine","sentinel","guardian","gladiator","backbone","catalyst","shadow","wall","glove","shield");
    protected static final Map<Position, List<String>> positionAttributesMap = new HashMap<>();

    protected static void populatePositionMap() {
        positionAttributesMap.put(GK, List.of("gkreflexes", "gkhandling", "gkdiving","gkpositioning","gkkicking"));
        positionAttributesMap.put(RB, List.of("standingtackle", "marking", "slidingtackle","acceleration","longpassing"));
        positionAttributesMap.put(LB, List.of("standingtackle", "marking", "slidingtackle","acceleration","longpassing"));
        positionAttributesMap.put(CB, List.of("marking", "standingtackle", "strength","slidingtackle","aggression"));
        positionAttributesMap.put(RM, List.of("sprintspeed", "dribbling", "agility","shortpassing","reactions"));
        positionAttributesMap.put(LM, List.of("sprintspeed", "dribbling", "agility","shortpassing","reactions"));
        positionAttributesMap.put(CM, List.of("ballcontrol", "reactions", "vision","shortpassing","interceptions"));
        positionAttributesMap.put(ST, List.of("positioning", "finishing", "reactions","acceleration","strength"));
        positionAttributesMap.put(CDM, List.of("marking", "standingtackle", "interceptions","shortpassing","ballcontrol"));
        positionAttributesMap.put(CAM, List.of("dribbling", "agility", "vision","ballcontrol","sprintspeed"));
        positionAttributesMap.put(LWB, List.of("standingtackle", "marking", "aggression","crossing","sprintspeed"));
        positionAttributesMap.put(RWB, List.of("standingtackle", "marking", "aggression","crossing","sprintspeed"));
        positionAttributesMap.put(LW, List.of("dribbling", "agility", "sprintspeed","reactions","positioning"));
        positionAttributesMap.put(RW, List.of("dribbling", "agility", "sprintspeed","reactions","positioning"));
        positionAttributesMap.put(CF, List.of("positioning", "finishing", "dribbling","agility","longshotsaccuracy"));
    }


    public static void main( String[] args ) throws IOException {
        populatePositionMap();
        CardInput cardInput = new CardInput(53218, "BRUNO G TOTS", CM, 93);

        Scraper scraper = new Scraper();
        PlayerCard playerCard = scraper.getCardData(cardInput.getCardId(), cardInput.getPosition());
        playerCard.setCardInput(cardInput);
        if (!Objects.equals(playerCard.getRating(), playerCard.getCardInput().getRating())) {
            System.out.println("Mismatched rated card: " + playerCard.getCardInput().getName());
        }
        List<CardScore> scoresList = new ArrayList<>();
        chemStyles.forEach(style -> {
            Map<String, Integer> chemAttributes = ChemistryCalculator.createChemStyleStats(style, playerCard.getBaseAttributes(), 3, cardInput.getPosition() == GK);
//                System.out.println(style);
            Double score = getPlayerScore(cardInput.getPosition(), chemAttributes, playerCard);
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
        playerCard.chemScores.add(0, getPlayerScore(cardInput.getPosition(), ChemistryCalculator.createChemStyleStats(bestScore.getChemStyle(), playerCard.getBaseAttributes(), 0, cardInput.getPosition() == GK), playerCard));
        playerCard.chemScores.add(1, getPlayerScore(cardInput.getPosition(), ChemistryCalculator.createChemStyleStats(bestScore.getChemStyle(), playerCard.getBaseAttributes(), 1, cardInput.getPosition() == GK), playerCard));
        playerCard.chemScores.add(2, getPlayerScore(cardInput.getPosition(), ChemistryCalculator.createChemStyleStats(bestScore.getChemStyle(), playerCard.getBaseAttributes(), 2, cardInput.getPosition() == GK), playerCard));
        playerCard.chemScores.add(3, bestScore.getScore());
        playerCard.chemistry = bestScore.getChemStyle();

        System.out.println("Score: " + bestScore.getScore() + ", Chemistry: " + bestScore.getChemStyle());
    }

    public static double getPlayerScore(Position position, Map<String, Integer> attributes, PlayerCard playerCard) {
        if (attributes.size() == 0) {
            return 0.0;
        }
        AtomicReference<Double> attributesScore = new AtomicReference<>(0.0);
        positionAttributesMap.get(position).forEach(attr ->
                attributesScore.updateAndGet(v -> v + attributes.get(attr)));
        if (position.equals(GK)) {
//            AtomicReference<Double> attributesScore = new AtomicReference<>(0.0);
//            positionAttributesMap.get(position).forEach(attr ->
//                    attributesScore.updateAndGet(v -> v + attributes.get(attr)));
            return (playerCard.getRating() + attributesScore.get()) / 6.0;
        } else {
//            double workRating;
//            if (attackingPositions.contains(position)) {
//                workRating = playerCard.getAttackingWorkRate() * 100 / 3.0;
//            } else if (defensivePositions.contains(position)) {
//                workRating = playerCard.getDefensiveWorkRate() * 100 / 3.0;
//            } else {
//                workRating = (playerCard.getAttackingWorkRate() + playerCard.getDefensiveWorkRate()) * 100 / 6.0;
//            }
            double positionRating = ChemistryCalculator.getPlayerRatingPerPosition(attributes, position);
            return (positionRating + attributesScore.get()
                    + attributes.get("sprintspeed") + attributes.get("strength")
            ) / 8.0;
//            double total = positionRating + attributesScore.get() + (positionsNeedingSpeed.contains(position) ? attributes.get("sprintspeed") : 0) + (positionsNeedingStrength.contains(position) ? attributes.get("strength") : 0);
//            return total / (6.0 + (positionsNeedingStrength.contains(position) ? 1 : 0) + (positionsNeedingSpeed.contains(position) ? 1 : 0));
        }
    }
}
