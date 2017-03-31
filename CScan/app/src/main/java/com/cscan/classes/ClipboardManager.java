package com.cscan.classes;

import android.content.ClipData;
import android.content.Context;

import static android.content.Context.CLIPBOARD_SERVICE;

public class ClipboardManager {
    private static final String TEXT_LABEL = "text_info";

    public static void copyToClipboard(String text, Context context) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                context.getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(TEXT_LABEL, text);
        clipboard.setPrimaryClip(clip);
    }
}