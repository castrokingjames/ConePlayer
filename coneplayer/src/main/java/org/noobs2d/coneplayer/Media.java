package org.noobs2d.coneplayer;

import android.net.Uri;

public class Media {

    private Uri path;

    public Media(Uri path) {
        this.path = path;
    }

    public Uri getPath() {
        return path;
    }
}