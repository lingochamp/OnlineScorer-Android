package com.liulishuo.engzo.stat;


import org.json.JSONObject;

/**
 * Created by rantianhua on 17/7/27.
 * this is the parent class of all stat item.
 *
 * <P>the data I need to collect is varied. This class is used to
 * abstract them. The "type" field can distinguish different data, other fields depend on
 * specific subclass.
 *
 * In order to let this class become easy-to-use, some abstract methods are defined.
 * Pay attention to the "onStatCome" method, because every subclass needs to collect state only
 * it concerned, I must use many subclasses in one class, such as
 * {@link com.liulishuo.engzo.onlinescorer.OnlineScorerRecorder}.
 * without this method, I need to use "instanceof" everywhere, which is terrible. But with
 *
 * @see #onStatCome(String, String)
 * , I do not need to know specific subclass. Maybe you worry about there is not only String value
 * needs to be collected, but int or long ... need to be collected. Just take it easy, they all can
 * be
 * covert to string.
 */

public abstract class StatItem {

    /**
     * indicate different stat item
     */
    public String type;

    public abstract JSONObject toJson();

    public abstract StatItem fromJson(JSONObject item);

    public abstract void onStatCome(String name, String value);

}
