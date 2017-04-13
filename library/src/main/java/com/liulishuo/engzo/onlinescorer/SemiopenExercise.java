package com.liulishuo.engzo.onlinescorer;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by wcw on 3/29/17.
 */

class SemiopenExercise extends BaseExercise {

    private List<String> refAnswers;
    private int questionType;
    private int targetAudience;

    private SemiopenExercise() {
        setType("semiopen");
    }

    public List<String> getRefAnswers() {
        return refAnswers;
    }

    public void setRefAnswers(List<String> refAnswers) {
        this.refAnswers = refAnswers;
    }

    public int getQuestionType() {
        return questionType;
    }

    public void setQuestionType(int questionType) {
        this.questionType = questionType;
    }

    public int getTargetAudience() {
        return targetAudience;
    }

    public void setTargetAudience(int targetAudience) {
        this.targetAudience = targetAudience;
    }

    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject();
            jsonObject.put("type", "semiopen");
            jsonObject.put("quality", getQuality());
            jsonObject.put("questionType", getQuestionType());
            jsonObject.put("targetAudience", getTargetAudience());
            jsonObject.put("refAnswers", joinRefAnswers(getRefAnswers()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private static String joinRefAnswers(List<String> refAnswers) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < refAnswers.size(); i++) {
            sb.append(refAnswers.get(0));
            if (i < refAnswers.size() - 1) {
                sb.append("|");
            }
        }
        return sb.toString();
    }
}
