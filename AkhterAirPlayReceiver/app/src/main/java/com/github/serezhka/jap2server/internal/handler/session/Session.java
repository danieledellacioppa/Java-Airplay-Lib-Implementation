package com.github.serezhka.jap2server.internal.handler.session;

import android.util.Log;

import com.cjx.airplayjavademo.tools.LogRepository;
import com.github.serezhka.jap2lib.AirPlay;
import com.github.serezhka.jap2server.internal.MirroringReceiver;

public class Session {
    private static final String TAG = "Session";
    private static final long THREAD_JOIN_TIMEOUT_MS = 2000;

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
        Log.d(TAG, "setAirPlayReceiverThread: " + airPlayReceiverThread.getId());
        LogRepository.INSTANCE.addLog(TAG, "setAirPlayReceiverThread: " + airPlayReceiverThread.getId(), 'I');
    }

    public void setAudioReceiverThread(Thread audioReceiverThread) {
        this.audioReceiverThread = audioReceiverThread;
        Log.d(TAG, "setAudioReceiverThread: " + audioReceiverThread.getId());
        LogRepository.INSTANCE.addLog(TAG, "setAudioReceiverThread: " + audioReceiverThread.getId(), 'I');
    }

    public void setAudioControlServerThread(Thread audioControlServerThread) {
        this.audioControlServerThread = audioControlServerThread;
        Log.d(TAG, "setAudioControlServerThread: " + audioControlServerThread.getId());
        LogRepository.INSTANCE.addLog(TAG, "setAudioControlServerThread: " + audioControlServerThread.getId(), 'I');
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
        LogRepository.INSTANCE.addLog(TAG, "stopMirroring requested. active=" + isMirroringActive(), 'I');
        if (mirroringReceiver != null) {
            mirroringReceiver.shutdown(); // Arresta MirroringReceiver in modo ordinato
            try {
                if (airPlayReceiverThread != null && airPlayReceiverThread.isAlive()) {
                    LogRepository.INSTANCE.addLog(TAG, "Joining MirroringReceiver thread id=" +
                            airPlayReceiverThread.getId(), 'I');
                    airPlayReceiverThread.join(THREAD_JOIN_TIMEOUT_MS);
                    if (airPlayReceiverThread.isAlive()) {
                        LogRepository.INSTANCE.addLog(TAG, "MirroringReceiver thread still alive after " +
                                THREAD_JOIN_TIMEOUT_MS + "ms; interrupting.", 'W');
                        airPlayReceiverThread.interrupt();
                    } else {
                        LogRepository.INSTANCE.addLog(TAG, "MirroringReceiver thread joined", 'I');
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(TAG, "Interrupted while waiting for MirroringReceiver to terminate", e);
                LogRepository.INSTANCE.addLog(TAG, "Interrupted while waiting for MirroringReceiver to terminate", 'E');
            }
            mirroringReceiver = null;
            airPlayReceiverThread = null;
            Log.d(TAG, "MirroringReceiver stopped.");

            if (airPlay != null) {
                airPlay.releaseDecryptors(); // Assicurati che questo metodo esista o modificalo come necessario
                LogRepository.INSTANCE.addLog(TAG, "airPlay.releaseDecryptors()", 'I');
            }
        }
    }

    public void stopAudio() {
        LogRepository.INSTANCE.addLog(TAG, "stopAudio requested. active=" + isAudioActive(), 'I');
        if (audioReceiverThread != null) {
            audioReceiverThread.interrupt();
            LogRepository.INSTANCE.addLog(TAG, "Audio receiver thread interrupted", 'I');
            audioReceiverThread = null;
        }
        if (audioControlServerThread != null) {
            audioControlServerThread.interrupt();
            LogRepository.INSTANCE.addLog(TAG, "Audio control thread interrupted", 'I');
            audioControlServerThread = null;
        }
    }

    public Thread getAirPlayReceiverThread() {
        return airPlayReceiverThread;
    }
}
