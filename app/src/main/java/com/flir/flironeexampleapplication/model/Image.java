package com.flir.flironeexampleapplication.model;

import android.graphics.Bitmap;
import java.io.Serializable;

/**
 * Created by Lincoln on 04/04/16.
 *
 */
public class Image implements Serializable {
    private String name, path;
    private Bitmap small, medium, large;
    private String timestamp;

    public Image() { }

    public Image(String name, String path, String timestamp) {
        this.name = name;
        this.path = path;
        this.timestamp = timestamp;
    }

    public Image(String name, String path, Bitmap small, Bitmap medium, Bitmap large, String timestamp) {
        this.name = name;
        this.path = path;
        this.small = small;
        this.medium = medium;
        this.large = large;
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

    public Bitmap getSmall() {
        return small;
    }

    public void setSmall(Bitmap small) {
        this.small = small;
    }

    public Bitmap getMedium() { return medium; }

    public void setMedium(Bitmap medium) {
        this.medium = medium;
    }

    public Bitmap getLarge() {
        return large;
    }

    public void setLarge(Bitmap large) {
        this.large = large;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
