package com.github.serezhka.jap2server.internal.handler.session;

import android.util.Log;

import com.cjx.airplayjavademo.tools.LogRepository;
import com.github.serezhka.jap2lib.AirPlay;

public class Session {

    private final AirPlay airPlay;

    private Thread airPlayReceiverThread;
    private Thread audioReceiverThread;
    private Thread audioControlServerThread;

    Session() {
        airPlay = new AirPlay();
    }

    public AirPlay getAirPlay() {
        return airPlay;
    }

    public void setAirPlayReceiverThread(Thread airPlayReceiverThread) {
        this.airPlayReceiverThread = airPlayReceiverThread;
        Log.d("Session", "setAirPlayReceiverThread: " + airPlayReceiverThread);
        LogRepository.INSTANCE.addLog("Session", "setAirPlayReceiverThread: " + airPlayReceiverThread);
    }

    public void setAudioReceiverThread(Thread audioReceiverThread) {
        this.audioReceiverThread = audioReceiverThread;
        Log.d("Session", "setAudioReceiverThread: " + audioReceiverThread);
        LogRepository.INSTANCE.addLog("Session", "setAudioReceiverThread: " + audioReceiverThread);
    }

    public void setAudioControlServerThread(Thread audioControlServerThread) {
        this.audioControlServerThread = audioControlServerThread;
        Log.d("Session", "setAudioControlServerThread: " + audioControlServerThread);
        LogRepository.INSTANCE.addLog("Session", "setAudioControlServerThread: " + audioControlServerThread);
    }

    public boolean isMirroringActive() {
        return airPlayReceiverThread != null;
    }

    public boolean isAudioActive() {
        return audioReceiverThread != null && audioControlServerThread != null;
    }

    public void stopMirroring() {
        if (airPlayReceiverThread != null) {
            airPlayReceiverThread.interrupt();
            airPlayReceiverThread = null;
        }
        // TODO destroy fair play video decryptor
        // Controlla e distrugge il decryptor di FairPlay, se necessario
        if (airPlay != null) {
            airPlay.releaseDecryptors(); // Assicurati che questo metodo esista o modificalo come necessario
            LogRepository.INSTANCE.addLog("Session", "airPlay.releaseDecryptors()");
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
}
