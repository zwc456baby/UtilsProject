package com.example.zhouwc.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.util.LinkedList;

/**
 * Created by zhouwenchao on 2017-09-20.
 */
public final class LayoutUtil {
    private static LayoutUtil instance;

    private final int progressID = GeneratedID.getID();

    private LayoutUtil() {
    }

    public static LayoutUtil instance() {
        if (instance == null) {
            synchronized (LayoutUtil.class) {
                if (instance == null) {
                    instance = new LayoutUtil();
                }
            }
        }
        return instance;
    }


    /**
     * 设置全屏并隐藏底部导航栏菜单
     */
    public void setFullScreenAndHidMenu(Activity activity) {
        Log.d("设置全屏并隐藏底部导航栏菜单");
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //一下为设置虚拟按键为三个小圆点
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        params.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE;
        activity.getWindow().setAttributes(params);
    }

    /**
     * 隐藏虚拟按键，并且全屏
     */
    public void hideMenu(final Window window) {
        //隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = window.getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            View decorView = window.getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
        window.getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int i) {
                hideMenu(window);
            }
        });
    }

    public FragManager getFramManager(@IdRes int layout, LinkedList<Class<? extends Fragment>> fragments,
                                      android.support.v4.app.FragmentManager fragmentManager) {
        return new FragManager(layout, fragments, fragmentManager);
    }


    public static View Inflater(LayoutInflater inflater, int layoutID, AttachViewCallBack callBack) {
        return instance().inflater(inflater, layoutID, null, false, callBack);
    }

    public static View Inflater(LayoutInflater inflater, int layoutID, ViewGroup container, boolean attachToRoot, AttachViewCallBack callBack) {
        return instance().inflater(inflater, layoutID, container, attachToRoot, callBack);
    }

    private View inflater(LayoutInflater inflater, int layoutID, ViewGroup container, boolean attachToRoot, AttachViewCallBack callBack) {
        ViewGroup viewGroup = createRootView(inflater.getContext());
        ThreadUtils.execute(new InflaterRunnable(viewGroup, inflater, layoutID, container, attachToRoot, callBack));
        return viewGroup;
    }

    private ViewGroup createRootView(Context context) {
        ViewGroup groupView = new RelativeLayout(context);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
//        ViewGroup groupView = new FrameLayout(context);
//        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        groupView.setLayoutParams(layoutParams);
        ViewGroup progressGroup = new RelativeLayout(context);
        ProgressBar progressBar = new ProgressBar(context);
        RelativeLayout.LayoutParams progressGrouplayout = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        RelativeLayout.LayoutParams progresslayout = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

        progressGroup.setLayoutParams(progressGrouplayout);
        progresslayout.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        progressBar.setLayoutParams(progresslayout);
        progressGroup.setId(progressID);

        progressGroup.addView(progressBar);
        groupView.addView(progressGroup);
        return groupView;
    }

    private final class InflaterRunnable implements Runnable {
        private ViewGroup viewGroup;
        private LayoutInflater inflater;
        private int layouId;
        private ViewGroup container;
        private boolean attachToRootView;
        private AttachViewCallBack attachViewCallBack;

        private InflaterRunnable(ViewGroup group, LayoutInflater inflater, int layoutID, ViewGroup container, boolean attachToRoot, AttachViewCallBack callBack) {
            this.viewGroup = group;
            this.inflater = inflater;
            this.layouId = layoutID;
            this.container = container;
            this.attachToRootView = attachToRoot;
            this.attachViewCallBack = callBack;
        }

        @Override
        public void run() {
//            Looper.prepare();
            final View view = inflater.inflate(layouId, container, attachToRootView);
            if (view == null) return;
            if (attachViewCallBack != null) attachViewCallBack.AttachViewStart(view);
            ThreadUtils.post(new Runnable() {
                @Override
                public void run() {
//                    if (view instanceof ViewGroup) {
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                            viewGroup.setClipToPadding(((ViewGroup) view).getClipToPadding());
//                        }
//                        if (view.getFitsSystemWindows()) {
//                            viewGroup.setFitsSystemWindows(true);
//                            Drawable background = view.getBackground();
//                            view.setBackground(null);
//                            if (background != null) viewGroup.setBackground(background);
//                        }
//                    }
                    viewGroup.removeView(viewGroup.findViewById(progressID));
//                    if (background != null) viewGroup.setBackground(background);
//                    viewGroup.addView(view);
                    if (attachViewCallBack != null) attachViewCallBack.AttachViewOver(view);
                }
            });
        }
    }

    public final class FragManager {
        //        private LinkedList<String> stackIndex = new LinkedList<>();
        private LinkedList<Fragment> isNewList = new LinkedList<>();
        private LinkedList<Class<? extends Fragment>> fragmentList;
        private android.support.v4.app.FragmentManager fragmentManager;
        private int layout;
        private int cacheCount = 2;


        public FragManager(@IdRes int layout, LinkedList<Class<? extends Fragment>> fragments, android.support.v4.app.FragmentManager fragmentManager) {
            this.layout = layout;
            fragmentList = fragments;
            this.fragmentManager = fragmentManager;
        }

        public void setCacheCount(int count) {
            if (count > 1) {
                cacheCount = count;
            } else {
                throw new RuntimeException("缓存条目不能小于1");
            }
        }

        public boolean showFragment(int index) {
            Fragment fragment;
            android.support.v4.app.FragmentManager manager = fragmentManager;
            removeLastFragment(manager);
            hideAllFragment(manager);
        /*添加更多Fragment到此处*/
//        if (index == 0) {
//            fragment = getListFragment(MainFragment:: class.java)
//        }
//        if (index == 1) {
//            fragment = getListFragment(BindFaceFragment:: class.java)
//        }
            fragment = getListFragment(index);
        /*结束添加*/
            if (fragment == null) {
                Class<? extends Fragment> fragmentClass = fragmentList.get(index);
                try {
                    fragment = fragmentClass.newInstance();
                    Log.v(fragmentClass.getName());
                    Log.v(fragment.getClass().getName());
                    isNewList.addFirst(fragment);
//                    stackIndex.addFirst(fragmentClass.getName());
                    manager.beginTransaction().add(layout, fragment).commit();
                    return true;
                } catch (InstantiationException e) {
                    Log.e(e);
                    return false;
                } catch (IllegalAccessException e) {
                    Log.e(e);
                    return false;
                }
            } else {
                if (isNewList.remove(fragment)) {
                    isNewList.addFirst(fragment);   //将其从列表移除后添加到最前面
                }
//                stackIndex.addFirst(fragment.getClass().getName());
                manager.beginTransaction().show(fragment).commit();
                return true;
            }
        }


        private Fragment getListFragment(int index) {
            Class<? extends Fragment> fragmentClass = fragmentList.get(index);
            for (Fragment obj : isNewList) {
                if (obj.getClass().getName().equals(fragmentClass.getName())) {
                    return obj;
                }
            }
            return null;
        }

        private void hideAllFragment(android.support.v4.app.FragmentManager manager) {
            FragmentTransaction transaction = manager.beginTransaction();
            for (Fragment obj : isNewList) {
                transaction.hide(obj);
            }
            transaction.commit();
        }

        private void removeLastFragment(android.support.v4.app.FragmentManager manager) {
            FragmentTransaction transaction = manager.beginTransaction();
            while (isNewList.size() > cacheCount) {
                transaction.remove(isNewList.removeLast());
            }
            transaction.commit();
        }
    }

    public interface AttachViewCallBack {
        void AttachViewStart(View childView);

        void AttachViewOver(View childView);
    }
}
