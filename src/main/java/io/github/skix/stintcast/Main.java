package io.github.skix.stintcast;

import io.github.skix.stintcast.client.OpenF1Client;
import io.github.skix.stintcast.model.Lap;
import io.github.skix.stintcast.model.Pit;
import io.github.skix.stintcast.model.Stint;

import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {

        OpenF1Client openF1Client = new OpenF1Client();

        List<Lap> laps = openF1Client.getLaps(9590);

        System.out.println("Laps: " + laps.size());
        System.out.println(laps.getFirst());

        List<Stint> stint = openF1Client.getStints(9590);
        System.out.println("Stints: " + stint.size());
        System.out.println(stint.getFirst());

        List<Pit> pits = openF1Client.getPits(9590);
        System.out.println("Pits: " + pits.size());
        System.out.println(pits.getFirst());

    }
}