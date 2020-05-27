package org.projectforge.plugins.ihk;

public class IHKCommentObject {

    String ausbildungsStart;
    int ausbildungsJahr;
    String team;

    public IHKCommentObject(String ausbildungsStart, int ausbildungsJahr, String team){

        this.ausbildungsStart = ausbildungsStart;
        this.ausbildungsJahr = ausbildungsJahr;
        this.team = team;

    }

    public int getAusbildungsJahr() {
        return ausbildungsJahr;
    }

    public String getAusbildungStart() {
        return ausbildungsStart;
    }

    public String getTeam() {
        return team;
    }
}
