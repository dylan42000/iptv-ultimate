package com.dylandos.iptv

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dylandos.iptv.ui.DylandosApp
import com.dylandos.iptv.ui.input.FireStickKeys
import com.dylandos.iptv.ui.player.PlayerController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single-activity TV application. All navigation is Compose-based; the activity
 * is the single dispatch boundary where FireStick remote keys are normalized.
 *
 * Every "activate" key (DPAD_CENTER / ENTER / NUMPAD_ENTER) is rewritten to
 * KEYCODE_DPAD_CENTER so the rest of the app sees one key code. Media-transport
 * keys are also forwarded to the active [PlayerController] so transport buttons on
 * the remote / gamepad always control playback no matter which screen is up.
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
        // 1) Normalize the three "activate" keys into one before Compose sees it.
        val normalized = FireStickKeys.normalizeEvent(event)

        // 2) Forward media-transport keys to the active player on ACTION_DOWN so
        //    transport always works, then fall through to Compose for the rest.
        if (event.action == KeyEvent.ACTION_DOWN && FireStickKeys.isMediaTransport(event.keyCode)) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> playerController.togglePlayPause()
                KeyEvent.KEYCODE_MEDIA_PLAY -> playerController.play()
                KeyEvent.KEYCODE_MEDIA_PAUSE -> playerController.pause()
                KeyEvent.KEYCODE_MEDIA_REWIND -> playerController.rewind(15)
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> playerController.fastForward(15)
            }
        }

        // 3) Handle the activate-key-up (used for OSD activation).
        if (event.action == KeyEvent.ACTION_UP && FireStickKeys.isActivate(event.keyCode)) {
            playerController.onActivateKeyUp()
        }

        return super.dispatchKeyEvent(normalized)
    }
}
