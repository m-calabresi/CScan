package com.cscan;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.cscan.classes.Info;
import com.cscan.classes.XMLParser;
import com.cscan.classes.layout.RecyclerViewAdapter;

import java.util.ArrayList;
import java.util.List;

import static com.cscan.ScanActivity.INTENT_EXTRA_TITLE;

public class MainActivity extends AppCompatActivity {
    static {
        //allow vector icons in buttons
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private static final int PENDING_REMOVAL_TIMEOUT = 3500;

    private List<Info> infos;
    private List<Info> pendingInfos;
    private XMLParser parser;

    private TextView emptyView;
    private RecyclerView mRecyclerView;
    private RecyclerViewAdapter mRecyclerViewAdapter;

    private Runnable pendingRemovalRunnable;
    private Handler pendingRemovalHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        emptyView = (TextView) findViewById(R.id.empty_view);

        FloatingActionButton scan_fab = (FloatingActionButton) findViewById(R.id.scan_fab);
        scan_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ScanActivity.class));
            }
        });

        pendingRemovalHandler = new Handler();
        pendingInfos = new ArrayList<>();

        //setup empty view
        SpannableString string = new SpannableString(getString(R.string.text_empty_view));
        ImageSpan scanImageSpan = new ImageSpan(this, R.drawable.ic_scan_grey);
        string.setSpan(scanImageSpan, 57, 59, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        emptyView.setText(string);

        bindRecyclerView();
    }

    @Override
    public void onResume() {
        super.onResume();
        bindRecyclerView();
        bindEmptyView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings)
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        return super.onOptionsItemSelected(item);
    }

    private void bindRecyclerView() {
        parser = new XMLParser(getApplicationContext());
        infos = parser.read();

        mRecyclerViewAdapter = new RecyclerViewAdapter(infos);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mRecyclerViewAdapter);
        mRecyclerView.setHasFixedSize(true);

        bindItemTouchHelper();
    }

    private void bindEmptyView() {
        if (infos.size() > 0) {
            emptyView.setVisibility(View.GONE);
            if (mRecyclerView != null) //this method may be called before bindRecyclerView
                mRecyclerView.setOverScrollMode(View.OVER_SCROLL_ALWAYS); //enable scroll effect
        } else {
            emptyView.setVisibility(View.VISIBLE);
            if (mRecyclerView != null) //this method may be called before bindRecyclerView
                mRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER); //disable scroll effect
        }
    }

    private void bindItemTouchHelper() {

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback
                = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            // not important, we don't want drag & drop
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                int swipedPosition = viewHolder.getAdapterPosition();
                remove(swipedPosition);
            }
        };
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);
    }

    protected void remove(int position) {
        Info info = infos.get(position);
        pendingInfos.add(info); //add info to delete list

        infos.remove(position);
        mRecyclerViewAdapter.notifyItemRemoved(position);
        //update UI (if necessary)
        bindEmptyView();

        actionUndoMessage(getString(R.string.file_delete_success),
                BaseTransientBottomBar.LENGTH_LONG, //remove-thread and snackbar have same lifetime
                getString(R.string.action_undo),
                position);

        pendingRemovalRunnable = new Runnable() {
            @Override
            public void run() {
                /*every thread gets the first element and removes it*/
                Info info;
                synchronized (this) {
                    info = pendingInfos.get(0);
                    pendingInfos.remove(0);
                }
                if (!parser.delete(info)) {
                    textMessage(getString(R.string.file_delete_error),
                            BaseTransientBottomBar.LENGTH_SHORT);
                }
            }
        };
        pendingRemovalHandler.postDelayed(pendingRemovalRunnable, PENDING_REMOVAL_TIMEOUT);
    }

    protected void undo(int position) {
        /*terminates the last started thread and remove the last item swiped from the removing list*/
        Info info;
        int lastPendingPosition;

        pendingRemovalHandler.removeCallbacks(pendingRemovalRunnable);
        synchronized (this) {
            lastPendingPosition = pendingInfos.size() - 1;
            if (lastPendingPosition > -1) {
                info = pendingInfos.get(lastPendingPosition);
                pendingInfos.remove(lastPendingPosition);
                infos.add(position, info);
                mRecyclerViewAdapter.notifyItemInserted(position);
                //update UI (if necessary)
                bindEmptyView();
            }
        }
    }

    public void textMessage(String message, int time) {
        final Snackbar snackbar = Snackbar.make(findViewById(R.id.main_activity), message, time);
        setNotDismissible(snackbar);
        snackbar.show();
    }

    public void actionUndoMessage(String message, int time, String action, final int position) {
        final Snackbar snackbar = Snackbar.make(findViewById(R.id.main_activity), message, time);
        snackbar.setAction(action, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                undo(position);
            }
        });
        setNotDismissible(snackbar);
        snackbar.show();
    }

    public void openViewActivity(Info info) {
        if (!info.isNull()) {
            Intent openViewActivity = new Intent(MainActivity.this, EditActivity.class);
            openViewActivity.putExtra(INTENT_EXTRA_TITLE, info);
            startActivity(openViewActivity);
        } else
            textMessage(getString(R.string.generic_error),
                    BaseTransientBottomBar.LENGTH_LONG);
    }

    private void setNotDismissible(final Snackbar snackbar) {
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
    }
}
