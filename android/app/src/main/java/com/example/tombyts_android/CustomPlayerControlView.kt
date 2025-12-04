// Create a new file: CustomPlayerControlView.kt
package com.example.tombyts_android

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.ui.PlayerControlView

class CustomPlayerControlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var playPauseButton: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var positionText: TextView
    private lateinit var durationText: TextView
    private lateinit var settingsButton: ImageButton
    private lateinit var subtitleButton: ImageButton
    private lateinit var rewindButton: ImageButton
    private lateinit var fastForwardButton: ImageButton

    private var player: Player? = null

    init {
        setupCustomControls()
    }

    private fun setupCustomControls() {
        // Inflate custom layout
        LayoutInflater.from(context).inflate(R.layout.custom_player_controls, this, true)

        // Find views
        playPauseButton = findViewById(R.id.play_pause_button)
        seekBar = findViewById(R.id.seek_bar)
        positionText = findViewById(R.id.position_text)
        durationText = findViewById(R.id.duration_text)
        settingsButton = findViewById(R.id.settings_button)
        subtitleButton = findViewById(R.id.subtitle_button)
        rewindButton = findViewById(R.id.rewind_button)
        fastForwardButton = findViewById(R.id.fast_forward_button)

        // Set up focus indicators
        setupFocusIndicators()

        // Set up click listeners
        setupClickListeners()
    }

    private fun setupFocusIndicators() {
        val buttons = listOf(
            playPauseButton, settingsButton, subtitleButton,
            rewindButton, fastForwardButton
        )

        buttons.forEach { button ->
            button.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    // Highlighted state - bright border and background
                    view.background = ContextCompat.getDrawable(context, R.drawable.button_focused)
                    view.scaleX = 1.1f
                    view.scaleY = 1.1f
                } else {
                    // Normal state
                    view.background = ContextCompat.getDrawable(context, R.drawable.button_normal)
                    view.scaleX = 1.0f
                    view.scaleY = 1.0f
                }
            }
        }

        // Special handling for seek bar
        seekBar.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.background = ContextCompat.getDrawable(context, R.drawable.seekbar_focused)
                // Make it more prominent when focused
                view.scaleY = 1.3f
            } else {
                view.background = ContextCompat.getDrawable(context, R.drawable.seekbar_normal)
                view.scaleY = 1.0f
            }
        }
    }

    private fun setupClickListeners() {
        playPauseButton.setOnClickListener {
            player?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    playPauseButton.setImageResource(android.R.drawable.ic_media_play)
                } else {
                    player.play()
                    playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
                }
            }
        }

        rewindButton.setOnClickListener {
            player?.seekBack()
        }

        fastForwardButton.setOnClickListener {
            player?.seekForward()
        }

        // Add settings and subtitle button functionality as needed
        settingsButton.setOnClickListener {
            // TODO: Show settings menu
        }

        subtitleButton.setOnClickListener {
            // TODO: Show subtitle selection
        }
    }

    fun setPlayer(player: Player?) {
        this.player = player
        updatePlayPauseButton()
    }

    private fun updatePlayPauseButton() {
        player?.let { player ->
            if (player.isPlaying) {
                playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
            } else {
                playPauseButton.setImageResource(android.R.drawable.ic_media_play)
            }
        }
    }
}