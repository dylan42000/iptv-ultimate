package is.xyz.mpv;

import android.content.Context;
import android.view.Surface;

/**
 * DYLANDOS IPTV ULTIMATE — MPVLib STUB
 * This is a compile-time stub that allows the project to build when the external
 * mpv-android Maven artifact is unreachable (DNS issues, dead repos, jitpack 401).
 *
 * When the real libmpv AAR is present, this stub is shadowed by the real one
 * (real AAR wins if dependency is included). When dependency is disabled,
 * this no-op implementation lets the app build and run with LibVLC only.
 *
 * LibVLC handles Live TV. VOD/Series that would normally use MPV will gracefully
 * fallback or show paused state. Replace this stub with real maven artifact
 * io.github.abdallahmehiz:mpv-android-lib:0.1.12 (Maven Central, same package)
 * for full HDR/VOD hardware decoding.
 */
public class MPVLib {

    public static final int MPV_FORMAT_NONE = 0;
    public static final int MPV_FORMAT_STRING = 1;
    public static final int MPV_FORMAT_OSD_STRING = 2;
    public static final int MPV_FORMAT_FLAG = 3;
    public static final int MPV_FORMAT_INT64 = 4;
    public static final int MPV_FORMAT_DOUBLE = 5;
    public static final int MPV_FORMAT_NODE = 6;
    public static final int MPV_FORMAT_NODE_ARRAY = 7;
    public static final int MPV_FORMAT_NODE_MAP = 8;
    public static final int MPV_FORMAT_BYTE_ARRAY = 9;

    public interface OnPropertyChangeListener {
        void onPropertyChange(String property, int format, Object value);
    }

    private static OnPropertyChangeListener listener;

    public static void create(Context context) {
        // no-op stub
    }

    public static void init(Surface surface) {
        // no-op
    }

    public static void destroy() {}

    public static void observeProperty(String property, int format) {}

    public static void setOptionString(String option, String value) {}

    @SuppressWarnings("unused")
    public static void command(String... args) {}

    public static long getPropertyLong(String property, long def) { return def; }
    public static double getPropertyDouble(String property, double def) { return def; }
    public static String getPropertyString(String property, String def) { return def; }
    public static boolean getPropertyBoolean(String property, boolean def) { return def; }

    public static void setOnPropertyChangeListener(OnPropertyChangeListener l) {
        listener = l;
    }

    // Optional helpers used by some forks
    public static void setPropertyString(String property, String value) {}
    public static void setPropertyBoolean(String property, boolean value) {}
    public static void setPropertyLong(String property, long value) {}
    public static void setPropertyDouble(String property, double value) {}
}
