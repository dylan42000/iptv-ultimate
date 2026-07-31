package com.dylandos.iptv

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dylandos.iptv.ui.DylandosApp
import com.dylandos.iptv.ui.player.PlayerController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single-activity TV application. All navigation is Compose-based; the activity
 * only forwards key events to the active [PlayerController] so that D-pad center /
 * enter / numpad-enter are treated identically by the playback UI.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var playerController: PlayerController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DylandosApp(playerController = playerController)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Normalize the three "activate" keys so the UI never branches on keyCode.
        val normalized = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (event.action == KeyEvent.ACTION_UP) {
                    playerController.onActivateKeyUp()
                }
                KeyEvent(event.time, event.eventTime, event.action, KeyEvent.KEYCODE_DPAD_CENTER, 0)
            }
            else -> event
        }
        return super.dispatchKeyEvent(normalized)
    }
}
