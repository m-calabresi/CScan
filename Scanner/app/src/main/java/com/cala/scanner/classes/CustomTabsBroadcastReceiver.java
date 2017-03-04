package com.cala.scanner.classes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CustomTabsBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String url = intent.getDataString();
         ClipboardManager.copyToClipboard(url, context);
    }
}
