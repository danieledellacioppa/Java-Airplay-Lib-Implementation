package com.akhter.airplaytestlab.model

class NALPacket {
    var nalData: ByteArray? = null
    var nalType: Int = 0
    var pts: Long = 0
}