package org.noobs2d.coneplayer;

import android.content.Context;
import android.util.Log;

import org.videolan.libvlc.LibVLC;

import java.util.ArrayList;

public class ConePlayerFactory {

    public static ConePlayer newConePlayer(Context context) {
        ArrayList<String> options = new ArrayList<>();
        options.add("--aout=opensles");
        options.add("--audio-time-stretch");
        LibVLC libVLC = new LibVLC(context, options);
        Log.v("VLC", "Version: " + libVLC.version() + " Compiler: " + libVLC.compiler());
        return new SimpleConePlayer(libVLC);
    }
}