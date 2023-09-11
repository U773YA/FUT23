package org.example.model;

import org.example.enums.Position;

public class CardScore {

    private Integer cardId;
    private Double score;
    private String chemStyle;

    public Position getPosition() {
        return position;
    }

    private final Position position;

    public CardScore(Integer cardId, Double score, String chemStyle, Position position) {
        this.cardId = cardId;
        this.score = score;
        this.chemStyle = chemStyle;
        this.position = position;
    }

    public Integer getCardId() {
        return cardId;
    }

    public void setCardId(Integer cardId) {
        this.cardId = cardId;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getChemStyle() {
        return chemStyle;
    }

    public void setChemStyle(String chemStyle) {
        this.chemStyle = chemStyle;
    }
}
