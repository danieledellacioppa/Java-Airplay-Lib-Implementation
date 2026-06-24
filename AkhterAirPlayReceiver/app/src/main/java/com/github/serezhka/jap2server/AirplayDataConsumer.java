package com.github.serezhka.jap2server;

import com.github.serezhka.jap2lib.rtsp.AudioStreamInfo;
import com.github.serezhka.jap2lib.rtsp.VideoStreamInfo;

public interface AirplayDataConsumer {

    void onVideo(byte[] video, long timestampUs, long sequenceNumber, boolean codecConfig);

    void onVideoFormat(VideoStreamInfo videoStreamInfo);

    void onAudio(byte[] audio, long timestamp, int sequenceNumber);

    void onAudioFormat(AudioStreamInfo audioInfo);
}
