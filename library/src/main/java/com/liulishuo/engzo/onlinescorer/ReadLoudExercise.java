package com.liulishuo.engzo.onlinescorer;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by wcw on 3/29/17.
 */

public class ReadLoudExercise extends BaseExercise {

    private String reftext;

    private int targetAudience;

    public ReadLoudExercise() {
        setType("readaloud");
    }

    public String getReftext() {
        return reftext;
    }

    public void setReftext(String reftext) {
        this.reftext = reftext;
    }

    public int getTargetAudience() {
        return targetAudience;
    }

    public void setTargetAudience(int targetAudience) {
        if (targetAudience < 0 || targetAudience > 1) {
            throw new IllegalArgumentException("illegal targetAudience only support 0(adult) or 1(child)");
        }
        this.targetAudience = targetAudience;
    }

    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject();
            jsonObject.put("type", getType());
            jsonObject.put("quality", getQuality());
            jsonObject.put("reftext", getReftext());
            jsonObject.put("targetAudience", getTargetAudience());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
