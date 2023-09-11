package org.example.model;

import org.example.enums.Position;

public class CardInput {

    private final Integer cardId;
    private final String name;

    public void setPosition(Position position) {
        this.position = position;
    }

    private Position position;

    public Integer getRating() {
        return rating;
    }

    private final Integer rating;

    private boolean isChemNotAttached = false;

    public CardInput(Integer cardId, String name, Position position, Integer rating, boolean isChemnNotAttached) {
        this(cardId, name, position, rating);
        this.isChemNotAttached = isChemnNotAttached;
    }

    public CardInput(Integer cardId, String name, Position position, Integer rating) {
        this.cardId = cardId;
        this.name = name;
        this.position = position;
        this.rating = rating;
    }

    public Integer getCardId() {
        return cardId;
    }

    public String getName() {
        return name;
    }

    public Position getPosition() {
        return position;
    }

    public boolean isChemAttached() {
        return !isChemNotAttached;
    }

}
