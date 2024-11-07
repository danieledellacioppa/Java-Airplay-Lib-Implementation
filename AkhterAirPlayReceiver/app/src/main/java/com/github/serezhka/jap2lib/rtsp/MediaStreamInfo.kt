package com.github.serezhka.jap2lib.rtsp

interface MediaStreamInfo {

    val streamType: StreamType

    enum class StreamType {
        AUDIO,
        VIDEO
    }
}
