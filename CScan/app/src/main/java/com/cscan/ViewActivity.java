package com.cscan;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.KeyListener;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.cscan.classes.ClipboardManager;
import com.cscan.classes.Info;
import com.cscan.classes.XMLParser;
import com.cscan.classes.layout.CustomEditText;

public class ViewActivity extends AppCompatActivity {

    private FloatingActionButton done_fab;
    private FloatingActionButton undo_fab;
    private CustomEditText editText;
    private TextView hintTextView;

    private boolean isEditing;
    private Info info;

    private XMLParser parser;

    private KeyListener defaultKeyListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
                finish();
            }
        });

        init();
    }

    private void init() {
        info = getIntent().getExtras().getParcelable(MainActivity.INTENT_EXTRA_TITLE);

        isEditing = false;
        parser = new XMLParser(getApplicationContext());
        done_fab = (FloatingActionButton) findViewById(R.id.done_fab);
        undo_fab = (FloatingActionButton) findViewById(R.id.undo_fab);
        editText = (CustomEditText) findViewById(R.id.edit_text);
        hintTextView = (TextView) findViewById(R.id.hint_text_view);

        defaultKeyListener = editText.getKeyListener();

        // Set it to null - this will make the field non-editable
        editText.setKeyListener(null);

        hintTextView.setText(getString(R.string.hint_copy_link));
        editText.setText(info.getText());
        editText.setCursorVisible(false);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        done_fab.setImageResource(R.drawable.ic_edit_white);
        undo_fab.setVisibility(View.INVISIBLE);

        done_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isEditing) {
                    String message;
                    Info newInfo;

                    newInfo = new Info(editText.getText().toString());

                    if (!info.equals(newInfo)) {
                        if (parser.update(info, newInfo)) {
                            //update Info
                            info = newInfo;

                            message = getString(R.string.file_update_success);
                        } else
                            message = getString(R.string.file_update_error);
                        simpleMessage(message, BaseTransientBottomBar.LENGTH_SHORT);
                    }
                    dismiss();
                } else
                    edit();
            }
        });

        undo_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //undo all changes by refill strView with it's original content
                editText.setText(info.getText());
                dismiss();
            }
        });

        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isEditing)
                    edit();
                else
                    editText.setBackgroundResource(R.drawable.edit_text_bg_is_edited);
            }
        });

        editText.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!isEditing)
                    ClipboardManager.copyToClipboard(editText.getText().toString(), v.getContext());
                else
                    editText.setBackgroundResource(R.drawable.edit_text_bg_is_edited);

                //return false = after long click a normal click is performed
                return true;
            }
        });

        editText.setBackPressedListener(new CustomEditText.BackPressedListener() {
            @Override
            public void onImeBack(CustomEditText editText) {
                if (isEditing) {
                    //undo all changes by refill strView with it's original content
                    editText.setText(info.getText());
                    dismiss();
                }
            }
        });
    }

    private void edit() {
        isEditing = true;
        //Place cursor to the end of text
        editText.setSelection(editText.getText().length());
        // Restore key listener - this will make the field editable again.
        editText.setKeyListener(defaultKeyListener);
        // Focus the field.
        editText.requestFocus();
        // Show soft keyboard for the user to enter the value.
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        done_fab.setImageResource(R.drawable.ic_done);
        undo_fab.setVisibility(View.VISIBLE);
        editText.setCursorVisible(true);
        hintTextView.setVisibility(View.INVISIBLE);
    }

    private void dismiss() {
        isEditing = false;
        done_fab.setImageResource(R.drawable.ic_edit_white);
        undo_fab.setVisibility(View.INVISIBLE);
        editText.setCursorVisible(false);
        editText.setBackgroundResource(R.drawable.edit_text_bg);
        hintTextView.setVisibility(View.VISIBLE);

        hideKeyboard();
    }

    private void hideKeyboard() {
        // Hide soft keyboard.
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        // Make EditText non-editable again.
        editText.setKeyListener(null);
    }

    private void simpleMessage(String message, int time) {
        final Snackbar snackbar = Snackbar.make(findViewById(R.id.view_activity), message, time);
        //makes snackbar not dismissible in CoordinatorLayout
        snackbar.getView().getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                snackbar.getView().getViewTreeObserver().removeOnPreDrawListener(this);
                ((CoordinatorLayout.LayoutParams) snackbar.getView().getLayoutParams())
                        .setBehavior(null);
                return true;
            }
        });
        snackbar.show();
    }
}
