package org.example.model;

public class Manager {

    private String name;
    private String nation;
    private Integer league;

    public Manager(String name, String nation, Integer league) {
        this.name = name;
        this.nation = nation;
        this.league = league;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNation() {
        return nation;
    }

    public void setNation(String nation) {
        this.nation = nation;
    }

    public Integer getLeague() {
        return league;
    }

    public void setLeague(Integer league) {
        this.league = league;
    }
}
