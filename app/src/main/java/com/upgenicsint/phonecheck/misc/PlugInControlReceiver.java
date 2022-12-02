package com.upgenicsint.phonecheck.misc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;

import com.upgenicsint.phonecheck.R;

public class PlugInControlReceiver extends BroadcastReceiver {

    static String wireless = "WirelessActivity";

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = -1;
        if (intent != null) {
            plugged = intent.getIntExtra("plugged", -1);
        }
        if ((action != null && action.equals(Intent.ACTION_POWER_CONNECTED)) || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.WIRELESS));
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.CHARGER));
        } else if (action != null && action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.COMMENTS));
        }
    }

//    SharedPreferences savePluggedState = context.getSharedPreferences(context.getString(R.string.wireless_battery_charging), Context.MODE_PRIVATE);
//    SharedPreferences.Editor wirelessEditor = savePluggedState.edit();
//    intent =context.registerReceiver(null,new
//
//    IntentFilter(Intent.ACTION_BATTERY_CHANGED));
//    int plugged = -1;
//        if(intent !=null)
//
//    {
//        plugged = intent.getIntExtra("plugged", -1);
//    }
//    if(action !=null&&action.equals(Intent.ACTION_POWER_CONNECTED)&&plugged ==BatteryManager.BATTERY_PLUGGED_WIRELESS)
//
//    {
//        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.WIRELESS));
//        Log.d(wireless, "Wireless Charging Connected");
//
//        wirelessEditor.putInt(context.getString(R.string.wireless_plugged_state), plugged);
//        wirelessEditor.apply();
//    }
//    else
//SharedPreferences wirelessPrefs = context.getSharedPreferences(context.getString(R.string.wireless_battery_charging), Context.MODE_PRIVATE);
//    int wirelessState = wirelessPrefs.getInt(context.getString(R.string.wireless_plugged_state), -1);
//            if(wirelessState !=BatteryManager.BATTERY_PLUGGED_WIRELESS &&wirelessState !=0)
//
//    {
//        showToast(context);
//        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.WIRELESS));
//    }
//private void showToast(Context context) {
//    Toast.makeText(context, "USB/AC Charging Removed", Toast.LENGTH_SHORT).show();
//}
}