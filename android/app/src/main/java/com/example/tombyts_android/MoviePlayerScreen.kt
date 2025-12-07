package com.example.tombyts_android

import android.app.Activity
import android.net.Uri
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(UnstableApi::class)
@Composable
fun MoviePlayerScreen(movieTitle: String, token: String, navController: NavController) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    var mediaSession by remember { mutableStateOf<MediaSession?>(null) }
    var moviePath: String? by remember { mutableStateOf(null) }
    var playWhenReady by rememberSaveable { mutableStateOf(true) }
    var progress by rememberSaveable { mutableDoubleStateOf(0.0) }
    val hasSeeked = rememberSaveable { mutableStateOf(false) }
    val lifecycleOwner: LifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    var areControlsVisible by remember { mutableStateOf(false) }

    // Back button handling - close controls if open, otherwise return to MovieListActivity
    BackHandler {
        if (areControlsVisible) {
            Log.d("MoviePlayerKeys", "🔙 Back pressed - hiding controls")
            playerView?.hideController()
        } else {
            Log.d("MoviePlayerKeys", "🔙 Back pressed - returning to MovieListActivity")
            // Since we came from MovieListActivity with FLAG_ACTIVITY_CLEAR_TOP,
            // we need to explicitly go back to it and restore the selected item
            val intent = android.content.Intent(context, MovieListActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("token", token)
                putExtra("restore_selection", movieTitle) // Pass the movie title to restore selection
            }
            context.startActivity(intent)
            (context as? Activity)?.finish()
        }
    }

    // Lifecycle handling - pause/resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    playWhenReady = player.playWhenReady
                    player.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (playWhenReady) {
                        player.play()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Player listener for seeking to saved progress and error handling
    val playerListener = remember {
        object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && !hasSeeked.value) {
                    if (player.duration > 0) {
                        val position = (player.duration * (progress / 100)).toLong()
                        player.seekTo(position)
                        hasSeeked.value = true
                        Log.d("MoviePlayer", "Seeked to saved progress: ${progress}%")
                    }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e("MoviePlayer", "Playback error for '$movieTitle': ${error.message}", error)
                Log.e("MoviePlayer", "Error code: ${error.errorCodeName} (${error.errorCode})")

                // Show error dialog on main thread
                (context as? Activity)?.runOnUiThread {
                    val errorMessage = when (error.errorCode) {
                        androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                        androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                        androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                            "Network error: Unable to load the video. Please check your connection.\n\n" +
                            "File: $movieTitle"

                        androidx.media3.common.PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                        androidx.media3.common.PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                            "This video file appears to be corrupted or has invalid metadata.\n\n" +
                            "The file may need to be re-encoded. You can try:\n" +
                            "ffmpeg -i input.mp4 -c copy -movflags +faststart output.mp4\n\n" +
                            "File: $movieTitle"

                        androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                        androidx.media3.common.PlaybackException.ERROR_CODE_DECODING_FAILED ->
                            "Unable to decode this video. The format may not be supported by your device.\n\n" +
                            "File: $movieTitle"

                        androidx.media3.common.PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                        androidx.media3.common.PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                        androidx.media3.common.PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE ->
                            "Unable to read the video file. The file may be corrupted, missing, or inaccessible.\n\n" +
                            "File: $movieTitle\n" +
                            "Error: ${error.errorCodeName}"

                        else -> {
                            val errorDetails = error.cause?.message ?: error.message ?: "Unknown error"
                            "An error occurred while playing this video.\n\n" +
                            "File: $movieTitle\n" +
                            "Error: ${error.errorCodeName}\n" +
                            "Details: $errorDetails"
                        }
                    }

                    android.app.AlertDialog.Builder(context)
                        .setTitle("Playback Error")
                        .setMessage(errorMessage)
                        .setCancelable(false)
                        .setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                            Log.d("MoviePlayer", "Error dialog dismissed, navigating back to MovieListActivity")

                            // Navigate back to MovieListActivity with selection restored
                            val intent = android.content.Intent(context, MovieListActivity::class.java).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                                putExtra("token", token)
                                putExtra("restore_selection", movieTitle)
                            }
                            context.startActivity(intent)
                            (context as? Activity)?.finish()
                        }
                        .show()
                }
            }
        }
    }

    LaunchedEffect(player) {
        player.addListener(playerListener)
    }

    // Fetch movie details
    LaunchedEffect(Unit) {
        try {
            val response = Classes.ApiProvider.apiService.getMovieDetails(movieTitle, "Bearer $token")
            if (response.isSuccessful) {
                moviePath = response.body()?.path?.let { Uri.encode(it) }
                Log.d("MoviePlayer", "Movie path loaded: $moviePath")
            } else {
                Log.e("MoviePlayer", "Failed to fetch movie: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("MoviePlayer", "Error fetching movie: ${e.message}", e)
        }
    }

    // Load media when path is available
    LaunchedEffect(moviePath) {
        mediaSession?.release()
        mediaSession = MediaSession.Builder(context, player).build()
        hasSeeked.value = false

        if (moviePath != null) {
            // Fetch progress
            try {
                val progressResponse = Classes.ApiProvider.apiService.getProgress(movieTitle, "Bearer $token")
                if (progressResponse.isSuccessful) {
                    progress = progressResponse.body()?.progress ?: 0.0
                    Log.d("MoviePlayer", "Progress: ${progress}%")
                }
            } catch (e: Exception) {
                Log.e("MoviePlayer", "Error fetching progress: ${e.message}", e)
            }

            val mediaItemBuilder = MediaItem.Builder()
                .setUri(Uri.parse("https://${BuildConfig.SERVER_IP}:3001/media/$moviePath"))

            // Fetch subtitles
            try {
                val subtitleResponse = Classes.ApiProvider.getSubtitleApiService()
                    .getSubtitles(movieTitle, "en", "Bearer $token")

                if (subtitleResponse.isSuccessful) {
                    val subtitleContent = subtitleResponse.body()
                    if (!subtitleContent.isNullOrBlank()) {
                        val tempFile = File.createTempFile("subtitle", ".vtt", context.cacheDir)
                        FileOutputStream(tempFile).use { it.write(subtitleContent.toByteArray()) }

                        val subtitle = SubtitleConfiguration.Builder(Uri.fromFile(tempFile))
                            .setMimeType("text/vtt")
                            .setLanguage("en")
                            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                            .build()

                        mediaItemBuilder.setSubtitleConfigurations(listOf(subtitle))
                        Log.d("MoviePlayer", "Subtitles loaded")
                    }
                }
            } catch (e: Exception) {
                Log.e("MoviePlayer", "Error fetching subtitles: ${e.message}", e)
            }

            player.setMediaItem(mediaItemBuilder.build())
            player.prepare()
            player.playWhenReady = playWhenReady
        }
    }

    // Update progress periodically
    LaunchedEffect(player.isPlaying) {
        while (true) {
            delay(5000)
            if (player.isPlaying && player.duration > 0) {
                val currentProgress = (player.currentPosition.toDouble() / player.duration.toDouble()) * 100
                try {
                    val response = Classes.ApiProvider.apiService.updateProgress(
                        movieTitle,
                        "Bearer $token",
                        ProgressUpdate(currentProgress.toInt())
                    )
                    if (response.isSuccessful) {
                        Log.d("MoviePlayer", "Progress updated: ${currentProgress.toInt()}%")
                    }
                } catch (e: Exception) {
                    Log.e("MoviePlayer", "Error updating progress: ${e.message}", e)
                }
            }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            player.removeListener(playerListener)
            player.release()
            mediaSession?.release()
        }
    }

    // UI - Simple PlayerView with default controls
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { keyEvent ->
                val eventType = when (keyEvent.type) {
                    KeyEventType.KeyDown -> "DOWN"
                    KeyEventType.KeyUp -> "UP"
                    else -> "UNKNOWN"
                }

                val keyCode = keyEvent.key.nativeKeyCode
                val keyName = KeyEvent.keyCodeToString(keyCode)

                Log.d("MoviePlayerKeys", """
                    ═══════════════════════════════════════
                    [COMPOSE LEVEL] Key Event: $eventType
                    Key Code: $keyCode
                    Key Name: $keyName
                    ═══════════════════════════════════════
                """.trimIndent())

                // Only handle KeyDown events to avoid double-triggering
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            // Rewind 10 seconds
                            val newPosition = (player.currentPosition - 10000).coerceAtLeast(0)
                            player.seekTo(newPosition)
                            Log.d("MoviePlayerKeys", "⏪ Rewound 10 seconds to ${newPosition / 1000}s")
                            true // Consume the event
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            // Fast forward 10 seconds
                            val newPosition = (player.currentPosition + 10000).coerceAtMost(player.duration)
                            player.seekTo(newPosition)
                            Log.d("MoviePlayerKeys", "⏩ Fast forwarded 10 seconds to ${newPosition / 1000}s")
                            true // Consume the event
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            // Handle dedicated play/pause button
                            if (player.isPlaying) {
                                player.pause()
                                Log.d("MoviePlayerKeys", "⏸️  Paused (media button)")
                            } else {
                                player.play()
                                Log.d("MoviePlayerKeys", "▶️  Playing (media button)")
                            }
                            true // Consume the event
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            // Show the media controls
                            playerView?.let { view ->
                                Log.d("MoviePlayerKeys", "📺 Opening media controls")
                                view.showController()
                                true // Consume the event
                            } ?: false
                        }
                        KeyEvent.KEYCODE_BACK -> {
                            // Handle back button - close controls if open, otherwise navigate back
                            if (areControlsVisible) {
                                Log.d("MoviePlayerKeys", "🔙 Back pressed - hiding controls")
                                playerView?.hideController()
                                true // Consume the event
                            } else {
                                Log.d("MoviePlayerKeys", "🔙 Back pressed - returning to MovieListActivity")
                                // Navigate back directly instead of propagating to BackHandler
                                val intent = android.content.Intent(context, MovieListActivity::class.java).apply {
                                    flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    putExtra("token", token)
                                    putExtra("restore_selection", movieTitle) // Pass the movie title to restore selection
                                }
                                context.startActivity(intent)
                                (context as? Activity)?.finish()
                                true // Consume the event
                            }
                        }
                        else -> {
                            // Don't consume other keys (UP, DOWN)
                            // Let them propagate to PlayerView
                            Log.d("MoviePlayerKeys", "Allowing key to propagate to PlayerView")
                            false
                        }
                    }
                } else {
                    // Don't consume KeyUp events
                    false
                }
            }
            .focusable()
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = true
                    this.player = player
                    playerView = this

                    // Add controller visibility listener for debugging and state tracking
                    setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                        val wasVisible = areControlsVisible
                        val isNowVisible = visibility == android.view.View.VISIBLE

                        // Only log when visibility actually changes to avoid duplicate logs
                        if (wasVisible != isNowVisible) {
                            areControlsVisible = isNowVisible
                            if (isNowVisible) {
                                Log.d("MoviePlayerKeys", "🎮 Media controls OPENED")
                            } else {
                                Log.d("MoviePlayerKeys", "🎮 Media controls CLOSED")
                            }
                        }
                    })

                    // Make PlayerView focusable for TV remote
                    isFocusable = true
                    isFocusableInTouchMode = true

                    // Request focus when created
                    post {
                        requestFocus()
                    }

                    // Add key event listener for debugging remote control
                    setOnKeyListener { view, keyCode, event ->
                        val action = when (event.action) {
                            KeyEvent.ACTION_DOWN -> "DOWN"
                            KeyEvent.ACTION_UP -> "UP"
                            else -> "UNKNOWN(${event.action})"
                        }

                        val keyName = KeyEvent.keyCodeToString(keyCode)

                        Log.d("MoviePlayerKeys", """
                            ═══════════════════════════════════════
                            [PLAYERVIEW LEVEL] Key Event: $action
                            Key Code: $keyCode
                            Key Name: $keyName
                            Repeat Count: ${event.repeatCount}
                            ═══════════════════════════════════════
                        """.trimIndent())

                        // Return false to allow default handling
                        false
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
