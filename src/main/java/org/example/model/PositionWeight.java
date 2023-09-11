package org.example.model;

import java.util.Map;

public class PositionWeight {

    private Map<String, Double> attributes;
    private double skillMoves;
    private double weakFoot;
    private Map<String, Double> workRates;

    public Map<String, Double> getAttributes() {
        return attributes;
    }

    public double getSkillMoves() {
        return skillMoves;
    }

    public double getWeakFoot() {
        return weakFoot;
    }

    public Map<String, Double> getWorkRates() {
        return workRates;
    }
}
