package org.example.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.enums.Position;
import org.example.model.PlayerCard;
import org.example.model.PositionWeight;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.example.enums.Position.GK;

public class PlayerScoreCalculator {

    // Define the weightage for each category
    private static Map<Position, PositionWeight> positionWeights = new HashMap<>();

    static {
        ObjectMapper objectMapper = new ObjectMapper();

        File file = new File("C:\\Users\\Uttey\\Documents\\Java\\FUT23\\src\\main\\java\\org\\example\\resources\\positionWeights.json");
        TypeReference<Map<Position, PositionWeight>> typeRef1 = new TypeReference<>() {};
        try {
            positionWeights = objectMapper.readValue(file, typeRef1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static double calculateScore(Position position, Map<String, Integer> attributes, PlayerCard playerCard) {
        if (attributes.isEmpty()) {
            return 0.0;
        }
        PositionWeight positionWeight = positionWeights.get(position);
        double attributeScore = 0;
        for (Map.Entry<String, Double> attribute : positionWeight.getAttributes().entrySet()) {
            if (attribute.getKey().equals("weakFoot")) {
                attributeScore += positionWeight.getWeakFoot() * attribute.getValue();
            } else {
                attributeScore += attributes.get(attribute.getKey()) * attribute.getValue();
            }
        }
        if (position  == GK) {
            return attributeScore + playerCard.getRating();
        }
        double positionRating = ChemistryCalculator.getPlayerRatingPerPosition(attributes, position);
        double skillMovesScore = playerCard.getSkills() * positionWeight.getSkillMoves();
        double weakFootScore = playerCard.getWeakFoot() * positionWeight.getWeakFoot();
        double workRateScore = playerCard.getAttackingWorkRate() * positionWeight.getWorkRates().get("attacking")
                + playerCard.getDefensiveWorkRate() * positionWeight.getWorkRates().get("defensive");
        return attributeScore + positionRating + skillMovesScore + weakFootScore + workRateScore;
    }

}
