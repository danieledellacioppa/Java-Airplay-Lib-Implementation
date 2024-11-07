package com.github.serezhka.jap2server;

import android.util.Log;

import com.cjx.airplayjavademo.tools.LogRepository;
import com.github.serezhka.jap2lib.AirPlayBonjour;
import com.github.serezhka.jap2server.internal.ControlServer;

public class AirPlayServer {

    private final AirPlayBonjour airPlayBonjour;
    private final AirplayDataConsumer airplayDataConsumer;
    private final ControlServer controlServer;
    private Thread controlServerThread;
    private final String TAG = "AirPlayServer";

    private final String serverName;
    private final int airPlayPort;
    private final int airTunesPort;

    public AirPlayServer(String serverName, int airPlayPort, int airTunesPort,
                         AirplayDataConsumer airplayDataConsumer) {
        this.serverName = serverName;
        airPlayBonjour = new AirPlayBonjour(serverName);
        this.airPlayPort = airPlayPort;
        this.airTunesPort = airTunesPort;
        this.airplayDataConsumer = airplayDataConsumer;
        controlServer = new ControlServer(airPlayPort, airTunesPort, airplayDataConsumer);
    }

    public void start() throws Exception {
        airPlayBonjour.start(airPlayPort, airTunesPort);
        LogRepository.INSTANCE.addLog(TAG, serverName + " started on ports: " + airPlayPort + ", " + airTunesPort, 'I');
//        new Thread(controlServer).start();
        controlServerThread = new Thread(controlServer);
        controlServerThread.start();
    }

    public void stop() {
        airPlayBonjour.stop(); // Ferma Bonjour
        if (controlServerThread != null) {
            controlServer.stop();
            try {
                controlServerThread.join();  // attende la terminazione
                Log.d(TAG, "ControlServer stopped");
                LogRepository.INSTANCE.addLog(TAG, "ControlServer stopped", 'W');
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();  // gestisce l'interruzione
            }
        }
    }

    // TODO On client connected / disconnected
}
