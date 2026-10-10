package xyz.doikki.videoplayer.mpv;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.os.Build;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.util.Map;

import is.xyz.mpv.MPVLib;
import xyz.doikki.videoplayer.player.AbstractPlayer;

public class MpvPlayer extends AbstractPlayer implements MPVLib.EventObserver {

    protected final Context mAppContext;
    private boolean mIsPreparing = false;
    private int mHdrMode = 0; // 0: 自动, 1: 开启, 2: 关闭

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
        MPVLib.setOptionString("hdr-compute-peak", "no"); // 节约电视 GPU 算力
        MPVLib.init();
        MPVLib.addObserver(this);
    }

    public void setHdrToSdrMode(int mode) {
        this.mHdrMode = mode;
        applyHdrToSdr();
    }

    private void applyHdrToSdr() {
        boolean enableToneMapping = false;
        if (mHdrMode == 1) {
            enableToneMapping = true; // 强制开启
        } else if (mHdrMode == 2) {
            enableToneMapping = false; // 强制关闭
        } else {
            enableToneMapping = !isDisplayHdrSupported(); // 自动判断
        }

        if (enableToneMapping) {
            // 启用 HDR to SDR 色调映射
            MPVLib.setPropertyString("target-trc", "bt.709");
            MPVLib.setPropertyString("target-prim", "bt.709");
            MPVLib.setPropertyString("tone-mapping", "mobius");
        } else {
            // 直通模式
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
        applyHdrToSdr();
        MPVLib.command(new String[]{"loadfile", path, "replace"});
    }

    @Override
    public void setDataSource(AssetFileDescriptor fd) {}

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
    }

    @Override
    public void prepareAsync() {
        mIsPreparing = true;
    }

    @Override
    public void reset() {
        stop();
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
        if (surface != null) {
            MPVLib.attachSurface(surface);
        } else {
            MPVLib.detachSurface();
        }
    }

    @Override
    public void setDisplay(SurfaceHolder holder) {
        if (holder != null) setSurface(holder.getSurface());
    }

    @Override
    public void setVolume(float v1, float v2) {
        MPVLib.setPropertyDouble("volume", (v1 + v2) * 50);
    }

    @Override
    public void setLooping(boolean isLooping) {
        MPVLib.setPropertyString("loop", isLooping ? "inf" : "no");
    }

    @Override
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
        if (eventId == MPVLib.mpvEventId.MPV_EVENT_PLAYBACK_RESTART && mIsPreparing) {
            mIsPreparing = false;
            setPlayerState(STATE_PREPARED);
            setPlayerState(STATE_PLAYING);
        } else if (eventId == MPVLib.mpvEventId.MPV_EVENT_END_FILE) {
            setPlayerState(STATE_PLAYBACK_COMPLETED);
        }
    }
}
