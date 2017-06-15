package com.flir.flironeexampleapplication;

/**
 * Created by cp2732 on 6/12/17.
 */

public class CreateList {

    private String image_title, image_location;
    private Integer image_id;

    public String getImageTitle() {
        return image_title;
    }
    public void setImageTitle(String android_version_name) { this.image_title = android_version_name; }

    public Integer getImageID() {
        return image_id;
    }
    public void setImageID(Integer android_image_url) {
        this.image_id = android_image_url;
    }

    public String getImageLocation() {
        return image_location;
    }
    public void setImageLocation(String android_image_loc) { this.image_location = android_image_loc; }
}