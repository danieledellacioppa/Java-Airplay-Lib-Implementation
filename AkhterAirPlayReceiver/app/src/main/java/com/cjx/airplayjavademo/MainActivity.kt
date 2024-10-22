package com.cjx.airplayjavademo

import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cjx.airplayjavademo.compose.VideoDisplayComposable
import com.cjx.airplayjavademo.model.NALPacket
import com.cjx.airplayjavademo.model.PCMPacket
import com.cjx.airplayjavademo.player.AudioPlayer
import com.cjx.airplayjavademo.player.VideoPlayer
import com.github.serezhka.jap2lib.rtsp.AudioStreamInfo
import com.github.serezhka.jap2lib.rtsp.VideoStreamInfo
import com.github.serezhka.jap2server.AirPlayServer
import com.github.serezhka.jap2server.AirplayDataConsumer
import java.util.LinkedList

/**
 * # MainActivity
 *
 * The `MainActivity` class handles the initialization of the AirPlay server for mirroring video and audio
 * streams from an iOS device to an Android device. It sets up a SurfaceView to display the video content
 * and uses custom AudioPlayer and VideoPlayer classes to manage the playback.
 *
 * The activity starts the AirPlay server and listens for incoming mirroring data via the `AirplayDataConsumer` interface,
 * which processes video and audio streams.
 *
 * ## Main Features
 * - Initializes and starts the AirPlay server.
 * - Uses a `SurfaceView` for video display.
 * - Handles video and audio streams using `VideoPlayer` and `AudioPlayer`.
 * - Caches incoming video packets when the player is not ready and plays them once ready.
 *
 * ## Lifecycle Management
 * - `onCreate(Bundle?)`: Initializes the SurfaceView and AirPlay server, and starts audio playback.
 * - `onStop()`: Stops the AirPlay server and releases resources.
 *
 * ## Surface Callbacks
 * - `surfaceCreated(SurfaceHolder)`: Currently unused.
 * - `surfaceChanged(SurfaceHolder, int, int, int)`: Initializes the `VideoPlayer` when the Surface is available and starts video playback.
 * - `surfaceDestroyed(SurfaceHolder)`: Currently unused.
 *
 * ## AirplayDataConsumer
 * - `onVideo(ByteArray)`: Receives video data packets, caches them if the video player is not ready, and adds them to the player once it is ready.
 * - `onVideoFormat(VideoStreamInfo)`: Receives video format information (currently unused).
 * - `onAudio(ByteArray)`: Receives audio data packets and adds them to the audio player.
 * - `onAudioFormat(AudioStreamInfo)`: Receives audio format information (currently unused).
 *
 * ## Usage
 * This activity sets up and manages the AirPlay server for mirroring video and audio streams, while ensuring the
 * proper lifecycle management of video and audio resources.
 */




class MainActivity : ComponentActivity(), SurfaceHolder.Callback {

    private lateinit var airPlayServer: AirPlayServer
    private var mVideoPlayer: VideoPlayer? = null
    private var mAudioPlayer: AudioPlayer? = null
    private val mVideoCacheList = LinkedList<NALPacket>()

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VideoDisplayComposable(this@MainActivity)
        }

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