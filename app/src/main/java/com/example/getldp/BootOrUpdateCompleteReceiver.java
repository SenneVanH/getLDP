package com.example.getldp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootOrUpdateCompleteReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null
                || !intent.getAction().equals("android.intent.action.BOOT_COMPLETED")
                || !intent.getAction().equals("android.intent.action.MY_PACKAGE_REPLACED")) {
            return;
        }
        HttpWorker.enqueueSelf(context);
    }
}