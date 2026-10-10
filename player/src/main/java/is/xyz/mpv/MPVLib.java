package is.xyz.mpv;

import android.content.Context;
import android.view.Surface;

public class MPVLib {

    static {
        // 必须按依赖倒序加载完整库链，确保 Android 系统底层链接器能够解析所有符号
        String[] libs = {
            "c++_shared",
            "avutil",
            "swresample",
            "avcodec",
            "avformat",
            "swscale",
            "avfilter",
            "avdevice",
            "mpv",
            "player"
        };
        for (String lib : libs) {
            try {
                System.loadLibrary(lib);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
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
        public static final int MPV_EVENT_TRACKS_CHANGED = 9;
        public static final int MPV_EVENT_TRACK_SWITCHED = 10;
        public static final int MPV_EVENT_IDLE = 11;
        public static final int MPV_EVENT_PAUSE = 12;
        public static final int MPV_EVENT_UNPAUSE = 13;
        public static final int MPV_EVENT_TICK = 14;
        public static final int MPV_EVENT_VIDEO_RECONFIG = 17;
        public static final int MPV_EVENT_AUDIO_RECONFIG = 18;
        public static final int MPV_EVENT_SEEK = 20;
        public static final int MPV_EVENT_PLAYBACK_RESTART = 21;
        public static final int MPV_EVENT_PROPERTY_CHANGE = 22;
    }

    // 严禁删减此接口方法，底层 libplayer.so JNI 初始化严格依赖这些方法签名
    public interface EventObserver {
        void eventProperty(String property);
        void eventProperty(String property, long value);
        void eventProperty(String property, boolean value);
        void eventProperty(String property, String value);
        void eventProperty(String property, double value);
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
