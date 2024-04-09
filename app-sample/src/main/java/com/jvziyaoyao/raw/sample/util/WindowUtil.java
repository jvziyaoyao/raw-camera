package com.jvziyaoyao.raw.sample.util;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class WindowUtil
{
    /**
     * 隐藏activity的系统状态栏
     */
    public static void hideStatusBar(Activity activity)
    {
        View decorView = activity.getWindow().getDecorView();
        int oldUi = decorView.getSystemUiVisibility();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        int newUi = oldUi | uiOptions;
        decorView.setSystemUiVisibility(newUi);
    }

    /**
     * 隐藏activity的系统状态栏
     */
    public static void hideStatusBar(Dialog dialog)
    {
        View decorView = dialog.getWindow().getDecorView();
        int oldUi = decorView.getSystemUiVisibility();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        int newUi = oldUi | uiOptions;
        decorView.setSystemUiVisibility(newUi);
    }


    /**
     * 隐藏activity的系统导航栏
     * activity在设置了隐藏系统导航栏后，弹出dialog会导致底部导航栏又显示出来，
     * 具体原因是因为dialog的Window抢走了焦点，Window中的DecorView状态改变导致的。
     * 解决办法是：
     *      给dialog的window设置WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE，让dialog不获取焦点。
     *      【但是】FLAG_NOT_FOCUSABLE表示Window不需要获取焦点，也不需要接受各种输入事件，最明显的现象就是
     *      点击dialog上的EditText时键盘不会弹出来(但是其他各种View的点击事件可以触发)。
     *      要在dialog的show方法执行完后将FLAG_NOT_FOCUSABLE清除，这样可以使得dialog弹出后不会显示系统导
     *      航栏，而且dialog能正常获取焦点，正常弹出输入法
     */
    public static void hideNavigationBar(Activity activity)
    {
        View decorView = activity.getWindow().getDecorView();
        int oldUi = decorView.getSystemUiVisibility();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        int newUi = oldUi | uiOptions;
        decorView.setSystemUiVisibility(newUi);
    }

    /**
     * 隐藏dialog的系统导航栏
     * activity在设置了隐藏系统导航栏后，弹出dialog会导致底部导航栏又显示出来，
     * 具体原因是因为dialog的Window抢走了焦点，Window中的DecorView状态改变导致的。
     * 解决办法是：
     *      给dialog的window设置WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE，让dialog不获取焦点。
     *      【但是】FLAG_NOT_FOCUSABLE表示Window不需要获取焦点，也不需要接受各种输入事件，最明显的现象就是
     *      点击dialog上的EditText时键盘不会弹出来(但是其他各种View的点击事件可以触发)。
     *      要在dialog的show方法执行完后将FLAG_NOT_FOCUSABLE清除，这样可以使得dialog弹出后不会显示系统导
     *      航栏，而且dialog能正常获取焦点，正常弹出输入法
     */
    public static void hideNavigationBar(Dialog dialog)
    {
        Window window = dialog.getWindow();
        View decorView = window.getDecorView();
        int oldUi = decorView.getSystemUiVisibility();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        int newUi = oldUi | uiOptions;
        decorView.setSystemUiVisibility(newUi);

        //一定要加上flag
        int flag = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        window.setFlags(flag, flag);
    }

    /**
     * 清除dialog的FLAG_NOT_FOCUSABLE标志位，具体原因看WindowUtil.hideNavigationBar(Dialog dialog)的注释
     */
    public static void clearNotFocusableFlag(Dialog dialog)
    {
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    }
}
