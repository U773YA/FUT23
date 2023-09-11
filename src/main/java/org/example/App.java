package org.example;

import java.io.IOException;

/**
 * Hello world!
 *
 */
public class App
{
    public static void main( String[] args ) throws Exception {
        SquadBuilder2 squadBuilder = new SquadBuilder2();
//        SquadBuilderWithDataStore squadBuilder = new SquadBuilderWithDataStore();
        squadBuilder.build();
    }
}
