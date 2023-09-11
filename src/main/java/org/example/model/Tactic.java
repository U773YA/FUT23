package org.example.model;

import org.example.enums.Position;

import java.util.List;

public class Tactic {

    private String name;
    private List<Position> positions;
    private long teamCount;

    public Tactic(String name, List<Position> positions) {
        this.name = name;
        this.positions = positions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Position> getPositions() {
        return positions;
    }

    public void setPositions(List<Position> positions) {
        this.positions = positions;
    }

    public long getTeamCount() {
        return teamCount;
    }

    public void setTeamCount(long teamCount) {
        this.teamCount = teamCount;
    }
}
