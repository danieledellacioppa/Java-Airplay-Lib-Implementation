package org.example;

import com.github.serezhka.jap2lib.rtsp.AudioStreamInfo;
import com.github.serezhka.jap2lib.rtsp.VideoStreamInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.channels.FileChannel;

public class AirplayDataConsumerImpl implements AirplayDataConsumer {

    private FileChannel videoFileChannel;
    private FileChannel audioFileChannel;

    public AirplayDataConsumerImpl() throws IOException {
        // Crea file per salvare i dati video e audio
        videoFileChannel = FileChannel.open(Paths.get("video.h264"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        audioFileChannel = FileChannel.open(Paths.get("audio.pcm"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    @Override
    public void onVideo(byte[] video) {
        // Scrive i dati video nel file
        try {
            videoFileChannel.write(ByteBuffer.wrap(video));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onVideoFormat(VideoStreamInfo videoStreamInfo) {
        // Logga o gestisce informazioni sul formato video
        System.out.println("Video format received: " + videoStreamInfo.toString());
    }

    @Override
    public void onAudio(byte[] audio) {
        // Scrive i dati audio nel file
        try {
            audioFileChannel.write(ByteBuffer.wrap(audio));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAudioFormat(AudioStreamInfo audioInfo) {
        // Logga o gestisce informazioni sul formato audio
        System.out.println("Audio format received: " + audioInfo.toString());
    }

    // Metodo per chiudere i file
    public void closeChannels() throws IOException {
        if (videoFileChannel != null) {
            videoFileChannel.close();
        }
        if (audioFileChannel != null) {
            audioFileChannel.close();
        }
    }
}