package com.akhter.airplaytestlab.apserver

import com.akhter.airplaytestlab.aplib.rtsp.AudioStreamInfo
import com.akhter.airplaytestlab.aplib.rtsp.VideoStreamInfo

interface AirplayDataConsumer {

    fun onVideo(video: ByteArray?)

    fun onVideoFormat(videoStreamInfo: VideoStreamInfo?)

    fun onAudio(audio: ByteArray?)

    fun onAudioFormat(audioInfo: AudioStreamInfo?)
}