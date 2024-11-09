package com.github.serezhka.jap2lib;

import android.util.Log;

import com.cjx.airplayjavademo.tools.LogRepository;
import com.github.serezhka.jap2lib.rtsp.MediaStreamInfo;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Responds on pairing setup, fairplay setup requests, decrypts data
 */
public class AirPlay {

    private final Pairing pairing;
    private final FairPlay fairplay;
    private final RTSP rtsp;

    private FairPlayVideoDecryptor fairPlayVideoDecryptor;
    private FairPlayAudioDecryptor fairPlayAudioDecryptor;

    // Aggiungiamo airPlayPort e airTunesPort come attributi della classe
    private Integer airPlayPort;
    private Integer airTunesPort;

    public AirPlay() {
        pairing = new Pairing();
        fairplay = new FairPlay();
        rtsp = new RTSP();
    }

    /**
     * {@code /info}
     * <p>
     * Writes server info to output stream
     */
    public void info(OutputStream out) throws Exception {
        pairing.info(out);
    }

    /**
     * {@code /pair-setup}
     * <p>
     * Writes EdDSA public key bytes to output stream
     */
    public void pairSetup(OutputStream out) throws Exception {
        pairing.pairSetup(out);
    }

    /**
     * {@code /pair-verify}
     * <p>
     * On first request writes curve25519 public key + encrypted signature bytes to output stream;
     * On second request verifies signature
     */
    public void pairVerify(InputStream in, OutputStream out) throws Exception {
        pairing.pairVerify(in, out);
    }

    /**
     * Pair was verified successfully
     */
    public boolean isPairVerified() {
        return pairing.isPairVerified();
    }

    /**
     * {@code /fp-setup}
     * <p>
     * Writes fp-setup response bytes to output stream
     */
    public void fairPlaySetup(InputStream in, OutputStream out) throws Exception {
        fairplay.fairPlaySetup(in, out);
    }

    /**
     * Retrieves information about media stream from RTSP SETUP request
     *
     * @return null if there's no stream info
     */
    public MediaStreamInfo rtspGetMediaStreamInfo(InputStream in) throws Exception {
        LogRepository.INSTANCE.addLog("AirPlay", "Getting media stream info...", 'I');
        return rtsp.getMediaStreamInfo(in);
    }

    public int rtspSetParameterInfo(InputStream in) throws Exception {
        int volume = rtsp.getSetParameterVolume(in);
        return volume;
    }


    /**
     * {@code RTSP SETUP ENCRYPTION}
     * <p>
     * Retrieves encrypted EAS key and IV
     */
    public void rtspSetupEncryption(InputStream in) throws Exception {
        rtsp.setup(in);
    }

    /**
     * {@code RTSP SETUP VIDEO}
     * <p>
     * Writes video event, data and timing ports info to output stream
     */
    public void rtspSetupVideo(OutputStream out, int videoDataPort, int videoEventPort, int videoTimingPort) throws Exception {
        rtsp.setupVideo(out, videoDataPort, videoEventPort, videoTimingPort);
        if (this.airPlayPort == null) {
            this.airPlayPort = videoDataPort; // Salva airPlayPort
            Log.d("AirPlay", "AirPlay port saved: " + airPlayPort);
            LogRepository.INSTANCE.addLog("AirPlay", "AirPlay port saved: " + airPlayPort, 'I');
        }
        if (this.airTunesPort == null) {
            this.airTunesPort = videoEventPort; // Salva airTunesPort
            Log.d("AirPlay", "AirTunes port saved: " + airTunesPort);
            LogRepository.INSTANCE.addLog("AirPlay", "AirTunes port saved: " + airTunesPort, 'I');
        }
    }

    /**
     * {@code RTSP SETUP AUDIO}
     * <p>
     * Writes audio control and data ports info to output stream
     */
    public void rtspSetupAudio(OutputStream out, int audioDataPort, int audioControlPort) throws Exception {
        rtsp.setupAudio(out, audioDataPort, audioControlPort);
    }

    public byte[] getFairPlayAesKey() {
        return fairplay.decryptAesKey(rtsp.getEncryptedAESKey());
    }

    /**
     * @return {@code true} if we got shared secret during pairing, ekey & stream connection id during RTSP SETUP
     */
    public boolean isFairPlayVideoDecryptorReady() {
        return pairing.getSharedSecret() != null && rtsp.getEncryptedAESKey() != null && rtsp.getStreamConnectionID() != null;
    }

    /**
     * @return {@code true} if we got shared secret during pairing, ekey & eiv during RTSP SETUP
     */
    public boolean isFairPlayAudioDecryptorReady() {
        return pairing.getSharedSecret() != null && rtsp.getEncryptedAESKey() != null && rtsp.getEiv() != null;
    }

    public void decryptVideo(byte[] video) throws Exception {
        if (fairPlayVideoDecryptor == null) {
            if (!isFairPlayVideoDecryptorReady()) {
                throw new IllegalStateException("FairPlayVideoDecryptor not ready!");
            }
            fairPlayVideoDecryptor = new FairPlayVideoDecryptor(getFairPlayAesKey(), pairing.getSharedSecret(), rtsp.getStreamConnectionID());
        }
        fairPlayVideoDecryptor.decrypt(video);
    }

    public void decryptAudio(byte[] audio, int audioLength) throws Exception {
        if (fairPlayAudioDecryptor == null) {
            if (!isFairPlayAudioDecryptorReady()) {
                throw new IllegalStateException("FairPlayAudioDecryptor not ready!");
            }
            fairPlayAudioDecryptor = new FairPlayAudioDecryptor(getFairPlayAesKey(), rtsp.getEiv(), pairing.getSharedSecret());
        }
        fairPlayAudioDecryptor.decrypt(audio, audioLength);
    }

    public void printPlist(String methodName, InputStream inputStream) {
        rtsp.printPlist(methodName, inputStream);
    }

    // Aggiungi questo metodo in AirPlay
    public void releaseDecryptors() {
        if (fairPlayVideoDecryptor != null) {
            fairPlayVideoDecryptor = null;
            Log.d("AirPlay", "FairPlay video decryptor released.");
            LogRepository.INSTANCE.addLog("AirPlay", "FairPlay video decryptor released.", 'I');
        }
        if (fairPlayAudioDecryptor != null) {
            fairPlayAudioDecryptor = null;
            Log.d("AirPlay", "FairPlay audio decryptor released.");
            LogRepository.INSTANCE.addLog("AirPlay", "FairPlay audio decryptor released.", 'I');
        }
    }

    // Implementazione del metodo sendReconnectRequest
    public void sendReconnectRequest() {
        if (airPlayPort == null || airTunesPort == null) {
            Log.e("AirPlay", "Ports not set. Cannot send reconnect request.");
            LogRepository.INSTANCE.addLog("AirPlay", "Ports not set. Cannot send reconnect request.", 'E');
            return;
        }

        try {
            Log.d("AirPlay", "Attempting to reinitialize pairing setup for reconnection.");
            LogRepository.INSTANCE.addLog("AirPlay", "Attempting to reinitialize pairing setup for reconnection.", 'I');

            ByteArrayOutputStream tempOutput = new ByteArrayOutputStream();
            pairSetup(tempOutput);
            Log.d("AirPlay", "Pairing setup response generated for reconnection attempt.");
            LogRepository.INSTANCE.addLog("AirPlay", "Pairing setup response generated for reconnection attempt.", 'I');

            // Usa i valori salvati di airPlayPort e airTunesPort
            rtspSetupVideo(tempOutput, airPlayPort, airTunesPort, 7011);
            Log.d("AirPlay", "Video setup response generated for reconnection attempt.");
            LogRepository.INSTANCE.addLog("AirPlay", "Video setup response generated for reconnection attempt.", 'I');

        } catch (Exception e) {
            Log.e("AirPlay", "Failed to send reconnect request: " + e.getMessage(), e);
            LogRepository.INSTANCE.addLog("AirPlay", "Failed to send reconnect request: " + e.getMessage(), 'E');
        }
    }

}
