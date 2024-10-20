package org.example;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Main {
    public static void main(String[] args) throws Exception {
        AirplayDataConsumerImpl dataConsumer = new AirplayDataConsumerImpl();

        String serverName = "AirPlayServer";
        int airPlayPort = 15614;
        int airTunesPort = 5001;

        // Avvia il server con il data consumer
        AirPlayServer airPlayServer = new AirPlayServer(serverName, airPlayPort, airTunesPort, dataConsumer);
        airPlayServer.start();

        // Aggiungi un modo per fermare e chiudere i file quando il server si ferma
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                dataConsumer.closeChannels();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }
}