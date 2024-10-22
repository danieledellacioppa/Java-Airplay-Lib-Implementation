package com.cjx.airplayjavademo

import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import com.cjx.airplayjavademo.model.NALPacket
import com.cjx.airplayjavademo.model.PCMPacket
import com.cjx.airplayjavademo.player.AudioPlayer
import com.cjx.airplayjavademo.player.VideoPlayer
import com.github.serezhka.jap2lib.rtsp.AudioStreamInfo
import com.github.serezhka.jap2lib.rtsp.VideoStreamInfo
import com.github.serezhka.jap2server.AirPlayServer
import com.github.serezhka.jap2server.AirplayDataConsumer
import airplayjavademo.R
import java.util.LinkedList

class MainActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private lateinit var mSurfaceView: SurfaceView
    private lateinit var airPlayServer: AirPlayServer
    private var mVideoPlayer: VideoPlayer? = null
    private var mAudioPlayer: AudioPlayer? = null
    private val mVideoCacheList = LinkedList<NALPacket>()

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mSurfaceView = findViewById(R.id.surfaceView)
        mSurfaceView.holder.addCallback(this)

        mAudioPlayer = AudioPlayer().apply {
            start()
        }

        airPlayServer = AirPlayServer("AKHTER-panel", 7000, 49152, airplayDataConsumer)

        Thread(Runnable {
            try {
                airPlayServer.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, "start-server-thread").start()
    }

    override fun onStop() {
        super.onStop()
        mAudioPlayer?.stopPlay()
        mAudioPlayer = null
        mVideoPlayer?.stopVideoPlay()
        mVideoPlayer = null
        airPlayServer.stop()
    }

    private val airplayDataConsumer = object : AirplayDataConsumer {
        override fun onVideo(video: ByteArray) {
            val nalPacket = NALPacket().apply {
                nalData = video
            }

            if (mVideoPlayer != null) {
                while (mVideoCacheList.isNotEmpty()) {
                    mVideoPlayer?.addPacker(mVideoCacheList.removeFirst())
                }
                mVideoPlayer?.addPacker(nalPacket)
            } else {
                mVideoCacheList.add(nalPacket)
            }
        }

        override fun onVideoFormat(videoStreamInfo: VideoStreamInfo) {
            // Implement if needed
        }

        override fun onAudio(audio: ByteArray) {
            val pcmPacket = PCMPacket().apply {
                data = audio
            }

            mAudioPlayer?.addPacker(pcmPacket)
        }

        override fun onAudioFormat(audioInfo: AudioStreamInfo) {
            // Implement if needed
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Not needed at the moment
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (mVideoPlayer == null) {
            Log.i(TAG, "surfaceChanged: width:$width --- height:$height")
            mVideoPlayer = VideoPlayer(holder.surface, width, height).apply {
                start()
            }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Not needed at the moment
    }
}