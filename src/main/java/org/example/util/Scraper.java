package org.example.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.enums.Position;
import org.example.model.AttributeValues;
import org.example.model.PlayerCard;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scraper {

    private static final Logger LOG = LogManager.getLogger(Scraper.class);

    private Element infoContentTable;

    public Scraper() {

    }

    public PlayerCard getCardData(long cardId, Position playerInputPosition) throws IOException {
        PlayerCard playerCard = new PlayerCard();
        String cardUrl = "https://www.futbin.com/23/player/" + cardId;
        Connection connection = Jsoup.connect(cardUrl);
        connection.userAgent("Mozilla/5.0 (Windows NT 10.0;Win64) AppleWebkit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.82 Safari/537.36");
        Document doc = connection.get();

        Element playerStatsJson = doc.getElementById("player_stats_json");
        infoContentTable = doc.getElementById("info_content").child(0).child(0);

//        System.out.println(playerStatsJson.text());
        Map<String, AttributeValues> baseAttributes;
        try {
            baseAttributes = CardUtil.populateCardAttributes(playerStatsJson.text());
            playerCard.setBaseAttributes(baseAttributes);
        } catch (Exception ex) {
            LOG.error("Error in getting card data for card id: " + cardId +" with exception: " + ex.getMessage());
        }
        Element divElement = doc.selectFirst("#page-info");
        String currentPosition = divElement.attr("data-position");
        Position currPosition = null;
        try {
            currPosition = mapPosition(currentPosition);
        } catch (Exception ex) {
            LOG.error("Unknown position found " + currentPosition + " for card id " + cardId);
        }

        divElement = doc.selectFirst("div.pcdisplay-rat");
        playerCard.setRating(Integer.parseInt(divElement.text().trim()));

        Element thElement = infoContentTable.select("th:contains(Alt POS)").first();
        if (thElement != null) {
            assert thElement.parent() != null;
            Element tdElement = thElement.parent().select("td").first();
            if (tdElement != null) {
                String tdText = tdElement.text();
                String[] positionsArray = tdText.split(", ");
                List<Position> positionsList = new ArrayList<>();
                for (String position : positionsArray) {
                    try {
                        positionsList.add(mapPosition(position));
                    } catch (Exception ex) {
                        LOG.error("Unknown position found " + position + " for card id " + cardId);
                    }
                }
                if (currPosition != playerInputPosition) {
                    int index = positionsList.indexOf(playerInputPosition);
                    if (index != -1) {
                        positionsList.set(index, currPosition);
                    }
                }
                playerCard.setAltPositions(positionsList);
            }
        }

        playerCard.setNation(extractAttribute("Nation"));
        playerCard.setSkills(Integer.parseInt(extractAttribute("Skills")));
        playerCard.setWeakFoot(Integer.parseInt(extractAttribute("Weak Foot")));
        playerCard.setAttackingWorkRate(extractAttribute("Att. WR"));
        playerCard.setDefensiveWorkRate(extractAttribute("Def. WR"));
        playerCard.setId(Integer.valueOf(extractAttribute("ID")));
        playerCard.setClubId(Integer.valueOf(extractAttribute("Club ID")));
        playerCard.setLeagueId(Integer.valueOf(extractAttribute("League ID")));
        playerCard.setBodyType(extractAttribute("B.Type"));
        String height = extractAttribute("Height");
        Pattern pattern = Pattern.compile("(\\d+)cm");
        Matcher matcher = pattern.matcher(height);
        if (matcher.find()) {
            String extractedValue = matcher.group(1);
            playerCard.setHeight(Integer.parseInt(extractedValue));
        }
//        playerCard.setHeight(Integer.valueOf(pattern.matcher(extractAttribute("Height")).toString()));

        Map<String, List<String>> accelerateMap = new HashMap<>();
        String accelerateString = doc.getElementsByClass("chem-style-holder-points chem-style-three").get(0).getAllElements().get(0).text();
        String[] words = accelerateString.split(" ");
        List<String> chemList = new ArrayList<>();
        String key = "";

        for (String word : words) {
            if (Objects.equals(word, "None") || Arrays.asList("Lengthy", "Explosive", "Controlled").contains(word)) {
                if (!chemList.isEmpty()) {
                    accelerateMap.put(key, chemList);
                    chemList = new ArrayList<>();
                }
                if (!Objects.equals(word, "None")) {
                    key = word;
                }
            } else {
                chemList.add(word.toLowerCase());
            }
        }

        // Put the remaining list in the map (if any)
        if (!chemList.isEmpty()) {
            accelerateMap.put(key, chemList);
        }
        playerCard.accelerateMap = accelerateMap;

        return playerCard;
    }

    private String extractAttribute(String attributeName) {
        Element thElement = infoContentTable.select("th:contains(" + attributeName + ")").first();
        if (thElement != null) {
            Element tdElement = thElement.parent().select("td").first();
            if (tdElement != null) {
                String tdText = tdElement.text();
                return tdText;
            }
        }
        return "";
    }

    private Position mapPosition(String position) throws Exception {
        switch (position) {
            case "GK" -> {
                return Position.GK;
            }
            case "RB" -> {
                return Position.RB;
            }
            case "LB" -> {
                return Position.LB;
            }
            case "CB" -> {
                return Position.CB;
            }
            case "CDM" -> {
                return Position.CDM;
            }
            case "RWB" -> {
                return Position.RWB;
            }
            case "LWB" -> {
                return Position.LWB;
            }
            case "RM" -> {
                return Position.RM;
            }
            case "LM" -> {
                return Position.LM;
            }
            case "CM" -> {
                return Position.CM;
            }
            case "CAM" -> {
                return Position.CAM;
            }
            case "RW" -> {
                return Position.RW;
            }
            case "LW" -> {
                return Position.LW;
            }
            case "CF" -> {
                return Position.CF;
            }
            case "ST" -> {
                return Position.ST;
            }
            default -> throw new Exception();
        }
    }
}
