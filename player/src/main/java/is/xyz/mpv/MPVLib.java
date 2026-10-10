package is.xyz.mpv;

import android.content.Context;
import android.view.Surface;

public class MPVLib {

    static {
        System.loadLibrary("mpv");
    }

    public static class mpvEventId {
        public static final int MPV_EVENT_NONE = 0;
        public static final int MPV_EVENT_SHUTDOWN = 1;
        public static final int MPV_EVENT_LOG_MESSAGE = 2;
        public static final int MPV_EVENT_GET_PROPERTY_REPLY = 3;
        public static final int MPV_EVENT_SET_PROPERTY_REPLY = 4;
        public static final int MPV_EVENT_COMMAND_REPLY = 5;
        public static final int MPV_EVENT_START_FILE = 6;
        public static final int MPV_EVENT_END_FILE = 7;
        public static final int MPV_EVENT_FILE_LOADED = 8;
        public static final int MPV_EVENT_PLAYBACK_RESTART = 22;
    }

    public interface EventObserver {
        void event(int eventId);
    }

    public static native void create(Context context);
    public static native void init();
    public static native void destroy();
    public static native void attachSurface(Surface surface);
    public static native void detachSurface();
    public static native void command(String[] cmd);
    public static native int setOptionString(String name, String value);
    public static native int setPropertyString(String name, String value);
    public static native int setPropertyBoolean(String name, boolean value);
    public static native int setPropertyDouble(String name, double value);
    public static native String getPropertyString(String name);
    public static native Boolean getPropertyBoolean(String name);
    public static native Double getPropertyDouble(String name);
    public static native void addObserver(EventObserver observer);
    public static native void removeObserver(EventObserver observer);
}
