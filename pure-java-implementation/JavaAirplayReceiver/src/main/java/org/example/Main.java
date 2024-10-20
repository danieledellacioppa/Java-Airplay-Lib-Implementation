package org.example;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    public static void main(String[] args) {
        String serverName = "@srzhkaOnMacco";
        int airPlayPort = 5001;
        int airTunesPort = 7001;

        AirPlayServer airPlayServer = new AirPlayServer(serverName, airPlayPort, airTunesPort);
        try {
            airPlayServer.start();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                airPlayServer.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}