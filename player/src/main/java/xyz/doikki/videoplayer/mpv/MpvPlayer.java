package xyz.doikki.videoplayer.mpv;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.util.Map;

import is.xyz.mpv.MPVLib;
import xyz.doikki.videoplayer.player.AbstractPlayer;

public class MpvPlayer extends AbstractPlayer implements MPVLib.EventObserver {

    protected final Context mAppContext;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private boolean mIsPreparing = false;
    private boolean mFileLoaded = false;
    private int mHdrMode = 0; // 0: 自动, 1: 开启, 2: 关闭

    private Surface mSurface;
    private String mDataSourceUrl;
    private Map<String, String> mHeaders;

    public MpvPlayer(Context context) {
        mAppContext = context.getApplicationContext();
        initPlayer();
    }

    @Override
    public void initPlayer() {
        MPVLib.create(mAppContext);
        MPVLib.setOptionString("vo", "gpu");
        MPVLib.setOptionString("gpu-context", "android");
        MPVLib.setOptionString("hwdec", "mediacodec,auto");
        MPVLib.setOptionString("hdr-compute-peak", "no");
        MPVLib.setOptionString("force-window", "yes");
        MPVLib.setOptionString("pause", "yes");
        MPVLib.setOptionString("tls-verify", "no");
        MPVLib.setOptionString("ytdl", "no");
        MPVLib.setOptionString("demuxer-max-bytes", "32MiB");
        MPVLib.setOptionString("demuxer-max-back-bytes", "16MiB");
        MPVLib.init();
        MPVLib.addObserver(this);
    }

    @Override
    public void setOptions() {
    }

    public void setHdrToSdrMode(int mode) {
        this.mHdrMode = mode;
        applyHdrToSdr();
    }

    private void applyHdrToSdr() {
        boolean enableToneMapping = false;
        if (mHdrMode == 1) {
            enableToneMapping = true;
        } else if (mHdrMode == 2) {
            enableToneMapping = false;
        } else {
            enableToneMapping = !isDisplayHdrSupported();
        }

        if (enableToneMapping) {
            MPVLib.setPropertyString("target-trc", "bt.709");
            MPVLib.setPropertyString("target-prim", "bt.709");
            MPVLib.setPropertyString("tone-mapping", "mobius");
        } else {
            MPVLib.setPropertyString("target-trc", "auto");
            MPVLib.setPropertyString("target-prim", "auto");
            MPVLib.setPropertyString("tone-mapping", "clip");
        }
    }

    private boolean isDisplayHdrSupported() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            WindowManager wm = (WindowManager) mAppContext.getSystemService(Context.WINDOW_SERVICE);
            if (wm != null && wm.getDefaultDisplay() != null) {
                Display.HdrCapabilities caps = wm.getDefaultDisplay().getHdrCapabilities();
                if (caps != null && caps.getSupportedHdrTypes() != null) {
                    return caps.getSupportedHdrTypes().length > 0;
                }
            }
        }
        return false;
    }

    @Override
    public void setDataSource(String path, Map<String, String> headers) {
        this.mDataSourceUrl = path;
        this.mHeaders = headers;
        this.mFileLoaded = false;
    }

    @Override
    public void setDataSource(AssetFileDescriptor fd) {}

    @Override
    public void prepareAsync() {
        mIsPreparing = true;
        // 如果 Surface 已经就绪，立刻触发加载；若还没就绪，将在 setSurface 回调时安全触发
        if (mSurface != null && mSurface.isValid() && mDataSourceUrl != null) {
            startLoadFileInternal();
        }
    }

    private void startLoadFileInternal() {
        if (mDataSourceUrl == null) return;

        if (mHeaders != null && !mHeaders.isEmpty()) {
            StringBuilder headerSb = new StringBuilder();
            for (Map.Entry<String, String> entry : mHeaders.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                if (key != null && val != null) {
                    if (key.equalsIgnoreCase("user-agent")) {
                        MPVLib.setPropertyString("user-agent", val);
                    } else if (key.equalsIgnoreCase("referer")) {
                        MPVLib.setPropertyString("referrer", val);
                    } else {
                        if (headerSb.length() > 0) headerSb.append(",");
                        headerSb.append(key).append(": ").append(val);
                    }
                }
            }
            if (headerSb.length() > 0) {
                MPVLib.setPropertyString("http-header-fields", headerSb.toString());
            }
        }
        applyHdrToSdr();
        MPVLib.command(new String[]{"loadfile", mDataSourceUrl, "replace"});
    }

    @Override
    public void start() {
        MPVLib.setPropertyBoolean("pause", false);
    }

    @Override
    public void pause() {
        MPVLib.setPropertyBoolean("pause", true);
    }

    @Override
    public void stop() {
        MPVLib.command(new String[]{"stop"});
        mDataSourceUrl = null;
        mIsPreparing = false;
        mFileLoaded = false;
    }

    @Override
    public void reset() {
        stop();
        setSurface(null);
    }

    @Override
    public boolean isPlaying() {
        return !Boolean.TRUE.equals(MPVLib.getPropertyBoolean("pause"));
    }

    @Override
    public void seekTo(long time) {
        MPVLib.command(new String[]{"seek", String.valueOf((double) time / 1000.0), "absolute"});
    }

    @Override
    public void release() {
        reset();
        MPVLib.removeObserver(this);
        MPVLib.destroy();
    }

    @Override
    public long getCurrentPosition() {
        Double pos = MPVLib.getPropertyDouble("time-pos");
        return pos == null ? 0 : (long) (pos * 1000);
    }

    @Override
    public long getDuration() {
        Double duration = MPVLib.getPropertyDouble("duration");
        return duration == null ? 0 : (long) (duration * 1000);
    }

    @Override
    public int getBufferedPercentage() {
        return 0;
    }

    @Override
    public void setSurface(Surface surface) {
        this.mSurface = surface;
        if (surface != null && surface.isValid()) {
            MPVLib.attachSurface(surface);
            // 核心安全时序：Surface 就绪后，若处于准备中且尚未开始加载，立即触发启动
            if (mIsPreparing && mDataSourceUrl != null && !mFileLoaded) {
                startLoadFileInternal();
            }
        } else {
            MPVLib.detachSurface();
        }
    }

    @Override
    public void setDisplay(SurfaceHolder holder) {
        if (holder != null) {
            setSurface(holder.getSurface());
        } else {
            setSurface(null);
        }
    }

    @Override
    public void setVolume(float v1, float v2) {
        MPVLib.setPropertyDouble("volume", (v1 + v2) * 50);
    }

    @Override
    public void setLooping(boolean isLooping) {
        MPVLib.setPropertyString("loop", isLooping ? "inf" : "no");
    }

    public boolean isLooping() {
        return "inf".equals(MPVLib.getPropertyString("loop"));
    }

    @Override
    public void setSpeed(float speed) {
        MPVLib.setPropertyDouble("speed", speed);
    }

    @Override
    public float getSpeed() {
        Double speed = MPVLib.getPropertyDouble("speed");
        return speed == null ? 1.0f : speed.floatValue();
    }

    @Override
    public long getTcpSpeed() {
        return 0;
    }

    @Override
    public void event(int eventId) {
        if (eventId == MPVLib.mpvEventId.MPV_EVENT_FILE_LOADED || eventId == MPVLib.mpvEventId.MPV_EVENT_PLAYBACK_RESTART) {
            if (mIsPreparing) {
                mIsPreparing = false;
                mFileLoaded = true;
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mPlayerEventListener != null) {
                            Double w = MPVLib.getPropertyDouble("dwidth");
                            Double h = MPVLib.getPropertyDouble("dheight");
                            if (w != null && h != null && w > 0 && h > 0) {
                                mPlayerEventListener.onVideoSizeChanged(w.intValue(), h.intValue());
                            }
                            mPlayerEventListener.onPrepared();
                        }
                    }
                });
            }
        } else if (eventId == MPVLib.mpvEventId.MPV_EVENT_END_FILE) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mPlayerEventListener != null) {
                        // 只有在真正加载过文件且自然播放完毕时才通知完成，杜绝初始替换时的误触发
                        if (mFileLoaded) {
                            mPlayerEventListener.onCompletion();
                        } else if (mIsPreparing && mSurface != null) {
                            mPlayerEventListener.onError();
                        }
                    }
                }
            });
        }
    }
}
