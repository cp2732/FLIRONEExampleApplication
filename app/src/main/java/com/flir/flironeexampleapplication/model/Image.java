package com.flir.flironeexampleapplication.model;

import android.graphics.Bitmap;
import java.io.Serializable;

/**
 * Created by Lincoln on 04/04/16.
 *
 */
public class Image implements Serializable {
    private String name, path;
    private String timestamp;
    private transient Bitmap bmp; //"transient" is needed because Bitmap is not serializable (will cause NotSerializableException)

    public Image() { }

    public Image(String name, String path, String timestamp) {
        this.name = name;
        this.path = path;
        this.timestamp = timestamp;
    }

    public Image(String name, String path, Bitmap bmp, String timestamp) {
        this.name = name;
        this.path = path;
        this.bmp = bmp;
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Bitmap getBitmap() {
        return bmp;
    }

    public void setBitmap(Bitmap bmp) {
        this.bmp = bmp;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
