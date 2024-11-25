package com.akhter.airplaytestlab.aplib.rtsp

class VideoStreamInfo(val streamConnectionID: String? = null) : MediaStreamInfo {

    override val streamType: MediaStreamInfo.StreamType
        get() = MediaStreamInfo.StreamType.VIDEO
}