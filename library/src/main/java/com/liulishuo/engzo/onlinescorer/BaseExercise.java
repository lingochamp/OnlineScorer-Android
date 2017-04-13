package com.liulishuo.engzo.onlinescorer;

import org.json.JSONObject;

/**
 * Created by wcw on 3/29/17.
 */

public abstract class BaseExercise {

    private String type;
    private int quality;

    public String getType() {
        return type;
    }

    protected void setType(String type) {
        this.type = type;
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public abstract JSONObject toJson();
}
