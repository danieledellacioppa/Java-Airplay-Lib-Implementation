package com.github.serezhka.jap2server.internal.handler.session;

import android.util.Log;

import com.cjx.airplayjavademo.tools.LogRepository;
import com.github.serezhka.jap2lib.AirPlay;
import com.github.serezhka.jap2server.internal.MirroringReceiver;

public class Session {

    private final AirPlay airPlay;

    private MirroringReceiver mirroringReceiver; // Reference to MirroringReceiver
    private Thread airPlayReceiverThread;
    private Thread audioReceiverThread;
    private Thread audioControlServerThread;


    Session() {
        airPlay = new AirPlay();
    }

    public AirPlay getAirPlay() {
        return airPlay;
    }

    public void setAirPlayReceiverThread(Thread airPlayReceiverThread, MirroringReceiver receiver) {
        this.airPlayReceiverThread = airPlayReceiverThread;
        this.mirroringReceiver = receiver;
        Log.d("Session", "setAirPlayReceiverThread: " + airPlayReceiverThread.getId());
        LogRepository.INSTANCE.addLog("Session", "setAirPlayReceiverThread: " + airPlayReceiverThread.getId(), 'I');
    }

    public void setAudioReceiverThread(Thread audioReceiverThread) {
        this.audioReceiverThread = audioReceiverThread;
        Log.d("Session", "setAudioReceiverThread: " + audioReceiverThread.getId());
        LogRepository.INSTANCE.addLog("Session", "setAudioReceiverThread: " + audioReceiverThread.getId(), 'I');
    }

    public void setAudioControlServerThread(Thread audioControlServerThread) {
        this.audioControlServerThread = audioControlServerThread;
        Log.d("Session", "setAudioControlServerThread: " + audioControlServerThread.getId());
        LogRepository.INSTANCE.addLog("Session", "setAudioControlServerThread: " + audioControlServerThread.getId(), 'I');
    }

    public boolean isMirroringActive() {
        return mirroringReceiver != null && airPlayReceiverThread != null && airPlayReceiverThread.isAlive();
    }

    public boolean isAudioActive() {
        return audioReceiverThread != null && audioControlServerThread != null;
    }

//    public void stopMirroring() {
//        if (airPlayReceiverThread != null) {
//            Log.d("Session", "Stopping MirroringReceiver with Thread ID: " + airPlayReceiverThread.getId());
//            LogRepository.INSTANCE.addLog("Session", "Stopping MirroringReceiver with Thread ID: " + airPlayReceiverThread.getId());
//            airPlayReceiverThread.interrupt();
//            airPlayReceiverThread = null;
//        }
//        // TODO destroy fair play video decryptor
//        // Controlla e distrugge il decryptor di FairPlay, se necessario
//        if (airPlay != null) {
//            airPlay.releaseDecryptors(); // Assicurati che questo metodo esista o modificalo come necessario
//            LogRepository.INSTANCE.addLog("Session", "airPlay.releaseDecryptors()");
//        }
//    }

    public void stopMirroring() {
        if (mirroringReceiver != null) {
            mirroringReceiver.shutdown(); // Arresta MirroringReceiver in modo ordinato
            try {
                if (airPlayReceiverThread != null && airPlayReceiverThread.isAlive()) {
                    airPlayReceiverThread.join(); // Assicura la chiusura completa del thread
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e("Session", "Interrupted while waiting for MirroringReceiver to terminate", e);
                LogRepository.INSTANCE.addLog("Session", "Interrupted while waiting for MirroringReceiver to terminate", 'E');
            }
            mirroringReceiver = null;
            airPlayReceiverThread = null;
            Log.d("Session", "MirroringReceiver stopped.");
        }
    }

    public void stopAudio() {
        if (audioReceiverThread != null) {
            audioReceiverThread.interrupt();
            audioReceiverThread = null;
        }
        if (audioControlServerThread != null) {
            audioControlServerThread.interrupt();
            audioControlServerThread = null;
        }
    }

    public Thread getAirPlayReceiverThread() {
        return airPlayReceiverThread;
    }
}
