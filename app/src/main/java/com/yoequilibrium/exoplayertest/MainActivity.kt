package com.yoequilibrium.exoplayertest

import android.content.Context
import android.content.pm.PackageManager
import android.database.MergeCursor
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.TrackSelector
import androidx.media3.exoplayer.upstream.BandwidthMeter
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.ui.PlayerView
import java.io.File
import java.util.Locale

/**@author yo,  06.2024*/
class MainActivity : AppCompatActivity() {
    private lateinit var playerView:PlayerView
    private lateinit var player:ExoPlayer
    private lateinit var tvTitle:TextView

    private var saveTimePos:Long = 0

    private var files:List<String>?=null

    private val onVideoListItemClick:(Int)->Unit = {pos->
        player.stop()
        player.seekToDefaultPosition(pos)
        player.play()
    }

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvTitle = findViewById<TextView>(R.id.tvTitle)
        playerView = findViewById<PlayerView>(R.id.playerView)
        findViewById<ImageView>(R.id.ivVidsList).setOnClickListener {
            if (!files.isNullOrEmpty())
                listDialog(this@MainActivity, files!!.toTypedArray(), onVideoListItemClick)
                    .show()
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(android.Manifest.permission.READ_MEDIA_VIDEO,android.Manifest.permission.READ_EXTERNAL_STORAGE),PERMISSION_REQUEST_CODE)


        if(savedInstanceState?.containsKey(SAVED_TIME)==true)
            saveTimePos = savedInstanceState.getLong(SAVED_TIME)


        // BandwidthMeter is used for getting default bandwidth
        val bandwidthMeter: BandwidthMeter = DefaultBandwidthMeter.getSingletonInstance(this)
        val trackSelParam = TrackSelectionParameters.Builder(this).build()

        // track selector is used to navigate between video using a default seekbar.
        val trackSelector: TrackSelector = DefaultTrackSelector(this,trackSelParam)

        player = ExoPlayer.Builder(this).setDeviceVolumeControlEnabled(true).setPauseAtEndOfMediaItems(true)
            .setLoadControl(DefaultLoadControl.Builder()
                .setPrioritizeTimeOverSizeThresholds(false)
                // minBufferMs cannot be less than bufferForPlaybackAfterRebufferMs
                .setBufferDurationsMs(500,1500,500,500)
                .build())
            .setTrackSelector(trackSelector)
            .setBandwidthMeter(bandwidthMeter)
            .build()
        player.volume = 0.1f
        player.addListener(object : Player.Listener{
            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                Log.e("MY_TEST","Player Error: "+error.message+", codeName:"+error.errorCodeName)
                Toast.makeText(this@MainActivity,"${error.errorCode} - ${error.message}",Toast.LENGTH_LONG).show()
            }

            override fun onRenderedFirstFrame() {
                super.onRenderedFirstFrame()
                Log.d("MY_TEST","Rendered 1st frame")
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                when(playbackState){
                    Player.STATE_BUFFERING -> Log.d("MY_TEST","State BUFFERING")
                    Player.STATE_ENDED -> {
                        Log.d("MY_TEST","State ENDED")
                        player.clearMediaItems()
                        saveTimePos = 0
                    }
                    Player.STATE_IDLE -> Log.d("MY_TEST","State IDLE")
                    Player.STATE_READY -> Log.d("MY_TEST","State READY")
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                super.onPlayWhenReadyChanged(playWhenReady, reason)
                Log.d("MY_TEST","PlayWhenReadyChanged: $playWhenReady by reason: "+reason.toString())
                when(reason){
                    Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY ->  Log.d("MY_TEST","REASON AUDIO_BECOMING_NOISY")

                    Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS -> Log.d("MY_TEST","REASON AUDIO_FOCUS_LOSS")

                    Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM -> Log.d("MY_TEST","REASON END_OF_MEDIA_ITEM")

                    Player.PLAY_WHEN_READY_CHANGE_REASON_REMOTE -> Log.d("MY_TEST","REASON REMOTE")

                    Player.PLAY_WHEN_READY_CHANGE_REASON_SUPPRESSED_TOO_LONG -> Log.d("MY_TEST","REASON SUPPRESSED_TOO_LONG")

                    Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST -> Log.d("MY_TEST","REASON USER_REQUEST")
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    // Active playback.
                    Log.d("MY_TEST","isPlaying")
                } else {
                    Log.d("MY_TEST","not playing")
                    // Not playing because playback is paused, ended, suppressed, or the player
                    // is buffering, stopped or failed. Check player.playWhenReady,
                    // player.playbackState, player.playbackSuppressionReason and player.playerError for details.
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                Log.d("MY_TEST","onMediaItemTransition by reason ${reason.toString()} to "+mediaItem?.mediaMetadata?.title)
                if(mediaItem!=null)
                    Toast.makeText(this@MainActivity,"Playing: id=${mediaItem.mediaId}, metadata: ${mediaItem.mediaMetadata.title}",Toast.LENGTH_SHORT).show()
                when(reason){
                    Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> Log.d("MY_TEST","TRANSITION AUTO")

                    Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> Log.d("MY_TEST","TRANSITION PLAYLIST_CHANGED")

                    Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> Log.d("MY_TEST","TRANSITION REPEAT")

                    Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> Log.d("MY_TEST","TRANSITION SEEK")
                }
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                super.onMediaMetadataChanged(mediaMetadata)
                val mediaData = with(mediaMetadata){
                    return@with TextUtils.concat(" title:$title"," mediatype:$mediaType"," descr:$description"," artist:$artist"," genre:$genre")
                }
                Log.d("MY_TEST","Media metadata changed: $mediaData")
                tvTitle.text = mediaMetadata.title
                tvTitle.bringToFront()
            }
        })

        playerView.player = player

        initVideos()
    }

    private fun addVideos(listUri:List<String>){
        val medias = listUri.map {MediaItem.Builder().setUri(it).setMediaMetadata(MediaMetadata.Builder().setTitle(File(it).name).build()).build()}
        medias.forEach { player.addMediaItem(it) }
    }

    private fun initVideos(){
        /*val DCIMDir = File("/storage/emulated/0/DCIM/Camera")
         val files = DCIMDir.listFiles()?.map { it.toURI().toString() }
         Log.d("MY_TEST","Files in Camera dir (${DCIMDir.absolutePath}): "+files.toString())*/
        files = getVideoFilesOnDevice(this).map { it.toURI().toString() }
        Log.d("MY_TEST","Files (size=${files?.size}): "+files.toString())

        if(!files.isNullOrEmpty())
            addVideos(files!!)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode==PERMISSION_REQUEST_CODE){
            if(grantResults.any { it==PackageManager.PERMISSION_GRANTED }) {
                initVideos()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        player.stop()
        saveTimePos = player.currentPosition
    }

    override fun onStart() {
        super.onStart()
        player.prepare()
        player.seekTo(saveTimePos)
    }

    override fun onPause() {
        super.onPause()
        player.pause()
        playerView.onPause()
    }

    override fun onResume() {
        super.onResume()
        playerView.onResume()
        //НЕ стратруем видео сразу, пусть сами копку жмут
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(SAVED_TIME,saveTimePos)
    }

    private companion object {
        const val PERMISSION_REQUEST_CODE = 11
        const val SAVED_TIME:String = "STATE_KEY_SAVE_TIME"

        fun listDialog(context: Context, files:Array<String>, onItemClick:(Int)->Unit):AlertDialog{
            return AlertDialog.Builder(context)
                .setTitle("Список видео")
                .setItems(files) { _, pos -> onItemClick(pos) }
                .create()
        }

        /**Все доступные видео с устройства
         * thanks to https://stackoverflow.com/a/64728234/5274775*/
        fun getVideoFilesOnDevice(context: Context): List<File> {
            val files: MutableList<File> = ArrayList()
            try {
                val columns = arrayOf(
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media.BUCKET_ID,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME
                )
                val cursor = MergeCursor(
                    arrayOf(
                        context.contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, columns, null, null, null),
                        context.contentResolver.query(MediaStore.Video.Media.INTERNAL_CONTENT_URI, columns, null, null, null)
                        //context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null, null, null),
                        //context.contentResolver.query(MediaStore.Images.Media.INTERNAL_CONTENT_URI, columns, null, null, null),
                    )
                )
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    var path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
                    val lastPoint = path.lastIndexOf(".")
                    path = path.substring(0, lastPoint) + path.substring(lastPoint).lowercase(Locale.getDefault())
                    files.add(File(path))
                    cursor.moveToNext()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return files
        }
    }
}