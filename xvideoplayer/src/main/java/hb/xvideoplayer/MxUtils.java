package hb.xvideoplayer;


import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;

import java.util.Formatter;
import java.util.Locale;

public class MxUtils {

    public static  String stringForTime(long milliseconds) {
        if (milliseconds < 0 || milliseconds >= 24 * 60 * 60 * 1000) {
            return "00:00";
        }
        milliseconds /= 1000;
        int minute = (int) (milliseconds / 60);
        int hour = minute / 60;
        int second = (int) (milliseconds % 60);
        minute %= 60;
        StringBuilder stringBuilder = new StringBuilder();
        Formatter mFormatter = new Formatter(stringBuilder, Locale.getDefault());
        if (hour > 0) {
            return mFormatter.format("%02d:%02d:%02d", hour, minute, second).toString();
        } else {
            return mFormatter.format("%02d:%02d", minute, second).toString();
        }
    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI);
    }

    public static AppCompatActivity getAppComptActivity(Context context) {
        if (context == null) {
            return null;
        }
        if (context instanceof AppCompatActivity) {
            return (AppCompatActivity) context;
        } else if (context instanceof ContextThemeWrapper) {
            return getAppComptActivity(((ContextThemeWrapper)context).getBaseContext());
        }
        return null;
    }

    public static Activity scanForActivity(Context context) {
        if (context == null) {
            return null;
        }
        if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof ContextWrapper) {
            return scanForActivity(((ContextWrapper) context).getBaseContext());
        }
        return null;
    }
}
