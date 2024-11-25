package com.akhter.airplaytestlab.player

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import com.akhter.airplaytestlab.model.NALPacket
import com.akhter.airplaytestlab.tools.LogRepository
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class VideoPlayer(private val mSurface: Surface) {
    companion object {
        private const val TAG = "VideoPlayer.kt"
        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val VIDEO_WIDTH = 540
        private const val VIDEO_HEIGHT = 960
    }

    private var mDecoder: MediaCodec? = null
    private val packets: BlockingQueue<NALPacket> = LinkedBlockingQueue(500)
    private val mDecodeThread = HandlerThread("VideoDecoder")

    private val mDecoderCallback = object : MediaCodec.Callback() {
        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            try {
                val packet = packets.take()
                val inputBuffer = codec.getInputBuffer(index)
                inputBuffer?.put(packet.nalData)
                packet.nalData?.let { codec.queueInputBuffer(index, 0, it.size, packet.pts, 0) }
            } catch (e: InterruptedException) {
                LogRepository.addLog(TAG, "Error while waiting for NALPacket", 'E')
            } catch (e: IllegalStateException) {
                LogRepository.addLog(TAG, "Error while queuing input buffer", 'E')
            }
        }

        override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
            try {
                codec.releaseOutputBuffer(index, true)
            } catch (e: IllegalStateException) {
                LogRepository.addLog(TAG, "Error releasing output buffer", 'E')
            }
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
            LogRepository.addLog(TAG, "Decoder error", 'E')
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            LogRepository.addLog(TAG, "Output format changed to $format", 'I')
        }
    }

    fun initDecoder() {
        mDecodeThread.start()
        try {
            LogRepository.addLog(TAG, "initDecoder: VIDEO_WIDTH=$VIDEO_WIDTH---VIDEO_HEIGHT=$VIDEO_HEIGHT", 'I')
            mDecoder = MediaCodec.createDecoderByType(MIME_TYPE)
            val format = MediaFormat.createVideoFormat(MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT)
            mDecoder?.apply {
                configure(format, mSurface, null, 0)
                setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                setCallback(mDecoderCallback, Handler(mDecodeThread.looper))
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            LogRepository.addLog(TAG, "Error while initializing MediaCodec", 'E')
        }
    }

    fun addPacket(nalPacket: NALPacket) {
        try {
            packets.put(nalPacket)
        } catch (e: InterruptedException) {
            LogRepository.addLog(TAG, "Error while adding NALPacket to queue", 'E')
        }
    }

    fun start() {
        initDecoder()
    }

    fun release() {
        mDecoder?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                LogRepository.addLog(TAG, "Error while releasing MediaCodec", 'E')
            }
        }

        if (mDecodeThread.isAlive) {
            mDecodeThread.quitSafely()
        }

        packets.clear()
        LogRepository.addLog(TAG, "VideoPlayer resources released.", 'I')
    }

    fun stopPlayer() {
        mDecoder?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                LogRepository.addLog(TAG, "Errore durante il rilascio del MediaCodec", 'E')
            }
        }

        if (mDecodeThread.isAlive) {
            mDecodeThread.quitSafely()
            try {
                mDecodeThread.join()
                LogRepository.addLog(TAG, "VideoPlayer thread stopped.", 'W')
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                LogRepository.addLog(TAG, "Errore durante l'interruzione di mDecodeThread", 'E')
            }
        }

        packets.clear()
        LogRepository.addLog(TAG, "Risorse VideoPlayer rilasciate.", 'I')
    }
}