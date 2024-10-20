package org.example;

import com.github.serezhka.jap2lib.AirPlay;
import com.github.serezhka.jap2lib.AirPlayBonjour;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class AirPlayServer {

    private final String serverName;
    private final int airPlayPort;
    private final int airTunesPort;
    private final AirPlay airPlay;
    private final AirplayDataConsumerImpl dataConsumer; // Aggiunto il dataConsumer
    private AirPlayBonjour airPlayBonjour;
    private ServerSocket rtspServerSocket;

    public AirPlayServer(String serverName, int airPlayPort, int airTunesPort, AirplayDataConsumerImpl dataConsumer) {
        this.serverName = serverName;
        this.airPlayPort = airPlayPort;
        this.airTunesPort = airTunesPort;
        this.airPlay = new AirPlay();
        this.dataConsumer = dataConsumer;  // Salva il dataConsumer
    }

    public void start() throws Exception {
        // Start Bonjour service
        airPlayBonjour = new AirPlayBonjour(serverName);
        airPlayBonjour.start(airPlayPort, airTunesPort);
        System.out.println("Bonjour service started: " + serverName);

        // Start RTSP server
        rtspServerSocket = new ServerSocket(airPlayPort);
        System.out.println("RTSP server started on port: " + airPlayPort);

        // Listen for RTSP requests
        while (true) {
            Socket clientSocket = rtspServerSocket.accept();
            handleRtspRequest(clientSocket);
        }
    }

    public void stop() throws Exception {
        if (airPlayBonjour != null) {
            airPlayBonjour.stop();
        }
        if (rtspServerSocket != null && !rtspServerSocket.isClosed()) {
            rtspServerSocket.close();
        }
    }

    private void handleRtspRequest(Socket clientSocket) {
        try (InputStream inputStream = clientSocket.getInputStream();
             OutputStream outputStream = clientSocket.getOutputStream()) {

            byte[] requestContent = inputStream.readAllBytes(); // Read the full request
            String uri = parseUriFromRequest(requestContent);   // Parse the RTSP URI

            switch (uri) {
                case "/info":
                    airPlay.info(outputStream);
                    sendRtspOk(outputStream);
                    break;
                case "/pair-setup":
                    airPlay.pairSetup(outputStream);
                    sendRtspOk(outputStream);
                    break;
                case "/pair-verify":
                    airPlay.pairVerify(new ByteArrayInputStream(requestContent), outputStream);
                    sendRtspOk(outputStream);
                    break;
                case "/fp-setup":
                    airPlay.fairPlaySetup(new ByteArrayInputStream(requestContent), outputStream);
                    sendRtspOk(outputStream);
                    break;
                case "/feedback":
                    sendRtspOk(outputStream);
                    break;
                case "/play":  // Esempio di richiesta per iniziare la riproduzione
                    // Esegui l'azione di setup RTSP per ricevere audio/video
                    airPlay.rtspSetup(new ByteArrayInputStream(requestContent), outputStream, 7002, 7003, 7004, 7005, 7006);
                    // Invio il contenuto ricevuto a dataConsumer
                    if (airPlay.isFairPlayVideoDecryptorReady()) {
                        // Esempio: ricevi dati video e inviali a dataConsumer
                        byte[] videoData = new byte[1024];  // Esempio di dati video, sostituisci con quelli veri
                        dataConsumer.onVideo(videoData);
                    }
                    if (airPlay.isFairPlayAudioDecryptorReady()) {
                        // Esempio: ricevi dati audio e inviali a dataConsumer
                        byte[] audioData = new byte[1024];  // Esempio di dati audio, sostituisci con quelli veri
                        dataConsumer.onAudio(audioData);
                    }
                    sendRtspOk(outputStream);
                    break;
                default:
                    sendRtspNotImplemented(outputStream);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String parseUriFromRequest(byte[] requestContent) {
        String requestString = new String(requestContent);
        String[] lines = requestString.split("\r\n");
        if (lines.length > 0) {
            String[] requestLineParts = lines[0].split(" ");
            if (requestLineParts.length > 1) {
                return requestLineParts[1];  // URI should be the second part
            }
        }
        return "";
    }

    private void sendRtspOk(OutputStream outputStream) throws Exception {
        String response = "RTSP/1.0 200 OK\r\nCSeq: 1\r\n\r\n";
        outputStream.write(response.getBytes());
    }

    private void sendRtspNotImplemented(OutputStream outputStream) throws Exception {
        String response = "RTSP/1.0 501 Not Implemented\r\nCSeq: 1\r\n\r\n";
        outputStream.write(response.getBytes());
    }
}