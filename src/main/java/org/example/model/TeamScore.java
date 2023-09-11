package org.example.model;

public class TeamScore {

    private Team team;
    private Double rating;

    public TeamScore(Team team, Double rating) {
        this.team = team;
        this.rating = rating;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }
}
