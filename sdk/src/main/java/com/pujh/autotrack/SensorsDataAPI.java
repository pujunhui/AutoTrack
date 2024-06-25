package com.pujh.autotrack;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.util.Log;
import android.view.ViewTreeObserver;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.util.Map;

/**
 * Created by 王灼洲 on 2018/7/22
 */
@Keep
public class SensorsDataAPI {
    private final String TAG = this.getClass().getSimpleName();
    public static final String SDK_VERSION = "1.0.0";
    private static SensorsDataAPI INSTANCE;
    private static final Object mLock = new Object();
    private static Map<String, Object> mDeviceInfo;
    private final String mDeviceId;

    @Keep
    @SuppressWarnings("UnusedReturnValue")
    public static SensorsDataAPI init(Application application) {
        synchronized (mLock) {
            if (null == INSTANCE) {
                INSTANCE = new SensorsDataAPI(application);
            }
            return INSTANCE;
        }
    }

    @Keep
    public static SensorsDataAPI getInstance() {
        return INSTANCE;
    }

    private SensorsDataAPI(Application application) {
        mDeviceId = SensorsDataPrivate.getAndroidID(application.getApplicationContext());
        mDeviceInfo = SensorsDataPrivate.getDeviceInfo(application.getApplicationContext());
        SensorsDataPrivate.observerAppAndActivityState(application);
//        SensorsDataPrivate.delegateAllViewEventsListener(application);
//        SensorsDataPrivate.delegateWindowCallback(application);
        SensorsDataPrivate.delegateAllViewAccessibility(application);
    }

    /**
     * 指定不采集哪个 Activity 的页面浏览事件
     *
     * @param activity Activity
     */
    public void ignoreAutoTrackActivity(Class<?> activity) {
        SensorsDataPrivate.ignoreAutoTrackActivity(activity);
    }

    /**
     * 恢复采集某个 Activity 的页面浏览事件
     *
     * @param activity Activity
     */
    public void removeIgnoredActivity(Class<?> activity) {
        SensorsDataPrivate.removeIgnoredActivity(activity);
    }

    /**
     * Track Dialog 的点击
     *
     * @param activity Activity
     * @param dialog   Dialog
     */
    public void trackDialog(@NonNull final Activity activity, @NonNull final Dialog dialog) {
        if (dialog.getWindow() != null) {
            dialog.getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    SensorsDataPrivate.delegateViewsOnClickListener(activity, dialog.getWindow().getDecorView());
                }
            });
        }
    }

    /**
     * track 事件
     *
     * @param eventName  String 事件名称
     * @param properties JSONObject 事件自定义属性
     */
    public void track(@NonNull String eventName, @Nullable JSONObject properties) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("event", eventName);
            jsonObject.put("device_id", mDeviceId);

            JSONObject sendProperties = new JSONObject(mDeviceInfo);

            if (properties != null) {
                SensorsDataPrivate.mergeJSONObject(properties, sendProperties);
            }

            jsonObject.put("properties", sendProperties);
            jsonObject.put("time", System.currentTimeMillis());

            Log.i(TAG, SensorsDataPrivate.formatJson(jsonObject.toString()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
