package com.pujh.autotrack;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.ActionMenuItemView;

import com.google.android.material.checkbox.MaterialCheckBox;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/*public*/ class SensorsDataPrivate {
    private static final List<String> mIgnoredActivities = new ArrayList<>();
    private static final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"
            + ".SSS", Locale.CHINA);
    private static SensorsDatabaseHelper mDatabaseHelper;
    private static CountDownTimer countDownTimer;
    private static WeakReference<Activity> mCurrentActivity;
    private final static int SESSION_INTERVAL_TIME = 30 * 1000;

    public static void ignoreAutoTrackActivity(Class<?> activity) {
        if (activity == null) {
            return;
        }

        mIgnoredActivities.add(activity.getCanonicalName());
    }

    public static void removeIgnoredActivity(Class<?> activity) {
        if (activity == null) {
            return;
        }

        mIgnoredActivities.remove(activity.getCanonicalName());
    }

    public static void mergeJSONObject(final JSONObject source, JSONObject dest)
            throws JSONException {
        Iterator<String> superPropertiesIterator = source.keys();
        while (superPropertiesIterator.hasNext()) {
            String key = superPropertiesIterator.next();
            Object value = source.get(key);
            if (value instanceof Date) {
                synchronized (mDateFormat) {
                    dest.put(key, mDateFormat.format((Date) value));
                }
            } else {
                dest.put(key, value);
            }
        }
    }

    @TargetApi(11)
    private static String getToolbarTitle(Activity activity) {
        try {
            ActionBar actionBar = activity.getActionBar();
            if (actionBar != null) {
                if (!TextUtils.isEmpty(actionBar.getTitle())) {
                    return actionBar.getTitle().toString();
                }
            } else {
                if (activity instanceof AppCompatActivity) {
                    AppCompatActivity appCompatActivity = (AppCompatActivity) activity;
                    androidx.appcompat.app.ActionBar supportActionBar = appCompatActivity.getSupportActionBar();
                    if (supportActionBar != null) {
                        if (!TextUtils.isEmpty(supportActionBar.getTitle())) {
                            return supportActionBar.getTitle().toString();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取 Activity 的 title
     *
     * @param activity Activity
     * @return String 当前页面 title
     */
    @SuppressWarnings("all")
    private static String getActivityTitle(Activity activity) {
        String activityTitle = null;

        if (activity == null) {
            return null;
        }

        try {
            activityTitle = activity.getTitle().toString();

            if (Build.VERSION.SDK_INT >= 11) {
                String toolbarTitle = getToolbarTitle(activity);
                if (!TextUtils.isEmpty(toolbarTitle)) {
                    activityTitle = toolbarTitle;
                }
            }

            if (TextUtils.isEmpty(activityTitle)) {
                PackageManager packageManager = activity.getPackageManager();
                if (packageManager != null) {
                    ActivityInfo activityInfo = packageManager.getActivityInfo(activity.getComponentName(), 0);
                    if (activityInfo != null) {
                        activityTitle = activityInfo.loadLabel(packageManager).toString();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return activityTitle;
    }

    /**
     * Track 页面浏览事件
     *
     * @param activity Activity
     */
    @Keep
    private static void trackAppViewScreen(Activity activity) {
        try {
            if (activity == null) {
                return;
            }
            if (mIgnoredActivities.contains(activity.getClass().getCanonicalName())) {
                return;
            }
            JSONObject properties = new JSONObject();
            properties.put("$activity", activity.getClass().getCanonicalName());
            properties.put("$title", getActivityTitle(activity));
            SensorsDataAPI.getInstance().track("$AppViewScreen", properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Track $AppStart 事件
     */
    private static void trackAppStart(Activity activity) {
        try {
            if (activity == null) {
                return;
            }
            JSONObject properties = new JSONObject();
            properties.put("$activity", activity.getClass().getCanonicalName());
            properties.put("$title", getActivityTitle(activity));
            SensorsDataAPI.getInstance().track("$AppStart", properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Track $AppEnd 事件
     */
    private static void trackAppEnd(Activity activity) {
        try {
            if (activity == null) {
                return;
            }
            JSONObject properties = new JSONObject();
            properties.put("$activity", activity.getClass().getCanonicalName());
            properties.put("$title", getActivityTitle(activity));
            SensorsDataAPI.getInstance().track("$AppEnd", properties);
            mDatabaseHelper.commitAppEndEventState(true);
            mCurrentActivity = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 注册 AppStart, AppEnd, AppViewScreen 的监听
     *
     * @param application Application
     */
    @TargetApi(14)
    public static void observerAppAndActivityState(Application application) {
        mDatabaseHelper = new SensorsDatabaseHelper(application.getApplicationContext(), application.getPackageName());
        countDownTimer = new CountDownTimer(SESSION_INTERVAL_TIME, 10 * 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                if (mCurrentActivity != null) {
                    trackAppEnd(mCurrentActivity.get());
                }
            }
        };

        application.registerActivityLifecycleCallbacks(new EmptyActivityLifecycleCallbacks() {

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                mDatabaseHelper.commitAppStart(true);
                double timeDiff = System.currentTimeMillis() - mDatabaseHelper.getAppPausedTime();
                if (timeDiff > SESSION_INTERVAL_TIME) {
                    if (!mDatabaseHelper.getAppEndEventState()) {
                        trackAppEnd(activity);
                    }
                }

                if (mDatabaseHelper.getAppEndEventState()) {
                    mDatabaseHelper.commitAppEndEventState(false);
                    trackAppStart(activity);
                }
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                trackAppViewScreen(activity);
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                mCurrentActivity = new WeakReference<>(activity);
                countDownTimer.start();
                mDatabaseHelper.commitAppPausedTime(System.currentTimeMillis());
            }
        });

        application.getContentResolver().registerContentObserver(mDatabaseHelper.getAppStartUri(),
                false, new ContentObserver(new Handler()) {
                    @Override
                    public void onChange(boolean selfChange, Uri uri) {
                        if (mDatabaseHelper.getAppStartUri().equals(uri)) {
                            countDownTimer.cancel();
                        }
                    }
                });
    }

    /**
     * 方案一：代理所有View的事件监听器
     *
     * @param application Application
     */
    @TargetApi(14)
    public static void delegateAllViewEventsListener(Application application) {
        application.registerActivityLifecycleCallbacks(new EmptyActivityLifecycleCallbacks() {
            private ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener;

            @Override
            public void onActivityCreated(@NonNull Activity activity, Bundle bundle) {
                final ViewGroup rootView = getRootViewFromActivity(activity, true);
                onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        delegateViewsOnClickListener(activity, rootView);
                    }
                };
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                final ViewGroup rootView = getRootViewFromActivity(activity, true);
                rootView.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                if (Build.VERSION.SDK_INT >= 16) {
                    final ViewGroup rootView = getRootViewFromActivity(activity, true);
                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
                }
            }
        });
    }

    /**
     * 方案二：代理Window的事件回调接口
     *
     * @param application Application
     */
    @TargetApi(14)
    public static void delegateWindowCallback(Application application) {
        application.registerActivityLifecycleCallbacks(new EmptyActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, Bundle bundle) {
                //方案二：包装Window的事件回调接口
                Window window = activity.getWindow();
                Window.Callback callback = window.getCallback();
                window.setCallback(new WrapperWindowCallback(activity, callback));
            }
        });
    }

    /**
     * 方案三：代理Window的事件回调接口
     *
     * @param application Application
     */
    @TargetApi(14)
    public static void delegateAllViewAccessibility(Application application) {
        application.registerActivityLifecycleCallbacks(new EmptyActivityLifecycleCallbacks() {
            private ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener;

            @Override
            public void onActivityCreated(@NonNull Activity activity, Bundle bundle) {
                final ViewGroup rootView = getRootViewFromActivity(activity, true);
                onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        delegateViewAccessibilityDelegate(activity, rootView);
                    }
                };
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                final ViewGroup rootView = getRootViewFromActivity(activity, true);
                rootView.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                if (Build.VERSION.SDK_INT >= 16) {
                    final ViewGroup rootView = getRootViewFromActivity(activity, true);
                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
                }
            }
        });
    }


    /**
     * Delegate view OnClickListener
     *
     * @param context Context
     * @param view    View
     */
    @TargetApi(15)
    @SuppressWarnings("all")
    private static void delegateViewAccessibilityDelegate(final Context context, final View view) {
        if (context == null || view == null) {
            return;
        }

        if (view instanceof RatingBar) {
            final RatingBar.OnRatingBarChangeListener onRatingBarChangeListener =
                    ((RatingBar) view).getOnRatingBarChangeListener();
            if (onRatingBarChangeListener != null &&
                    !(onRatingBarChangeListener instanceof WrapperOnRatingBarChangeListener)) {
                ((RatingBar) view).setOnRatingBarChangeListener(
                        new WrapperOnRatingBarChangeListener(onRatingBarChangeListener));
            }
        } else if (view instanceof SeekBar) {
            final SeekBar.OnSeekBarChangeListener onSeekBarChangeListener =
                    getOnSeekBarChangeListener(view);
            if (onSeekBarChangeListener != null &&
                    !(onSeekBarChangeListener instanceof WrapperOnSeekBarChangeListener)) {
                ((SeekBar) view).setOnSeekBarChangeListener(
                        new WrapperOnSeekBarChangeListener(onSeekBarChangeListener));
            }
        } else if (view instanceof Spinner) {
            AdapterView.OnItemSelectedListener onItemSelectedListener =
                    ((Spinner) view).getOnItemSelectedListener();
            if (onItemSelectedListener != null &&
                    !(onItemSelectedListener instanceof WrapperAdapterViewOnItemSelectedListener)) {
                ((Spinner) view).setOnItemSelectedListener(
                        new WrapperAdapterViewOnItemSelectedListener(onItemSelectedListener));
            }
        } else if (view instanceof ExpandableListView) {
            try {
                Class viewClazz = ExpandableListView.class;
                //Child
                Field mOnChildClickListenerField = viewClazz.getDeclaredField("mOnChildClickListener");
                if (!mOnChildClickListenerField.isAccessible()) {
                    mOnChildClickListenerField.setAccessible(true);
                }
                ExpandableListView.OnChildClickListener onChildClickListener =
                        (ExpandableListView.OnChildClickListener) mOnChildClickListenerField.get(view);
                if (onChildClickListener != null &&
                        !(onChildClickListener instanceof WrapperOnChildClickListener)) {
                    ((ExpandableListView) view).setOnChildClickListener(
                            new WrapperOnChildClickListener(onChildClickListener));
                }

                //Group
                Field mOnGroupClickListenerField = viewClazz.getDeclaredField("mOnGroupClickListener");
                if (!mOnGroupClickListenerField.isAccessible()) {
                    mOnGroupClickListenerField.setAccessible(true);
                }
                ExpandableListView.OnGroupClickListener onGroupClickListener =
                        (ExpandableListView.OnGroupClickListener) mOnGroupClickListenerField.get(view);
                if (onGroupClickListener != null &&
                        !(onGroupClickListener instanceof WrapperOnGroupClickListener)) {
                    ((ExpandableListView) view).setOnGroupClickListener(
                            new WrapperOnGroupClickListener(onGroupClickListener));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (view instanceof ListView ||
                view instanceof GridView) {
            AdapterView.OnItemClickListener onItemClickListener =
                    ((AdapterView) view).getOnItemClickListener();
            if (onItemClickListener != null &&
                    !(onItemClickListener instanceof WrapperAdapterViewOnItemClick)) {
                ((AdapterView) view).setOnItemClickListener(
                        new WrapperAdapterViewOnItemClick(onItemClickListener));
            }
        } else {
            View.AccessibilityDelegate delegate = null;
            try {
                Class<?> kClass = view.getClass();
                Method method = kClass.getMethod("getAccessibilityDelegate");
                delegate = (View.AccessibilityDelegate) method.invoke(view);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (delegate == null || !(delegate instanceof SensorsDataAccessibilityDelegate)) {
                view.setAccessibilityDelegate(new SensorsDataAccessibilityDelegate(delegate));
            }
        }

        //如果 view 是 ViewGroup，需要递归遍历子 View 并 hook
        if (view instanceof ViewGroup) {
            final ViewGroup viewGroup = (ViewGroup) view;
            int childCount = viewGroup.getChildCount();
            if (childCount > 0) {
                for (int i = 0; i < childCount; i++) {
                    View childView = viewGroup.getChildAt(i);
                    //递归
                    delegateViewAccessibilityDelegate(context, childView);
                }
            }
        }
    }

    private static String traverseViewContent(StringBuilder stringBuilder, View root) {
        try {
            if (root == null) {
                return stringBuilder.toString();
            }

            if (root instanceof ViewGroup) {
                final int childCount = ((ViewGroup) root).getChildCount();
                for (int i = 0; i < childCount; ++i) {
                    final View child = ((ViewGroup) root).getChildAt(i);

                    if (child.getVisibility() != View.VISIBLE) {
                        continue;
                    }
                    if (child instanceof ViewGroup) {
                        traverseViewContent(stringBuilder, child);
                    } else {
                        String viewText = getElementContent(child);
                        if (!TextUtils.isEmpty(viewText)) {
                            stringBuilder.append(viewText);
                        }
                    }
                }
            } else {
                stringBuilder.append(getElementContent(root));
            }

            return stringBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return stringBuilder.toString();
        }
    }

    public static void trackAdapterView(AdapterView<?> adapterView, View view, int groupPosition, int childPosition) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("$element_type", adapterView.getClass().getCanonicalName());
            jsonObject.put("$element_id", getViewId(adapterView));
            if (childPosition > -1) {
                jsonObject.put("$element_position", String.format(Locale.CHINA, "%d:%d", groupPosition, childPosition));
            } else {
                jsonObject.put("$element_position", String.format(Locale.CHINA, "%d", groupPosition));
            }
            StringBuilder stringBuilder = new StringBuilder();
            String viewText = traverseViewContent(stringBuilder, view);
            if (!TextUtils.isEmpty(viewText)) {
                jsonObject.put("$element_element", viewText);
            }
            Activity activity = getActivityFromView(adapterView);
            if (activity != null) {
                jsonObject.put("$activity", activity.getClass().getCanonicalName());
            }

            SensorsDataAPI.getInstance().track("$AppClick", jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trackAdapterView(AdapterView<?> adapterView, View view, int position) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("$element_type", adapterView.getClass().getCanonicalName());
            jsonObject.put("$element_id", getViewId(adapterView));
            jsonObject.put("$element_position", String.valueOf(position));
            StringBuilder stringBuilder = new StringBuilder();
            String viewText = traverseViewContent(stringBuilder, view);
            if (!TextUtils.isEmpty(viewText)) {
                jsonObject.put("$element_element", viewText);
            }
            Activity activity = getActivityFromView(adapterView);
            if (activity != null) {
                jsonObject.put("$activity", activity.getClass().getCanonicalName());
            }

            SensorsDataAPI.getInstance().track("$AppClick", jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static void trackAdapterViewOnClick(AdapterView view, MotionEvent motionEvent) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("$element_type", view.getClass().getCanonicalName());
            jsonObject.put("$element_id", SensorsDataPrivate.getViewId(view));

            int count = view.getChildCount();
            for (int i = 0; i < count; i++) {
                View itemView = view.getChildAt(i);
                if (TouchEventHandler.isContainView(itemView, motionEvent)) {
                    jsonObject.put("$element_content", traverseViewContent(new StringBuilder(), itemView));
                    jsonObject.put("$element_position", String.valueOf(i));
                    break;
                }
            }

            Activity activity = SensorsDataPrivate.getActivityFromView(view);
            if (activity != null) {
                jsonObject.put("$activity", activity.getClass().getCanonicalName());
            }

            SensorsDataAPI.getInstance().track("$AppClick", jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * View 被点击，自动埋点
     *
     * @param view View
     */
    @Keep
    protected static void trackViewOnClick(View view) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("$element_type", view.getClass().getCanonicalName());
            jsonObject.put("$element_id", SensorsDataPrivate.getViewId(view));
            jsonObject.put("$element_content", SensorsDataPrivate.getElementContent(view));

            Activity activity = SensorsDataPrivate.getActivityFromView(view);
            if (activity != null) {
                jsonObject.put("$activity", activity.getClass().getCanonicalName());
            }

            SensorsDataAPI.getInstance().track("$AppClick", jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static ViewGroup getRootViewFromActivity(Activity activity, boolean decorView) {
        if (decorView) {
            return (ViewGroup) activity.getWindow().getDecorView();
        } else {
            return activity.findViewById(android.R.id.content);
        }
    }

    /**
     * Delegate view OnClickListener
     *
     * @param context Context
     * @param view    View
     */
    @TargetApi(15)
    @SuppressWarnings("all")
    protected static void delegateViewsOnClickListener(final Context context, final View view) {
        if (context == null || view == null) {
            return;
        }

        if (view instanceof AdapterView) {
            if (view instanceof Spinner) {
                AdapterView.OnItemSelectedListener onItemSelectedListener =
                        ((Spinner) view).getOnItemSelectedListener();
                if (onItemSelectedListener != null &&
                        !(onItemSelectedListener instanceof WrapperAdapterViewOnItemSelectedListener)) {
                    ((Spinner) view).setOnItemSelectedListener(
                            new WrapperAdapterViewOnItemSelectedListener(onItemSelectedListener));
                }
            } else if (view instanceof ExpandableListView) {
                try {
                    Class viewClazz = ExpandableListView.class;
                    //Child
                    Field mOnChildClickListenerField = viewClazz.getDeclaredField("mOnChildClickListener");
                    if (!mOnChildClickListenerField.isAccessible()) {
                        mOnChildClickListenerField.setAccessible(true);
                    }
                    ExpandableListView.OnChildClickListener onChildClickListener =
                            (ExpandableListView.OnChildClickListener) mOnChildClickListenerField.get(view);
                    if (onChildClickListener != null &&
                            !(onChildClickListener instanceof WrapperOnChildClickListener)) {
                        ((ExpandableListView) view).setOnChildClickListener(
                                new WrapperOnChildClickListener(onChildClickListener));
                    }

                    //Group
                    Field mOnGroupClickListenerField = viewClazz.getDeclaredField("mOnGroupClickListener");
                    if (!mOnGroupClickListenerField.isAccessible()) {
                        mOnGroupClickListenerField.setAccessible(true);
                    }
                    ExpandableListView.OnGroupClickListener onGroupClickListener =
                            (ExpandableListView.OnGroupClickListener) mOnGroupClickListenerField.get(view);
                    if (onGroupClickListener != null &&
                            !(onGroupClickListener instanceof WrapperOnGroupClickListener)) {
                        ((ExpandableListView) view).setOnGroupClickListener(
                                new WrapperOnGroupClickListener(onGroupClickListener));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (view instanceof ListView ||
                    view instanceof GridView) {
                AdapterView.OnItemClickListener onItemClickListener =
                        ((AdapterView) view).getOnItemClickListener();
                if (onItemClickListener != null &&
                        !(onItemClickListener instanceof WrapperAdapterViewOnItemClick)) {
                    ((AdapterView) view).setOnItemClickListener(
                            new WrapperAdapterViewOnItemClick(onItemClickListener));
                }
            }
        } else {
            //获取当前 view 设置的 OnClickListener
            final View.OnClickListener listener = getOnClickListener(view);

            //判断已设置的 OnClickListener 类型，如果是自定义的 WrapperOnClickListener，说明已经被 hook 过，防止重复 hook
            if (listener != null && !(listener instanceof WrapperOnClickListener)) {
                //替换成自定义的 WrapperOnClickListener
                view.setOnClickListener(new WrapperOnClickListener(listener));
            } else if (view instanceof CompoundButton) {
                final CompoundButton.OnCheckedChangeListener onCheckedChangeListener =
                        getOnCheckedChangeListener(view);
                if (onCheckedChangeListener != null &&
                        !(onCheckedChangeListener instanceof WrapperOnCheckedChangeListener)) {
                    ((CompoundButton) view).setOnCheckedChangeListener(
                            new WrapperOnCheckedChangeListener(onCheckedChangeListener));
                }
            } else if (view instanceof RadioGroup) {
                final RadioGroup.OnCheckedChangeListener radioOnCheckedChangeListener =
                        getRadioGroupOnCheckedChangeListener(view);
                if (radioOnCheckedChangeListener != null &&
                        !(radioOnCheckedChangeListener instanceof WrapperRadioGroupOnCheckedChangeListener)) {
                    ((RadioGroup) view).setOnCheckedChangeListener(
                            new WrapperRadioGroupOnCheckedChangeListener(radioOnCheckedChangeListener));
                }
            } else if (view instanceof RatingBar) {
                final RatingBar.OnRatingBarChangeListener onRatingBarChangeListener =
                        ((RatingBar) view).getOnRatingBarChangeListener();
                if (onRatingBarChangeListener != null &&
                        !(onRatingBarChangeListener instanceof WrapperOnRatingBarChangeListener)) {
                    ((RatingBar) view).setOnRatingBarChangeListener(
                            new WrapperOnRatingBarChangeListener(onRatingBarChangeListener));
                }
            } else if (view instanceof SeekBar) {
                final SeekBar.OnSeekBarChangeListener onSeekBarChangeListener =
                        getOnSeekBarChangeListener(view);
                if (onSeekBarChangeListener != null &&
                        !(onSeekBarChangeListener instanceof WrapperOnSeekBarChangeListener)) {
                    ((SeekBar) view).setOnSeekBarChangeListener(
                            new WrapperOnSeekBarChangeListener(onSeekBarChangeListener));
                }
            }
        }

        //如果 view 是 ViewGroup，需要递归遍历子 View 并 hook
        if (view instanceof ViewGroup) {
            final ViewGroup viewGroup = (ViewGroup) view;
            int childCount = viewGroup.getChildCount();
            if (childCount > 0) {
                for (int i = 0; i < childCount; i++) {
                    View childView = viewGroup.getChildAt(i);
                    //递归
                    delegateViewsOnClickListener(context, childView);
                }
            }
        }
    }

    @SuppressWarnings("all")
    private static SeekBar.OnSeekBarChangeListener getOnSeekBarChangeListener(View view) {
        try {
            Class viewClazz = SeekBar.class;
            Field mOnCheckedChangeListenerField = viewClazz.getDeclaredField("mOnSeekBarChangeListener");
            if (!mOnCheckedChangeListenerField.isAccessible()) {
                mOnCheckedChangeListenerField.setAccessible(true);
            }
            return (SeekBar.OnSeekBarChangeListener) mOnCheckedChangeListenerField.get(view);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("all")
    private static RadioGroup.OnCheckedChangeListener getRadioGroupOnCheckedChangeListener(View view) {
        try {
            Class viewClazz = RadioGroup.class;
            Field mOnCheckedChangeListenerField = viewClazz.getDeclaredField("mOnCheckedChangeListener");
            if (!mOnCheckedChangeListenerField.isAccessible()) {
                mOnCheckedChangeListenerField.setAccessible(true);
            }
            return (RadioGroup.OnCheckedChangeListener) mOnCheckedChangeListenerField.get(view);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取 CheckBox 设置的 OnCheckedChangeListener
     *
     * @param view
     * @return
     */
    @SuppressWarnings("all")
    private static CompoundButton.OnCheckedChangeListener getOnCheckedChangeListener(View view) {
        try {
            Field mOnCheckedChangeListenerField = null;
            if (view instanceof MaterialCheckBox) {
                Class viewClazz = MaterialCheckBox.class;
                mOnCheckedChangeListenerField = viewClazz.getDeclaredField("onCheckedChangeListener");
            } else {
                Class viewClazz = CompoundButton.class;
                mOnCheckedChangeListenerField = viewClazz.getDeclaredField("mOnCheckedChangeListener");
            }
            if (!mOnCheckedChangeListenerField.isAccessible()) {
                mOnCheckedChangeListenerField.setAccessible(true);
            }
            return (CompoundButton.OnCheckedChangeListener) mOnCheckedChangeListenerField.get(view);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取 View 当前设置的 OnClickListener
     *
     * @param view View
     * @return View.OnClickListener
     */
    @SuppressWarnings({"all"})
    @TargetApi(15)
    private static View.OnClickListener getOnClickListener(View view) {
        boolean hasOnClick = view.hasOnClickListeners();
        if (hasOnClick) {
            try {
                Class viewClazz = View.class;
                Method listenerInfoMethod = viewClazz.getDeclaredMethod("getListenerInfo");
                if (!listenerInfoMethod.isAccessible()) {
                    listenerInfoMethod.setAccessible(true);
                }
                Object listenerInfoObj = listenerInfoMethod.invoke(view);
                Class listenerInfoClazz = Class.forName("android.view.View$ListenerInfo");
                Field onClickListenerField = listenerInfoClazz.getDeclaredField("mOnClickListener");
                if (!onClickListenerField.isAccessible()) {
                    onClickListenerField.setAccessible(true);
                }
                return (View.OnClickListener) onClickListenerField.get(listenerInfoObj);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 获取 view 的 android:id 对应的字符串
     *
     * @param view View
     * @return String
     */
    private static String getViewId(View view) {
        String idString = null;
        try {
            if (view.getId() != View.NO_ID) {
                idString = view.getContext().getResources().getResourceEntryName(view.getId());
            }
        } catch (Exception e) {
            //ignore
        }
        return idString;
    }

    /**
     * 获取 View 上显示的文本
     *
     * @param view View
     * @return String
     */
    @SuppressLint("RestrictedApi")
    private static String getElementContent(View view) {
        if (view == null) {
            return null;
        }

        String text = null;
        if (view instanceof Button) {
            text = ((Button) view).getText().toString();
        } else if (view instanceof ActionMenuItemView) {
            text = ((ActionMenuItemView) view).getText().toString();
        } else if (view instanceof TextView) {
            text = ((TextView) view).getText().toString();
        } else if (view instanceof ImageView) {
            text = view.getContentDescription().toString();
        } else if (view instanceof RadioGroup) {
            RadioGroup radioGroup = (RadioGroup) view;
            int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
            RadioButton radioButton = radioGroup.findViewById(checkedRadioButtonId);
            if (radioButton != null) {
                text = radioButton.getText().toString();
            }
        } else if (view instanceof RatingBar) {
            text = String.valueOf(((RatingBar) view).getRating());
        } else if (view instanceof SeekBar) {
            text = String.valueOf(((SeekBar) view).getProgress());
        } else if (view instanceof ViewGroup) {
            text = traverseViewContent(new StringBuilder(), view);
        }
        return text;
    }

    /**
     * 获取 View 所属 Activity
     *
     * @param view View
     * @return Activity
     */
    private static Activity getActivityFromView(View view) {
        Activity activity = null;
        if (view == null) {
            return null;
        }

        try {
            Context context = view.getContext();
            if (context != null) {
                if (context instanceof Activity) {
                    activity = (Activity) context;
                } else if (context instanceof ContextWrapper) {
                    while (!(context instanceof Activity) && context instanceof ContextWrapper) {
                        context = ((ContextWrapper) context).getBaseContext();
                    }
                    if (context instanceof Activity) {
                        activity = (Activity) context;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return activity;
    }

    public static Map<String, Object> getDeviceInfo(Context context) {
        final Map<String, Object> deviceInfo = new HashMap<>();
        {
            deviceInfo.put("$lib", "Android");
            deviceInfo.put("$lib_version", SensorsDataAPI.SDK_VERSION);
            deviceInfo.put("$os", "Android");
            deviceInfo.put("$os_version",
                    Build.VERSION.RELEASE == null ? "UNKNOWN" : Build.VERSION.RELEASE);
            deviceInfo
                    .put("$manufacturer", Build.MANUFACTURER == null ? "UNKNOWN" : Build.MANUFACTURER);
            if (TextUtils.isEmpty(Build.MODEL)) {
                deviceInfo.put("$model", "UNKNOWN");
            } else {
                deviceInfo.put("$model", Build.MODEL.trim());
            }

            try {
                final PackageManager manager = context.getPackageManager();
                final PackageInfo packageInfo = manager.getPackageInfo(context.getPackageName(), 0);
                deviceInfo.put("$app_version", packageInfo.versionName);

                int labelRes = packageInfo.applicationInfo.labelRes;
                deviceInfo.put("$app_name", context.getResources().getString(labelRes));
            } catch (final Exception e) {
                e.printStackTrace();
            }

            final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            deviceInfo.put("$screen_height", displayMetrics.heightPixels);
            deviceInfo.put("$screen_width", displayMetrics.widthPixels);

            return Collections.unmodifiableMap(deviceInfo);
        }
    }

    /**
     * 获取 Android ID
     *
     * @param mContext Context
     * @return String
     */
    @SuppressLint("HardwareIds")
    public static String getAndroidID(Context mContext) {
        String androidID = "";
        try {
            androidID = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return androidID;
    }

    private static void addIndentBlank(StringBuilder sb, int indent) {
        try {
            for (int i = 0; i < indent; i++) {
                sb.append('\t');
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String formatJson(String jsonStr) {
        try {
            if (null == jsonStr || "".equals(jsonStr)) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            char last;
            char current = '\0';
            int indent = 0;
            boolean isInQuotationMarks = false;
            for (int i = 0; i < jsonStr.length(); i++) {
                last = current;
                current = jsonStr.charAt(i);
                switch (current) {
                    case '"':
                        if (last != '\\') {
                            isInQuotationMarks = !isInQuotationMarks;
                        }
                        sb.append(current);
                        break;
                    case '{':
                    case '[':
                        sb.append(current);
                        if (!isInQuotationMarks) {
                            sb.append('\n');
                            indent++;
                            addIndentBlank(sb, indent);
                        }
                        break;
                    case '}':
                    case ']':
                        if (!isInQuotationMarks) {
                            sb.append('\n');
                            indent--;
                            addIndentBlank(sb, indent);
                        }
                        sb.append(current);
                        break;
                    case ',':
                        sb.append(current);
                        if (last != '\\' && !isInQuotationMarks) {
                            sb.append('\n');
                            addIndentBlank(sb, indent);
                        }
                        break;
                    default:
                        sb.append(current);
                }
            }

            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
