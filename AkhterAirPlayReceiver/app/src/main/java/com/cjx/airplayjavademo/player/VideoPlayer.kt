//package com.cjx.airplayjavademo.player
//
//import android.annotation.SuppressLint
//import android.media.MediaCodec
//import android.media.MediaFormat
//import android.os.Handler
//import android.os.HandlerThread
//import android.util.Log
//import android.view.Surface
//import com.cjx.airplayjavademo.model.NALPacket
//import com.cjx.airplayjavademo.tools.LogRepository
//import java.nio.ByteBuffer
//import java.util.concurrent.BlockingQueue
//import java.util.concurrent.LinkedBlockingQueue
//
//class VideoPlayer(private val surface: Surface, private val width: Int = 540, private val height: Int = 960) {
//    companion object {
//        private const val TAG = "VideoPlayer"
//        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
//    }
//
//    private val bufferInfo = MediaCodec.BufferInfo()
//    private var decoder: MediaCodec? = null
//    private val packets: BlockingQueue<NALPacket> = LinkedBlockingQueue(500)
//    private val decodeThread = HandlerThread("VideoDecoder").apply { start() }
//
//    private val decoderCallback = object : MediaCodec.Callback() {
//        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
//            try {
//                val packet = packets.take()
//                codec.getInputBuffer(index)?.apply {
//                    put(packet.nalData)
//                    decoder?.queueInputBuffer(index, 0, packet.nalData.size, packet.pts, 0)
//                }
//            } catch (e: InterruptedException) {
//                Log.e(TAG, "Interrupted when waiting for packet", e)
//                Thread.currentThread().interrupt()
//            } catch (e: IllegalStateException) {
//                Log.e(TAG, "Error queueing input buffer", e)
//            }
//        }
//
//        override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
//            try {
//                codec.releaseOutputBuffer(index, true)
//            } catch (e: IllegalStateException) {
//                Log.e(TAG, "Error releasing output buffer", e)
//            }
//        }
//
//        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
//            Log.e(TAG, "Decode error", e)
//        }
//
//        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
//            Log.i(TAG, "Output format changed: $format")
//        }
//    }
//
//    fun initDecoder() {
//        try {
//            Log.i(TAG, "Initializing decoder: width=$width, height=$height")
//            decoder = MediaCodec.createDecoderByType(MIME_TYPE).apply {
//                val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height)
//                configure(format, surface, null, 0)
//                setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT)
//                setCallback(decoderCallback, Handler(decodeThread.looper))
//                start()
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error initializing decoder", e)
//        }
//    }
//
//    fun addPacket(packet: NALPacket) {
//        try {
//            packets.put(packet)
//        } catch (e: InterruptedException) {
//            Log.e(TAG, "Error adding packet to queue", e)
//            Thread.currentThread().interrupt()
//        }
//    }
//
//    fun start() {
//        initDecoder()
//    }
//
//    fun release() {
//        try {
//            decoder?.stop()
//            decoder?.release()
//            Log.d(TAG, "Decoder stopped and released.")
//        } catch (e: Exception) {
//            Log.e(TAG, "Error releasing MediaCodec", e)
//        } finally {
//            decodeThread.quitSafely()
//            packets.clear()
//            Log.d(TAG, "VideoPlayer resources released.")
//        }
//    }
//
//    fun stopPlayer() {
//        release()
//        try {
//            decodeThread.join()
//            Log.d(TAG, "VideoPlayer thread terminated.")
//        } catch (e: InterruptedException) {
//            Log.e(TAG, "Error stopping decode thread", e)
//            Thread.currentThread().interrupt()
//        }
//    }
//
//    private fun doDecode(packet: NALPacket) {
//        val timeoutUsec = 10000L
//        if (packet.nalData == null) {
//            Log.w(TAG, "NAL data is null, skipping packet")
//            return
//        }
//
//        try {
//            val inputBufferIndex = decoder?.dequeueInputBuffer(timeoutUsec) ?: -1
//            if (inputBufferIndex >= 0) {
//                decoder?.getInputBuffer(inputBufferIndex)?.apply {
//                    put(packet.nalData)
//                    decoder?.queueInputBuffer(inputBufferIndex, 0, packet.nalData.size, packet.pts, 0)
//                }
//            } else {
//                Log.d(TAG, "Failed to dequeue input buffer")
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error during decoding process", e)
//        }
//
//        decode(timeoutUsec)
//    }
//
//    @SuppressLint("WrongConstant")
//    private fun decode(timeoutUsec: Long) {
//        try {
//            val outputBufferIndex = decoder?.dequeueOutputBuffer(bufferInfo, timeoutUsec) ?: -1
//            if (outputBufferIndex >= 0) {
//                decoder?.releaseOutputBuffer(outputBufferIndex, true)
//            } else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
//                Log.d(TAG, "No output buffer available, trying again later")
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error dequeuing output buffer", e)
//        }
//    }
//}