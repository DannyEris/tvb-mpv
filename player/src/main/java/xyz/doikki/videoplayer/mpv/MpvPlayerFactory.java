package xyz.doikki.videoplayer.mpv;

import android.content.Context;
import xyz.doikki.videoplayer.player.PlayerFactory;

public class MpvPlayerFactory extends PlayerFactory<MpvPlayer> {

    private final int mHdrMode;

    public static MpvPlayerFactory create() {
        return new MpvPlayerFactory(0);
    }

    public static MpvPlayerFactory create(int hdrMode) {
        return new MpvPlayerFactory(hdrMode);
    }

    public MpvPlayerFactory(int hdrMode) {
        this.mHdrMode = hdrMode;
    }

    @Override
    public MpvPlayer createPlayer(Context context) {
        MpvPlayer player = new MpvPlayer(context);
        player.setHdrToSdrMode(mHdrMode);
        return player;
    }
}
