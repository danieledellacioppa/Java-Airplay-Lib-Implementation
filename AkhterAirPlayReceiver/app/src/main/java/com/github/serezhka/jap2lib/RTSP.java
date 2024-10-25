package com.github.serezhka.jap2lib;

import android.util.Log;

import com.cjx.airplayjavademo.tools.LogRepository;
import com.dd.plist.BinaryPropertyListParser;
import com.dd.plist.BinaryPropertyListWriter;
import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListFormatException;
import com.github.serezhka.jap2lib.rtsp.AudioStreamInfo;
import com.github.serezhka.jap2lib.rtsp.MediaStreamInfo;
import com.github.serezhka.jap2lib.rtsp.VideoStreamInfo;

import net.i2p.crypto.eddsa.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;



/**
 * <h1 style="color: #2e6c80;">RTSP Class</h1>
 *
 * <p style="font-size: 1.1em; color: #555555;">
 * The <strong>RTSP</strong> class is responsible for managing the Real-Time Streaming Protocol (RTSP) interactions within the AirPlay framework.
 * It provides methods for parsing RTSP setup payloads, extracting media stream information, and handling AES key decryption for secure streaming.
 * </p>
 *
 * <h2 style="color: #3a79a1;">Main Features</h2>
 * <ul>
 *   <li>Handles RTSP setup requests and extracts media stream information.</li>
 *   <li>Decrypts AES keys used for video/audio streams.</li>
 *   <li>Parses and builds binary property lists (plist) for RTSP responses.</li>
 * </ul>
 *
 * <h2 style="color: #3a79a1;">Methods Overview</h2>
 * <ul>
 *   <li><strong>getMediaStreamInfo(InputStream rtspSetupPayload)</strong>: Parses the RTSP setup payload and extracts media stream information such as video and audio stream types.</li>
 *   <li><strong>getSetParameterVolume(InputStream in)</strong>: Retrieves the volume setting from a set parameter request.</li>
 *   <li><strong>setup(InputStream in)</strong>: Processes the RTSP setup request and extracts the encrypted AES key and initialization vector (eiv).</li>
 *   <li><strong>setupVideo(OutputStream out, int videoDataPort, int videoEventPort, int videoTimingPort)</strong>: Constructs and sends a video stream setup response.</li>
 *   <li><strong>setupAudio(OutputStream out, int audioDataPort, int audioControlPort)</strong>: Constructs and sends an audio stream setup response.</li>
 *   <li><strong>printPlist(String rtspMethod, InputStream inputStream)</strong>: Prints the content of a plist received in an RTSP request for debugging purposes.</li>
 * </ul>
 *
 * <h2 style="color: #3a79a1;">Usage</h2>
 * <p style="font-size: 1.1em; color: #555555;">
 * This class is used during the setup phase of an RTSP session for AirPlay streaming. It parses the setup payload, manages AES keys for secure streams, and builds the responses needed for video/audio stream configuration.
 * </p>
 *
 * <h2 style="color: #3a79a1;">Key Methods</h2>
 * <h3 style="color: #3a79a1;">getMediaStreamInfo</h3>
 * <p style="font-size: 1.1em; color: #555555;">
 * Parses the RTSP setup payload to extract media stream information (e.g., video or audio streams). Returns a {@link MediaStreamInfo} object representing the stream information.
 * </p>
 *
 * <h3 style="color: #3a79a1;">setup</h3>
 * <p style="font-size: 1.1em; color: #555555;">
 * Reads the RTSP setup request, extracting the encrypted AES key and initialization vector (eiv) for secure communication.
 * </p>
 *
 * <h3 style="color: #3a79a1;">setupVideo</h3>
 * <p style="font-size: 1.1em; color: #555555;">
 * Sends a setup response for the video stream, including information such as data port, event port, and timing port.
 * </p>
 *
 * <h3 style="color: #3a79a1;">setupAudio</h3>
 * <p style="font-size: 1.1em; color: #555555;">
 * Sends a setup response for the audio stream, specifying the data and control ports.
 * </p>
 *
 * <h3 style="color: #3a79a1;">printPlist</h3>
 * <p style="font-size: 1.1em; color: #555555;">
 * Prints the content of a property list (plist) from an RTSP request for debugging purposes.
 * </p>
 *
 * <h2 style="color: #3a79a1;">Attributes</h2>
 * <ul>
 *   <li><strong>streamConnectionID</strong>: The connection ID for the current RTSP stream.</li>
 *   <li><strong>encryptedAESKey</strong>: The encrypted AES key used for secure streaming.</li>
 *   <li><strong>eiv</strong>: The initialization vector (eiv) for AES decryption.</li>
 * </ul>
 */
class RTSP {

    private static final Logger log = LoggerFactory.getLogger(RTSP.class);
    private static String TAG = "RTSP";

    private String streamConnectionID;
    private byte[] encryptedAESKey;
    private byte[] eiv;

    /**
     * Parses the RTSP setup payload and extracts media stream information (e.g., video or audio streams).
     * Returns a {@link MediaStreamInfo} object representing the stream information.
     *
     * @param rtspSetupPayload The input stream containing the RTSP setup payload
     * @return A {@link MediaStreamInfo} object representing the stream information
     * @throws Exception If an error occurs while parsing the RTSP setup payload
     */
    MediaStreamInfo getMediaStreamInfo(InputStream rtspSetupPayload) throws Exception {
        // Verifica iniziale della disponibilità di dati nello stream
        if (rtspSetupPayload == null || rtspSetupPayload.available() == 0) {
            log.error("RTSP setup payload is empty or null");
            Log.d(TAG, "getMediaStreamInfo: RTSP setup payload is empty or null");
            return null;
        }

        try {
            // Log della disponibilità di dati nello stream
            log.info("Available bytes in RTSP setup payload: {}", rtspSetupPayload.available());

            // Parsing del payload RTSP
            NSDictionary rtspSetup = (NSDictionary) BinaryPropertyListParser.parse(rtspSetupPayload);
            log.info("Parsed RTSP setup payload: {}", rtspSetup.toXMLPropertyList());

            if (rtspSetup.containsKey("streams")) {
                // Assumiamo che ci sia solo un stream info per richiesta RTSP SETUP
                HashMap<String, Object> stream = (HashMap<String, Object>) ((Object[]) rtspSetup.get("streams").toJavaObject())[0];
                int type = (int) stream.get("type");

                // Controlla il tipo di stream (video o audio)
                switch (type) {
                    case 110:  // Video stream
                        log.info("Video stream detected");

                        // Log dei dettagli sullo stream video
                        if (stream.containsKey("streamConnectionID")) {
                            streamConnectionID = Long.toUnsignedString((long) stream.get("streamConnectionID"));
                            log.info("Video streamConnectionID: {}", streamConnectionID);
                        }
                        return new VideoStreamInfo(streamConnectionID);

                    case 96:   // Audio stream
                        log.info("Audio stream detected");

                        // Costruzione delle informazioni audio dal payload
                        AudioStreamInfo.AudioStreamInfoBuilder builder = new AudioStreamInfo.AudioStreamInfoBuilder();
                        if (stream.containsKey("ct")) {
                            int compressionType = (int) stream.get("ct");
                            builder.compressionType(AudioStreamInfo.CompressionType.fromCode(compressionType));
                            log.info("Audio compressionType: {}", compressionType);
                        }
                        if (stream.containsKey("audioFormat")) {
                            long audioFormatCode = (int) stream.get("audioFormat");  // FIXME: Verifica se int o long
                            builder.audioFormat(AudioStreamInfo.AudioFormat.fromCode(audioFormatCode));
                            log.info("Audio format: {}", audioFormatCode);
                        }
                        if (stream.containsKey("spf")) {
                            int samplesPerFrame = (int) stream.get("spf");
                            builder.samplesPerFrame(samplesPerFrame);
                            log.info("Samples per frame: {}", samplesPerFrame);
                        }
                        return builder.build();

                    default:
                        log.error("Unknown stream type: {}", type);
                        break;
                }
            } else {
                log.warn("RTSP setup does not contain 'streams' key. Payload: {}", rtspSetup.toXMLPropertyList());
            }

        } catch (ClassCastException e) {
            log.error("Error casting RTSP setup stream data: {}", e.getMessage(), e);
        } catch (IOException e) {
            log.error("IO error while parsing RTSP setup payload: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to parse RTSP setup payload: {}", e.getMessage(), e);
        }

        return null;
    }

    /**
     * Retrieves the volume setting from a set parameter request.
     *
     * @param in The input stream containing the set parameter request
     * @return The volume setting value
     * @throws IOException If an I/O error occurs while reading the input stream
     * @throws PropertyListFormatException If the property list format is invalid
     */
    int getSetParameterVolume(InputStream in) throws IOException, PropertyListFormatException {
        StringBuilder textBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader
                (in, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                textBuilder.append((char) c);
            }
        }
        String data = textBuilder.toString().trim();
        // ios 音量 -30----》0由小变大
        int volume = -15;
        try {
            volume = (int) Float.parseFloat(data.split(":")[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, "getSetParameterVolume: " + volume);
        return volume;
    }

    /**
     * Processes the RTSP setup request and extracts the encrypted AES key and initialization vector (eiv) for secure communication.
     *
     * @param in The input stream containing the RTSP setup request
     * @throws Exception If an error occurs while processing the RTSP setup request
     */
    void setup(InputStream in) throws Exception {
        NSDictionary request = (NSDictionary) BinaryPropertyListParser.parse(in);

        if (request.containsKey("ekey")) {
            encryptedAESKey = (byte[]) request.get("ekey").toJavaObject();
            log.info("Encrypted AES key: " + Utils.bytesToHex(encryptedAESKey));
        }

        if (request.containsKey("eiv")) {
            eiv = (byte[]) request.get("eiv").toJavaObject();
            log.info("AES eiv: " + Utils.bytesToHex(eiv));
        }
    }

    /**
     * Constructs and sends a setup response for the video stream, including information such as data port, event port, and timing port.
     *
     * @param out            The output stream for writing the video stream setup response
     * @param videoDataPort  The data port for the video stream
     * @param videoEventPort The event port for the video stream
     * @param videoTimingPort The timing port for the video stream
     * @throws IOException If an I/O error occurs while writing the video stream setup response
     */
    void setupVideo(OutputStream out, int videoDataPort, int videoEventPort, int videoTimingPort) throws IOException {
        NSArray streams = new NSArray(1);
        NSDictionary dataStream = new NSDictionary();
        dataStream.put("dataPort", videoDataPort);
        dataStream.put("type", 110);
        streams.setValue(0, dataStream);

        NSDictionary response = new NSDictionary();
        response.put("streams", streams);
        response.put("eventPort", videoEventPort);
        response.put("timingPort", videoTimingPort);
        BinaryPropertyListWriter.write(out, response);
    }

    /**
     * Constructs and sends a setup response for the audio stream, specifying the data and control ports.
     *
     * @param out             The output stream for writing the audio stream setup response
     * @param audioDataPort   The data port for the audio stream
     * @param audioControlPort The control port for the audio stream
     * @throws IOException If an I/O error occurs while writing the audio stream setup response
     */
    void setupAudio(OutputStream out, int audioDataPort, int audioControlPort) throws IOException {
        NSArray streams = new NSArray(1);
        NSDictionary dataStream = new NSDictionary();
        dataStream.put("dataPort", audioDataPort);
        dataStream.put("type", 96);
        dataStream.put("controlPort", audioControlPort);
        streams.setValue(0, dataStream);

        NSDictionary response = new NSDictionary();
        response.put("streams", streams);
        BinaryPropertyListWriter.write(out, response);
    }

    String getStreamConnectionID() {
        return streamConnectionID;
    }

    byte[] getEncryptedAESKey() {
        return encryptedAESKey;
    }

    byte[] getEiv() {
        return eiv;
    }

    /**
     * Prints the content of a property list (plist) from an RTSP request for debugging purposes.
     *
     * @param rtspMethod   The RTSP method (e.g., SETUP, PLAY, etc.)
     * @param inputStream The input stream containing the property list data
     */
    public void printPlist(String rtspMethod, InputStream inputStream) {

        try {
            NSDictionary plist = (NSDictionary) BinaryPropertyListParser.parse(inputStream);
            Log.i(TAG, rtspMethod + " plist 01: " + plist.toXMLPropertyList());
            LogRepository.INSTANCE.addLog(TAG, rtspMethod + " plist 01: " + plist.toXMLPropertyList());
        } catch (Exception e) {
            StringBuilder textBuilder = new StringBuilder();
            try (Reader reader = new BufferedReader(new InputStreamReader
                    (inputStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
                int c = 0;
                while ((c = reader.read()) != -1) {
                    textBuilder.append((char) c);
                }
                Log.i(TAG, rtspMethod + " plist 02: " + textBuilder.toString());
                LogRepository.INSTANCE.addLog(TAG, rtspMethod + " plist 02: " + textBuilder.toString());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            Log.e(TAG, rtspMethod+ "--printPlist: ", e);
            LogRepository.INSTANCE.addLog(TAG, rtspMethod + "--printPlist: " + e.getMessage());

        }
    }

}
