package org.example.model;

import java.util.List;

public class DbTeam {
    private List<Integer> players;
    private int chemistry;
    private double totalRating;
    private Manager manager;

    public DbTeam(List<Integer> players, int chemistry, double totalRating, Manager manager) {
        this.players = players;
        this.chemistry = chemistry;
        this.totalRating = totalRating;
        this.manager = manager;
    }

    public List<Integer> getPlayers() {
        return players;
    }

    public void setPlayers(List<Integer> players) {
        this.players = players;
    }

    public int getChemistry() {
        return chemistry;
    }

    public void setChemistry(int chemistry) {
        this.chemistry = chemistry;
    }

    public double getTotalRating() {
        return totalRating;
    }

    public void setTotalRating(double totalRating) {
        this.totalRating = totalRating;
    }

    public Manager getManager() {
        return manager;
    }

    public void setManager(Manager manager) {
        this.manager = manager;
    }
}
