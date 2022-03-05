package com.example.getldp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootOrUpdateCompleteReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {

        Log.d("RECEIVER: ", "IN ONRECEIVE");
        if (intent.getAction() == null
                || !(intent.getAction().equals("android.intent.action.BOOT_COMPLETED")
                || intent.getAction().equals("android.intent.action.MY_PACKAGE_REPLACED")
                || intent.getAction().equals("android.intent.action.QUICKBOOT_POWERON"))) {
            return;
        }
        Log.d("RECEIVER:", "got signal successfully: " + intent.getAction());
        HttpWorker.enqueueSelf(context);
    }
}