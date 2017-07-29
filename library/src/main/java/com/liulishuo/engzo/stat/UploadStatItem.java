package com.liulishuo.engzo.stat;

import com.liulishuo.engzo.common.LogCollector;
import com.liulishuo.engzo.onlinescorer.Config;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by rantianhua on 17/7/28.
 * the uploaded stat item should include some state info
 */

public class UploadStatItem {
    public String deviceId;
    public String appId;
    public String os;
    public String version;
    public List<StatItem> stats;

    public UploadStatItem() {
        deviceId = Config.get().deviceId;
        appId = Config.get().appId;
        os = Config.get().os;
        version = Config.get().version;
    }

    public String toJsonString() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("deviceId", deviceId);
            jsonObject.put("appId", appId);
            jsonObject.put("os", os);
            jsonObject.put("version", version);
            JSONArray statArr = new JSONArray();
            for (StatItem stat : stats) {
                statArr.put(stat.toJson());
            }
            jsonObject.put("stats", statArr);
        } catch (JSONException e) {
            LogCollector.get().e("UploadStatItem error in toJsonString: " + e.getMessage(),
                    e);
        }
        return jsonObject.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UploadStatItem that = (UploadStatItem) o;

        if (!deviceId.equals(that.deviceId)) return false;
        if (!appId.equals(that.appId)) return false;
        if (!os.equals(that.os)) return false;
        if (!version.equals(that.version)) return false;
        return stats.equals(that.stats);

    }

    @Override
    public int hashCode() {
        int result = deviceId.hashCode();
        result = 31 * result + appId.hashCode();
        result = 31 * result + os.hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + stats.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "UploadStatItem{" +
                "deviceId='" + deviceId + '\'' +
                ", appId='" + appId + '\'' +
                ", os='" + os + '\'' +
                ", version='" + version + '\'' +
                ", stats=" + stats +
                '}';
    }
}
