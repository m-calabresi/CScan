package com.cala.scanner.classes;

import android.content.ClipData;
import android.content.Context;

import static android.content.Context.CLIPBOARD_SERVICE;

public class ClipboardManager {

    public static void copyToClipboard(String text, Context context) {
        //copy text to clipboard
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                context.getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("text_info", text);
        clipboard.setPrimaryClip(clip);
    }
}
