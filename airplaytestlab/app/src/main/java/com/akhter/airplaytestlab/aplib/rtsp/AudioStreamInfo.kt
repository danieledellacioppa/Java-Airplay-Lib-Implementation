package com.akhter.airplaytestlab.aplib.rtsp

class AudioStreamInfo : MediaStreamInfo {
    override val streamType: MediaStreamInfo.StreamType
        get() = MediaStreamInfo.StreamType.AUDIO
}