package com.akhter.airplaytestlab.aplib.rtsp

interface MediaStreamInfo {

    val streamType: StreamType

    enum class StreamType {
        AUDIO,
        VIDEO
    }
}