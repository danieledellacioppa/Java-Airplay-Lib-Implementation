package com.cjx.airplayjavademo.player

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import com.cjx.airplayjavademo.model.NALPacket
import com.cjx.airplayjavademo.tools.LogRepository
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

class VideoPlayer(private val mSurface: Surface) {
    companion object {
        private const val TAG = "VideoPlayer.kt"
        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val VIDEO_WIDTH = 540
        private const val VIDEO_HEIGHT = 960
        private const val MAX_QUEUE_SIZE = 240
        private const val LOG_FIRST_PACKETS = 5
        private const val LOG_EVERY_PACKETS = 100
    }

    private var mDecoder: MediaCodec? = null
    private var mDecodeThread: HandlerThread? = null
    private val packets = LinkedBlockingQueue<NALPacket>(MAX_QUEUE_SIZE)
    private val availableInputBuffers = ConcurrentLinkedQueue<Int>()
    private val drainLock = Any()
    private val released = AtomicBoolean(false)
    private val failed = AtomicBoolean(false)

    private var queuedPackets = 0
    private var droppedPackets = 0
    private var inputCallbacks = 0
    private var queuedInputBuffers = 0
    private var outputBuffers = 0

    private val mDecoderCallback = object : MediaCodec.Callback() {
        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            inputCallbacks++
            if (shouldLogPacket(inputCallbacks)) {
                LogRepository.addLog(TAG, "Input buffer available index=$index queueSize=${packets.size}", 'I')
            }
            availableInputBuffers.offer(index)
            drainInputBuffers(codec)
        }

        override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
            outputBuffers++
            val surfaceValid = mSurface.isValid
            val render = surfaceValid && !released.get() && !failed.get() && info.size > 0
            try {
                codec.releaseOutputBuffer(index, render)
                if (shouldLogPacket(outputBuffers)) {
                    LogRepository.addLog(TAG, "Output buffer released index=$index render=$render " +
                            "size=${info.size} ptsUs=${info.presentationTimeUs} flags=${info.flags} " +
                            "surfaceValid=$surfaceValid", 'I')
                }
                if (!surfaceValid) {
                    failDecoder("Surface became invalid during output release", null)
                }
            } catch (e: Exception) {
                failDecoder("Error releasing output buffer index=$index", e)
            }
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
            failDecoder("MediaCodec error diagnostic=${e.diagnosticInfo} recoverable=${e.isRecoverable} transient=${e.isTransient}", e)
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            LogRepository.addLog(TAG, "Output format changed to $format", 'I')
        }
    }

    fun initDecoder() {
        if (!mSurface.isValid) {
            failDecoder("Cannot initialize decoder: Surface is invalid", null)
            return
        }

        val decodeThread = HandlerThread("VideoDecoder").also {
            it.start()
            mDecodeThread = it
        }

        try {
            LogRepository.addLog(TAG, "initDecoder: width=$VIDEO_WIDTH height=$VIDEO_HEIGHT surfaceValid=${mSurface.isValid}", 'I')
            val decoder = MediaCodec.createDecoderByType(MIME_TYPE)
            mDecoder = decoder
            val format = MediaFormat.createVideoFormat(MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT)
            decoder.setCallback(mDecoderCallback, Handler(decodeThread.looper))
            decoder.configure(format, mSurface, null, 0)
            decoder.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT)
            decoder.start()
            LogRepository.addLog(TAG, "MediaCodec decoder started.", 'I')
        } catch (e: Exception) {
            failDecoder("Error while initializing MediaCodec", e)
        }
    }

    fun addPacket(nalPacket: NALPacket) {
        val data = nalPacket.nalData
        if (released.get() || failed.get() || data == null) {
            droppedPackets++
            logDrop("Dropping video packet because decoder is unavailable or packet is empty")
            return
        }

        if (!mSurface.isValid) {
            droppedPackets++
            failDecoder("Surface invalid while adding video packet seq=${nalPacket.sequenceNumber}", null)
            return
        }

        if (!packets.offer(nalPacket)) {
            packets.poll()
            droppedPackets++
            logDrop("Dropping oldest video packet because decoder queue is full")
            if (!packets.offer(nalPacket)) {
                droppedPackets++
                logDrop("Dropping incoming video packet because decoder queue remains full")
                return
            }
        }

        queuedPackets++
        if (shouldLogPacket(queuedPackets)) {
            LogRepository.addLog(TAG, "Queued video packet seq=${nalPacket.sequenceNumber} " +
                    "ptsUs=${nalPacket.pts} bytes=${data.size} codecConfig=${nalPacket.codecConfig} " +
                    "queueSize=${packets.size}", 'I')
        }

        mDecoder?.let { drainInputBuffers(it) }
    }

    fun start() {
        initDecoder()
    }

    fun isAvailable(): Boolean {
        return !released.get() && !failed.get() && mDecoder != null && mSurface.isValid
    }

    fun release() {
        releaseInternal("release()", true)
    }

    fun stopPlayer() {
        releaseInternal("stopPlayer()", true)
    }

    private fun drainInputBuffers(codec: MediaCodec) {
        synchronized(drainLock) {
            if (released.get() || failed.get()) {
                return
            }

            while (true) {
                val inputIndex = availableInputBuffers.peek() ?: return
                val packet = packets.poll() ?: return
                availableInputBuffers.poll()

                val data = packet.nalData
                if (data == null || data.isEmpty()) {
                    droppedPackets++
                    logDrop("Dropping empty video packet before decoder input")
                    continue
                }

                try {
                    val inputBuffer = codec.getInputBuffer(inputIndex)
                    if (inputBuffer == null) {
                        droppedPackets++
                        LogRepository.addLog(TAG, "Input buffer $inputIndex is null; dropping seq=${packet.sequenceNumber}", 'E')
                        continue
                    }

                    inputBuffer.clear()
                    if (data.size > inputBuffer.capacity()) {
                        failDecoder("NAL packet larger than input buffer. seq=${packet.sequenceNumber} " +
                                "packetBytes=${data.size} capacity=${inputBuffer.capacity()}", null)
                        return
                    }

                    inputBuffer.put(data)
                    val flags = if (packet.codecConfig) MediaCodec.BUFFER_FLAG_CODEC_CONFIG else 0
                    codec.queueInputBuffer(inputIndex, 0, data.size, packet.pts, flags)
                    queuedInputBuffers++
                    if (shouldLogPacket(queuedInputBuffers)) {
                        LogRepository.addLog(TAG, "Queued decoder input index=$inputIndex seq=${packet.sequenceNumber} " +
                                "ptsUs=${packet.pts} bytes=${data.size} flags=$flags queueSize=${packets.size}", 'I')
                    }
                } catch (e: Exception) {
                    failDecoder("Error queuing decoder input index=$inputIndex seq=${packet.sequenceNumber}", e)
                    return
                }
            }
        }
    }

    private fun failDecoder(reason: String, throwable: Throwable?) {
        if (!failed.compareAndSet(false, true)) {
            return
        }
        if (throwable != null) {
            LogRepository.addLog(TAG, "$reason: ${throwable.message}", 'E')
        } else {
            LogRepository.addLog(TAG, reason, 'E')
        }

        Thread {
            releaseInternal("decoder failure: $reason", true)
        }.start()
    }

    private fun releaseInternal(reason: String, joinThread: Boolean) {
        if (!released.compareAndSet(false, true)) {
            return
        }

        LogRepository.addLog(TAG, "Releasing VideoPlayer resources. reason=$reason " +
                "queueSize=${packets.size} availableInputs=${availableInputBuffers.size}", 'I')

        packets.clear()
        availableInputBuffers.clear()

        val decoder = mDecoder
        mDecoder = null
        if (decoder != null) {
            try {
                decoder.stop()
                LogRepository.addLog(TAG, "MediaCodec stopped.", 'I')
            } catch (e: Exception) {
                LogRepository.addLog(TAG, "MediaCodec stop failed: ${e.message}", 'W')
            }
            try {
                decoder.release()
                LogRepository.addLog(TAG, "MediaCodec released.", 'I')
            } catch (e: Exception) {
                LogRepository.addLog(TAG, "MediaCodec release failed: ${e.message}", 'E')
            }
        }

        val decodeThread = mDecodeThread
        mDecodeThread = null
        if (decodeThread != null && decodeThread.isAlive) {
            decodeThread.quitSafely()
            if (joinThread && Thread.currentThread() != decodeThread) {
                try {
                    decodeThread.join(1000)
                    LogRepository.addLog(TAG, "Video decoder thread stopped. alive=${decodeThread.isAlive}", 'I')
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    LogRepository.addLog(TAG, "Interrupted while stopping decoder thread", 'E')
                }
            }
        }

        LogRepository.addLog(TAG, "VideoPlayer resources released.", 'I')
    }

    private fun logDrop(reason: String) {
        if (shouldLogPacket(droppedPackets)) {
            LogRepository.addLog(TAG, "$reason dropped=$droppedPackets queueSize=${packets.size}", 'W')
        }
    }

    private fun shouldLogPacket(count: Int): Boolean {
        return count <= LOG_FIRST_PACKETS || count % LOG_EVERY_PACKETS == 0
    }
}
