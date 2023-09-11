package org.example;

import org.example.enums.Position;
import org.example.model.CardInput;
import org.example.model.PlayerCard;
import org.example.model.VariationTeam;
import org.example.util.Scraper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FUTDraft {
    private static List<VariationTeam> possibleTeams = new ArrayList<>();

    public static void main( String[] args ) throws IOException {
        Scanner myObj = new Scanner(System.in);
        Scraper scraper = new Scraper();

        System.out.println("Enter cardId, name, position, rating, chemistryStyle for 5 players: ");
        List<PlayerCard> cardChoices = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Integer cardId = myObj.nextInt();
            String name = myObj.next();
            Position position = Position.valueOf(myObj.next());
            Integer rating = myObj.nextInt();
            String chemStyle = myObj.next();
            PlayerCard playerCard = scraper.getCardData(cardId, position);
            playerCard.setCardInput(new CardInput(cardId, name, position, rating));
            cardChoices.add(playerCard);
        }
    }
}
