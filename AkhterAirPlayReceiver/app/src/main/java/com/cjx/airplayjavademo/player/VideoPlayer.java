package com.cjx.airplayjavademo.player;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;


import com.cjx.airplayjavademo.model.NALPacket;
import com.cjx.airplayjavademo.tools.LogRepository;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class VideoPlayer {
    private static final String TAG = "VideoPlayer";
    private static final String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    private final int mVideoWidth = 540;
    private final int mVideoHeight = 960;
    private final MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private MediaCodec mDecoder = null;
    private final Surface mSurface;
    private BlockingQueue<NALPacket> packets = new LinkedBlockingQueue<>(500);
    private final HandlerThread mDecodeThread = new HandlerThread("VideoDecoder");

    private final MediaCodec.Callback mDecoderCallback = new MediaCodec.Callback() {
        @Override
        public void onInputBufferAvailable(MediaCodec codec, int index) {
            try {
                NALPacket packet = packets.take();
                ByteBuffer inputBuffer = codec.getInputBuffer(index);
                if (inputBuffer != null) {
                    inputBuffer.put(packet.nalData);
                    codec.queueInputBuffer(index, 0, packet.nalData.length, packet.pts, 0);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Error while waiting for NALPacket", e);
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error while queuing input buffer", e);
            }
        }

        @Override
        public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
            try {
                // Rilascia il buffer all'output Surface
                codec.releaseOutputBuffer(index, true);
                LogRepository.INSTANCE.addLog(TAG, "Released output buffer index=" + index, 'I');
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error releasing output buffer", e);
            }
        }

        @Override
        public void onError(MediaCodec codec, MediaCodec.CodecException e) {
            Log.e(TAG, "Decoder error", e);
        }

        @Override
        public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
            Log.i(TAG, "Output format changed to " + format);
        }
    };

    public VideoPlayer(Surface surface, int width, int heigth) {
//        this.mVideoWidth=width;
//        this.mVideoHeight=heigth;
        mSurface = surface;
    }

    public void initDecoder() {
        mDecodeThread.start();
        try {
            // 解码分辨率
            Log.i(TAG, "initDecoder: mVideoWidth=" + mVideoWidth + "---mVideoHeight=" + mVideoHeight);
            mDecoder = MediaCodec.createDecoderByType(MIME_TYPE);
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mVideoWidth, mVideoHeight);
            mDecoder.configure(format, mSurface, null, 0);
            mDecoder.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            mDecoder.setCallback(mDecoderCallback, new Handler(mDecodeThread.getLooper()));
            mDecoder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addPacket(NALPacket nalPacket) {
        try {
            packets.put(nalPacket);
        } catch (InterruptedException e) {
            // 队列满了
            Log.e(TAG, "run: put error:", e);
        }
    }

    public void start() {
        initDecoder();
    }


    public void release() {
        if (mDecoder != null) {
            try {
                mDecoder.stop();
                mDecoder.release();
            } catch (Exception e) {
                Log.e(TAG, "Error while releasing MediaCodec", e);
            }
        }

        if (mDecodeThread != null && mDecodeThread.isAlive()) {
            mDecodeThread.quitSafely();
        }

        packets.clear();  // Rilascia il buffer dei pacchetti

        Log.d(TAG, "VideoPlayer resources released.");

    }

    public void stopVideoPlay() {
        try {
            mDecoder.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            mDecoder.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mDecodeThread.quit();
        packets.clear();
    }

    public void stopPlayer() {
        if (mDecoder != null) {
            try {
                mDecoder.stop();
                mDecoder.release();
            } catch (Exception e) {
                Log.e(TAG, "Errore durante il rilascio del MediaCodec", e);
                LogRepository.INSTANCE.addLog(TAG, "Errore durante il rilascio del MediaCodec", 'E');
            }
        }

        if (mDecodeThread != null && mDecodeThread.isAlive()) {
            mDecodeThread.quitSafely();
            try {
                mDecodeThread.join(); // Assicura che il thread si sia fermato
                LogRepository.INSTANCE.addLog(TAG, "VideoPlayer thread stopped.", 'W');
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(TAG, "Errore durante l'interruzione di mDecodeThread", e);
            }
        }

        packets.clear();
        Log.d(TAG, "Risorse VideoPlayer rilasciate.");
        LogRepository.INSTANCE.addLog(TAG, "Risorse VideoPlayer rilasciate.", 'I');
    }

}
