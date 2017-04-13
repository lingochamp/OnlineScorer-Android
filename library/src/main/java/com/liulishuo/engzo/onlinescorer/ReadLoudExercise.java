package com.liulishuo.engzo.onlinescorer;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by wcw on 3/29/17.
 */

public class ReadLoudExercise extends BaseExercise {

    private String reftext;

    public ReadLoudExercise() {
        setType("readaloud");
    }

    public String getReftext() {
        return reftext;
    }

    public void setReftext(String reftext) {
        this.reftext = reftext;
    }

    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject();
            jsonObject.put("type", getType());
            jsonObject.put("quality", getQuality());
            jsonObject.put("reftext", getReftext());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
