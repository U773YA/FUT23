package org.example;

import com.opencsv.CSVWriter;
import org.example.enums.Position;
import org.example.model.PlayerCard;
import org.example.util.ChemistryCalculator;
import org.example.util.Scraper;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.example.enums.Position.CB;
import static org.example.enums.Position.GK;
import static org.example.enums.Position.ST;

public class DataMaker {

    private static final List<Integer> cardIdList = new ArrayList<>();
    static Position datasetPosition = ST;

    public static void main( String[] args ) throws Exception {
        Scraper scraper = new Scraper();
        CSVWriter csvWriter = new CSVWriter(new FileWriter(datasetPosition + "_dataset.csv"));

        cardIdList.addAll(List.of(26281, 50551, 50604, 53214, 50667, 50426, 53780, 48799, 48431, 54043,
                53178, 50208, 53849, 27396, 26601, 52793, 50524, 48673, 48769, 26951,
                284, 285, 69, 283, 67, 51671, 68, 50225, 50313, 168,
                209, 29606, 30508, 33894, 46696, 46715, 32657, 36303, 46385, 51051,
                26241, 26239, 26242, 50148, 53832, 52641, 51228, 47268, 44212, 50480,
                48603, 1, 3, 52967, 2, 103, 29791, 26316, 219, 47786,
                26233, 83, 1029, 50624, 52837, 47762, 26252, 37207, 26253, 54286,
                36, 51911, 51767, 35632, 35, 28747, 31631, 28159, 30439, 30927,
                295, 335, 48700, 50229, 50281, 48455, 50298, 88, 52528, 52911,
                52921, 50180, 279, 50194, 50563, 277, 51659, 52465, 48685, 30357,
                48438, 48589, 50341, 48407, 53999, 53756, 53875, 52956, 48713, 54009,
                50616, 46, 51250, 48, 48357, 29082, 29508, 38118, 28179, 35860,
                54070, 48613, 50241, 52936, 48414, 53769, 36137, 26705, 26698, 27174,
                34, 28880, 34924, 38249, 27213, 28260, 28825, 29400, 35927, 38298,
                52939, 34827, 27181, 31508, 29300, 33115, 37002, 36168, 51519, 46805, 34885, 44234, 48220,
                27821, 52623, 54030, 54238, 30261, 28827, 36755, 27819, 46969, 47965,
                38196, 31684, 32174, 44382, 45181, 52001, 28837, 31080, 45696, 45946,
                38360, 27222, 51368, 34173, 27290, 51461, 44478, 32172, 51956, 30051, 37235, 38198, 46341, 46896, 30677, 50936,
                54181, 53826, 52399,
                52516, 52535, 50526, 26247, 44182, 53131, 26965, 26547,
                53775, 26268, 54166, 51724, 52789, 48586, 50759,
                266, 265, 267, 52953, 50220,
                48688
                ));

        // Define headers (column names) if needed
        String[] headers = {"id", "name", "club", "nation", "league", "skills", "weakFoot", "height", "weight", "attWR", "defWR", "acceleRATE",
                "bodyType", "position", "rating", "acceleration", "sprintspeed", "positioning", "finishing","shotpower",
                "longshotsaccuracy","volleys","penalties","vision","crossing","freekickaccuracy", "shortpassing", "longpassing",
                "curve","agility","balance","reactions","ballcontrol","dribbling","composure", "interceptions",
                "headingaccuracy","marking","standingtackle","slidingtackle","jumping","stamina","strength",
                "aggression", "likes", "dislikes", "likesRatio", "positionRating"}; // Replace with your field names
        csvWriter.writeNext(headers);

        for (Integer cardId : cardIdList) {
            PlayerCard playerCard = scraper.getCardData(cardId, null);
            makeCsvRow(playerCard, cardId, csvWriter);
            System.out.println("Inserted data for cardID: " + cardId);
        }

        Random random = new Random();
        int totalWeight = 25;

        int count = 0;
        while (count < 500) {
            PlayerCard playerCard = new PlayerCard();
            int randomValue = 0;
            boolean isValidPlayer = false;
            while (!isValidPlayer) {
                try {
                    int randomWeight = random.nextInt(totalWeight);
                    if (randomWeight < 1) {
                        randomValue = random.nextInt(20000);
                    } else if (randomWeight < 11) {
                        randomValue = 20000 + random.nextInt(10000);
                    } else if (randomWeight < 14) {
                        randomValue = 30000 + random.nextInt(20000);
                    } else if (randomWeight < 24) {
                        randomValue = 50000 + random.nextInt(10000);
                    } else {
                        randomValue = 60000 + random.nextInt(40000);
                    }
//                    randomValue = random.nextInt(100000);
                    System.out.println("Trying for value " + randomValue);
                    if (cardIdList.contains(randomValue)) {
                        continue;
                    }
                    playerCard = scraper.getCardData(randomValue, null);
                    if (playerCard.getPrimaryPosition() == GK || playerCard.getPrimaryPosition() != datasetPosition ||
                            playerCard.getBodyType().isBlank()) {
                        continue;
                    }
                    isValidPlayer = true;
                } catch (Exception ignored) {
                }
            }

            makeCsvRow(playerCard, randomValue, csvWriter);
            System.out.println("Inserted data for cardID: " + randomValue + " count: "+ (count +1));
            count++;
            cardIdList.add(randomValue);
        }

//        for (Integer cardId : cardIdList) {
//            PlayerCard playerCard;
//            playerCard = scraper.getCardData(cardId, null);
//            Map<String, Integer> chemAttributes = ChemistryCalculator.createChemStyleStats(playerCard.chemistry,
//                    playerCard.getBaseAttributes(), 3, playerCard.getPrimaryPosition() == GK);
//            String[] data = {cardId.toString(), playerCard.getCardName(), playerCard.getClubId().toString(), playerCard.getNation(),
//                    playerCard.getLeagueId().toString(), String.valueOf(playerCard.getSkills()), String.valueOf(playerCard.getWeakFoot()),
//                    playerCard.getHeight().toString(), playerCard.getWeight().toString(), String.valueOf(playerCard.getAttackingWorkRate()),
//                    String.valueOf(playerCard.getDefensiveWorkRate()), playerCard.getAcceleRATE(), playerCard.getBodyType(), playerCard.getPrimaryPosition().toString(),
//                    playerCard.getRating().toString(), chemAttributes.get("acceleration").toString(), chemAttributes.get("sprintspeed").toString(),
//                    chemAttributes.get("positioning").toString(), chemAttributes.get("finishing").toString(), chemAttributes.get("shotpower").toString(),
//                    chemAttributes.get("longshotsaccuracy").toString(), chemAttributes.get("volleys").toString(), chemAttributes.get("penalties").toString(),
//                    chemAttributes.get("vision").toString(), chemAttributes.get("crossing").toString(), chemAttributes.get("freekickaccuracy").toString(),
//                    chemAttributes.get("shortpassing").toString(), chemAttributes.get("longpassing").toString(), chemAttributes.get("curve").toString(),
//                    chemAttributes.get("agility").toString(), chemAttributes.get("balance").toString(), chemAttributes.get("reactions").toString(),
//                    chemAttributes.get("ballcontrol").toString(), chemAttributes.get("dribbling").toString(), chemAttributes.get("composure").toString(),
//                    chemAttributes.get("interceptions").toString(), chemAttributes.get("headingaccuracy").toString(), chemAttributes.get("marking").toString(),
//                    chemAttributes.get("standingtackle").toString(), chemAttributes.get("slidingtackle").toString(), chemAttributes.get("jumping").toString(),
//                    chemAttributes.get("stamina").toString(), chemAttributes.get("strength").toString(), chemAttributes.get("aggression").toString(),
//                    String.valueOf(playerCard.getLikes()), String.valueOf(playerCard.getDislikes()),
//                    playerCard.getDislikes() == 0 ? String.valueOf((double) playerCard.getLikes()) : String.valueOf((double) playerCard.getLikes() / playerCard.getDislikes())};
//            csvWriter.writeNext(data);
//            System.out.println("Inserted data for cardID: " + cardId);
//        }

        csvWriter.close();
    }

    private static void makeCsvRow(PlayerCard playerCard, int randomValue, CSVWriter csvWriter) {
        Map<String, Integer> chemAttributes = ChemistryCalculator.createChemStyleStats(playerCard.chemistry,
                playerCard.getBaseAttributes(), 3, playerCard.getPrimaryPosition() == GK);
        double positionRating = ChemistryCalculator.getPlayerRatingPerPosition(chemAttributes, datasetPosition);
        String[] data = {String.valueOf(randomValue), playerCard.getCardName(), playerCard.getClubId().toString(), playerCard.getNation(),
                playerCard.getLeagueId().toString(), String.valueOf(playerCard.getSkills()), String.valueOf(playerCard.getWeakFoot()),
                playerCard.getHeight().toString(), playerCard.getWeight().toString(), String.valueOf(playerCard.getAttackingWorkRate()),
                String.valueOf(playerCard.getDefensiveWorkRate()), playerCard.getAcceleRATE(), playerCard.getBodyType(), playerCard.getPrimaryPosition().toString(),
                playerCard.getRating().toString(), chemAttributes.get("acceleration").toString(), chemAttributes.get("sprintspeed").toString(),
                chemAttributes.get("positioning").toString(), chemAttributes.get("finishing").toString(), chemAttributes.get("shotpower").toString(),
                chemAttributes.get("longshotsaccuracy").toString(), chemAttributes.get("volleys").toString(), chemAttributes.get("penalties").toString(),
                chemAttributes.get("vision").toString(), chemAttributes.get("crossing").toString(), chemAttributes.get("freekickaccuracy").toString(),
                chemAttributes.get("shortpassing").toString(), chemAttributes.get("longpassing").toString(), chemAttributes.get("curve").toString(),
                chemAttributes.get("agility").toString(), chemAttributes.get("balance").toString(), chemAttributes.get("reactions").toString(),
                chemAttributes.get("ballcontrol").toString(), chemAttributes.get("dribbling").toString(), chemAttributes.get("composure").toString(),
                chemAttributes.get("interceptions").toString(), chemAttributes.get("headingaccuracy").toString(), chemAttributes.get("marking").toString(),
                chemAttributes.get("standingtackle").toString(), chemAttributes.get("slidingtackle").toString(), chemAttributes.get("jumping").toString(),
                chemAttributes.get("stamina").toString(), chemAttributes.get("strength").toString(), chemAttributes.get("aggression").toString(),
                String.valueOf(playerCard.getLikes()), String.valueOf(playerCard.getDislikes()),
                playerCard.getDislikes() == 0 && playerCard.getLikes() == 0 ? String.valueOf(0.0) :
                        String.valueOf((double) playerCard.getLikes() / (playerCard.getLikes() + playerCard.getDislikes()) * 100.00),
                String.valueOf(positionRating)};
        csvWriter.writeNext(data);
    }
}
