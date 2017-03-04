package com.cala.scanner.classes.layout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

public class CustomEditText extends EditText {

    private BackPressedListener mOnImeBack;

    public CustomEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            if (mOnImeBack != null) mOnImeBack.onImeBack(this);
        }
        return super.dispatchKeyEvent(event);
    }

    public void setBackPressedListener(BackPressedListener listener) {
        mOnImeBack = listener;
    }

    public interface BackPressedListener {
        void onImeBack(CustomEditText editText);
    }
}
