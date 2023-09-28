package org.example.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.enums.Position;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerCard {

    private static final Logger LOG = LogManager.getLogger(PlayerCard.class);

    private CardInput cardInput;
    private String nation;
    private int skills;
    private int weakFoot;
    private int attackingWorkRate;
    private int defensiveWorkRate;
    private Integer id;
    private Integer clubId;
    private Integer leagueId;
    private Integer rating;
    private Integer height;
    private Integer weight;
    private Map<String, AttributeValues> baseAttributes;
    public List<Double> chemScores = new ArrayList<>();
    public String chemistry;
    public String bodyType;
    public Map<String, List<String>> accelerateMap = new HashMap<>();
    private List<Position> altPositions = new ArrayList<>();
    private long likes;
    private long dislikes;
    private Position primaryPosition;
    private String cardName;
    private String acceleRATE;

    public CardInput getCardInput() {
        return cardInput;
    }

    public void setCardInput(CardInput cardInput) {
        this.cardInput = cardInput;
    }

    public String getNation() {
        return nation;
    }

    public void setNation(String nation) {
        this.nation = nation;
    }

    public int getSkills() {
        return skills;
    }

    public void setSkills(int skills) {
        this.skills = skills;
    }

    public int getWeakFoot() {
        return weakFoot;
    }

    public void setWeakFoot(int weakFoot) {
        this.weakFoot = weakFoot;
    }

    public int getAttackingWorkRate() {
        return attackingWorkRate;
    }

    public void setAttackingWorkRate(String workRate) {
        switch (workRate) {
            case "High" -> this.attackingWorkRate = 3;
            case "Med" -> this.attackingWorkRate = 2;
            case "Low" -> this.attackingWorkRate = 1;
            default -> {
                this.attackingWorkRate = 0;
                LOG.error("Invalid attacking work rate for card id: " + this.cardInput.getCardId());
            }
        }
    }

    public void setAttackingWorkRate(int workRate) {
        this.attackingWorkRate = workRate;
    }

    public int getDefensiveWorkRate() {
        return defensiveWorkRate;
    }

    public void setDefensiveWorkRate(String workRate) {
        switch (workRate) {
            case "High" -> this.defensiveWorkRate = 3;
            case "Med" -> this.defensiveWorkRate = 2;
            case "Low" -> this.defensiveWorkRate = 1;
            default -> {
                this.defensiveWorkRate = 0;
                LOG.error("Invalid defensive work rate for card id: " + this.cardInput.getCardId());
            }
        }
    }

    public void setDefensiveWorkRate(int workRate) {
        this.defensiveWorkRate = workRate;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getClubId() {
        return clubId;
    }

    public void setClubId(Integer clubId) {
        this.clubId = clubId;
    }

    public Integer getLeagueId() {
        return leagueId;
    }

    public void setLeagueId(Integer leagueId) {
        this.leagueId = leagueId;
    }

    public Map<String, AttributeValues> getBaseAttributes() {
        return baseAttributes;
    }

    public void setBaseAttributes(Map<String, AttributeValues> baseAttributes) {
        this.baseAttributes = baseAttributes;
    }

    public List<Position> getAltPositions() {
        return altPositions;
    }

    public void setAltPositions(List<Position> altPositions) {
        this.altPositions = altPositions;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public String getBodyType() {
        return bodyType;
    }

    public void setBodyType(String bodyType) {
        this.bodyType = bodyType;
    }

    public long getLikes() {
        return likes;
    }

    public void setLikes(long likes) {
        this.likes = likes;
    }

    public long getDislikes() {
        return dislikes;
    }

    public void setDislikes(long dislikes) {
        this.dislikes = dislikes;
    }

    public Position getPrimaryPosition() {
        return primaryPosition;
    }

    public void setPrimaryPosition(Position primaryPosition) {
        this.primaryPosition = primaryPosition;
    }

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public String getAcceleRATE() {
        return acceleRATE;
    }

    public void setAcceleRATE(String acceleRATE) {
        this.acceleRATE = acceleRATE;
    }
}
