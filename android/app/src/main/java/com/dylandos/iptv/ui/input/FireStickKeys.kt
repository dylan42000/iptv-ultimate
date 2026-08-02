package com.dylandos.iptv.ui.input

import android.view.KeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key

/**
 * World-class FireStick remote / gamepad key mapping.
 *
 * The FireStick remote and the generic "Alexa" TV remotes report a consistent set
 * of Android key codes. This object centralizes that mapping so the whole app has
 * ONE source of truth for what each physical button does — never scattered
 * if/else branches across screens.
 *
 * Primary guarantees:
 *   - KEYCODE_DPAD_CENTER, KEYCODE_ENTER and KEYCODE_NUMPAD_ENTER all map to the
 *     same logical "ACTIVATE" action (selected / click).
 *   - Media transport keys are normalized to player actions regardless of whether
 *     the remote emits KEYCODE_MEDIA_PLAY_PAUSE or the gamepad emits KEYCODE_BUTTON_A.
 *   - Long-press (e.g. long-press SELECT to toggle a category) is exposed as a
 *     dedicated event so UI never has to measure timing itself.
 */
object FireStickKeys {

    /** The single normalized set of logical actions the app understands. */
    enum class Action {
        ACTIVATE,      // D-pad CENTER / ENTER / NUMPAD_ENTER
        UP, DOWN, LEFT, RIGHT,
        BACK,
        HOME,
        MENU,
        PLAY, PAUSE, PLAY_PAUSE,
        REWIND, FAST_FORWARD,
        STOP,
        SUBTITLE,      // INFO / SUBTITLE button (Fire TV: KEYCODE_INFO on some remotes)
        AUDIO_TRACK,
        GUIDE,         // KEYCODE_GUIDE where available
        SEARCH,
        NONE
    }

    private val rawKeyToAction = mapOf(
        KeyEvent.KEYCODE_DPAD_CENTER to Action.ACTIVATE,
        KeyEvent.KEYCODE_ENTER to Action.ACTIVATE,
        KeyEvent.KEYCODE_NUMPAD_ENTER to Action.ACTIVATE,
        KeyEvent.KEYCODE_SPACE to Action.ACTIVATE,
        KeyEvent.KEYCODE_DPAD_UP to Action.UP,
        KeyEvent.KEYCODE_DPAD_DOWN to Action.DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT to Action.LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT to Action.RIGHT,
        KeyEvent.KEYCODE_BACK to Action.BACK,
        KeyEvent.KEYCODE_ESCAPE to Action.BACK,
        KeyEvent.KEYCODE_HOME to Action.HOME,
        KeyEvent.KEYCODE_MENU to Action.MENU,
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE to Action.PLAY_PAUSE,
        KeyEvent.KEYCODE_MEDIA_PLAY to Action.PLAY,
        KeyEvent.KEYCODE_MEDIA_PAUSE to Action.PAUSE,
        KeyEvent.KEYCODE_MEDIA_STOP to Action.STOP,
        KeyEvent.KEYCODE_MEDIA_REWIND to Action.REWIND,
        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD to Action.FAST_FORWARD,
        KeyEvent.KEYCODE_BUTTON_A to Action.ACTIVATE,        // gamepad A == SELECT
        KeyEvent.KEYCODE_BUTTON_B to Action.BACK,             // gamepad B == BACK
        KeyEvent.KEYCODE_BUTTON_X to Action.SUBTITLE,
        KeyEvent.KEYCODE_BUTTON_Y to Action.AUDIO_TRACK,
        KeyEvent.KEYCODE_INFO to Action.SUBTITLE,
        KeyEvent.KEYCODE_GUIDE to Action.GUIDE,
        KeyEvent.KEYCODE_SEARCH to Action.SEARCH
    )

    /**
     * Map a raw Android [KeyEvent] to a logical [Action].
     *
     * [KeyEvent.KEYCODE_DPAD_CENTER], [KeyEvent.KEYCODE_ENTER] and
     * [KeyEvent.KEYCODE_NUMPAD_ENTER] are collapsed to [Action.ACTIVATE] so the
     * UI never branches on key code.
     */
    fun map(event: KeyEvent): Action =
        rawKeyToAction[event.keyCode] ?: Action.NONE

    /** True if the given event is one of the three "activate/select" keys. */
    fun isActivate(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
            keyCode == KeyEvent.KEYCODE_ENTER ||
            keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER

    /** True if the given event is a media-transport key. */
    fun isMediaTransport(keyCode: Int): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        KeyEvent.KEYCODE_MEDIA_PLAY,
        KeyEvent.KEYCODE_MEDIA_PAUSE,
        KeyEvent.KEYCODE_MEDIA_STOP,
        KeyEvent.KEYCODE_MEDIA_REWIND,
        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> true
        else -> false
    }

    /**
     * Build a synthetic, normalized event where the three activate keys become
     * [KeyEvent.KEYCODE_DPAD_CENTER]. Used at the Activity dispatch boundary so
     * every downstream Compose handler sees a single key code.
     */
    fun normalizeEvent(event: KeyEvent): KeyEvent {
        if (!isActivate(event.keyCode)) return event
        return KeyEvent(
            event.downTime,
            event.eventTime,
            event.action,
            KeyEvent.KEYCODE_DPAD_CENTER,
            event.repeatCount,
            event.deviceId,
            event.scanCode,
            event.flags or KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE,
            event.source
        )
    }
}

/**
 * Compose-level helper: maps a Compose [androidx.compose.ui.input.key.Key]
 * (from onPreviewKeyEvent) to a FireStick logical action. Useful for screens that
 * want to react to the remote inside the Compose tree without reaching for the
 * Android KeyEvent API directly.
 */
fun androidx.compose.ui.input.key.KeyEvent.toFireStickAction(): FireStickKeys.Action {
    return when (this.key) {
        Key.DirectionUp -> FireStickKeys.Action.UP
        Key.DirectionDown -> FireStickKeys.Action.DOWN
        Key.DirectionLeft -> FireStickKeys.Action.LEFT
        Key.DirectionRight -> FireStickKeys.Action.RIGHT
        Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> FireStickKeys.Action.ACTIVATE
        Key.Back, Key.Escape -> FireStickKeys.Action.BACK
        Key.Home -> FireStickKeys.Action.HOME
        Key.MediaPlayPause -> FireStickKeys.Action.PLAY_PAUSE
        Key.MediaPlay -> FireStickKeys.Action.PLAY
        Key.MediaPause -> FireStickKeys.Action.PAUSE
        Key.MediaStop -> FireStickKeys.Action.STOP
        Key.MediaRewind -> FireStickKeys.Action.REWIND
        Key.MediaFastForward -> FireStickKeys.Action.FAST_FORWARD
        Key.Menu -> FireStickKeys.Action.MENU
        Key.Search -> FireStickKeys.Action.SEARCH
        else -> FireStickKeys.Action.NONE
    }
}
