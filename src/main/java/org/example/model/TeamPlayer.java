package org.example.model;

public class TeamPlayer {
    private Integer playerId;
    private int chemistry;

    public TeamPlayer(Integer playerId) {
        this.playerId = playerId;
        this.chemistry = 0;
    }

    public Integer getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Integer playerId) {
        this.playerId = playerId;
    }

    public int getChemistry() {
        return chemistry;
    }

    public void setChemistry(int chemistry) {
        this.chemistry = chemistry;
    }
}
