package com.cala.scanner;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.LinearInterpolator;

import java.util.List;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.cala.scanner.classes.CustomTabsBroadcastReceiver;
import com.cala.scanner.classes.XMLParser;
import com.cala.scanner.classes.Info;
import com.cala.scanner.classes.layout.InfoAdapter;
import com.cala.scanner.classes.PreferencesHelper;
import com.cala.scanner.classes.URIChecker;

import net.sourceforge.zbar.Symbol; //QR-Code only library

// TODO: 25/01/2017 ALLA FINE aggiungere logo
public class MainActivity extends AppCompatActivity {
    private static final int ZBAR_SCANNER_REQUEST = 0;
    private static final int ZBAR_OR_SCANNER_REQUEST = 1; //QR-Code only variable

    public static final String CUSTOM_TAB_PACKAGE_NAME = "com.android.chrome";

    protected boolean scanBarcodes;
    protected String scanResult;
    protected List<Info> infos;
    protected XMLParser parser;

    protected Toolbar toolbar;
    protected FloatingActionButton scan_fab;
    protected RecyclerView mRecyclerView;
    protected InfoAdapter mInfoAdapter;

    protected PreferencesHelper preferencesHelper;

    protected CustomTabsIntent.Builder builder;
    protected CustomTabsClient mCustomTabsClient;
    protected CustomTabsSession mCustomTabsSession;
    protected CustomTabsServiceConnection mCustomTabsServiceConnection;
    protected CustomTabsIntent customTabsIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        scan_fab = (FloatingActionButton) findViewById(R.id.scan_fab);
        scan_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scan();
            }
        });

        preferencesHelper = new PreferencesHelper(this);

        setUpInAppWebBrowser();
        setUpRecyclerView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        scanBarcodes = preferencesHelper.getPreferences("pref_scanBarcodes");
        menu.findItem(R.id.action_scan_barcodes).setChecked(scanBarcodes);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_scan_barcodes) {
            item.setChecked(!item.isChecked());
            scanBarcodes = item.isChecked();

            preferencesHelper.savePreferences("pref_scanBarcodes", scanBarcodes);
        }

        return super.onOptionsItemSelected(item);
    }

    protected void scan() {
        Intent i = new Intent(MainActivity.this, ZBarScannerActivity.class);
        if (!scanBarcodes)
            i.putExtra(ZBarConstants.SCAN_MODES, new int[]{Symbol.QRCODE});
        startActivityForResult(i, ZBAR_SCANNER_REQUEST);
    }

    protected void openViewActivity(Info info) {
        if (!info.isNull()) {
            Intent openViewActivity = new Intent(MainActivity.this, ViewActivity.class);
            openViewActivity.putExtra("scan_result", info);
            startActivity(openViewActivity);
        } else
            Snackbar.make(findViewById(R.id.main_activity),
                    getString(R.string.generic_error),
                    BaseTransientBottomBar.LENGTH_LONG)
                    .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ZBAR_SCANNER_REQUEST || requestCode == ZBAR_OR_SCANNER_REQUEST) {
            if (resultCode == RESULT_OK) {
                //get scan result
                scanResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);
                //convert into Info
                final Info info = new Info(scanResult);

                //check for duplicate item
                if (!parser.find(info)) {
                    //check for URI
                    if (URIChecker.isURI(scanResult)) {
                        //check for syntax URI error
                        scanResult = URIChecker.toLink(scanResult);
                        //open link
                        Uri url = Uri.parse(scanResult);
                        customTabsIntent.launchUrl(this, url);
                    } else //not a URI
                        openViewActivity(info);

                    //save scanned element
                    if (!parser.write(info))
                        Snackbar.make(findViewById(R.id.main_activity),
                                getString(R.string.file_write_error),
                                BaseTransientBottomBar.LENGTH_LONG)
                                .show();
                } else
                    Snackbar.make(findViewById(R.id.main_activity),
                            getString(R.string.file_duplicate_item),
                            Snackbar.LENGTH_LONG)
                            .setAction(getString(R.string.snackbar_open_button),
                                    new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            //open already saved element
                                            int i;

                                            for (i = 0; i < infos.size(); i++)
                                                if (infos.get(i).getText().equals(info.getText())) {
                                                    openViewActivity(infos.get(i));
                                                }
                                        }
                                    })
                            .show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //update RecyclerView
        setUpRecyclerView();
    }

    private void setUpInAppWebBrowser(){
        PendingIntent pendingIntent;
        Intent intent;

        builder = new CustomTabsIntent.Builder();
        
        builder.setToolbarColor(ResourcesCompat.getColor(getResources(), R.color.colorPrimary, null));
        builder.setShowTitle(true);
        //back arrow icon
        builder.setCloseButtonIcon(BitmapFactory.decodeResource(
                getResources(), R.drawable.ic_arrow_back));
        //copy link action
        intent = new Intent(this, CustomTabsBroadcastReceiver.class);
        pendingIntent= PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addMenuItem(getString(R.string.copy_link_menu_item), pendingIntent);

        mCustomTabsServiceConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName componentName, CustomTabsClient customTabsClient) {
                mCustomTabsClient= customTabsClient;
                mCustomTabsClient.warmup(0L);
                mCustomTabsSession = mCustomTabsClient.newSession(null);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mCustomTabsClient= null;
            }
        };

        CustomTabsClient.bindCustomTabsService(this,
                CUSTOM_TAB_PACKAGE_NAME,
                mCustomTabsServiceConnection);

        customTabsIntent = builder.build();
    }

    private void setUpRecyclerView() {
        parser = new XMLParser(getApplicationContext());
        infos = parser.read();

        mInfoAdapter = new InfoAdapter(infos, customTabsIntent);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mInfoAdapter);
        mRecyclerView.setHasFixedSize(true);

        setUpItemTouchHelper();
        setUpAnimationDecoratorHelper();
    }

    private void setUpItemTouchHelper() {

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback
                = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

            // not important, we don't want drag & drop
            @Override
            public boolean onMove(RecyclerView recyclerView,
                                  RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                String message;
                int swipedPosition = viewHolder.getAdapterPosition();
                //delete element from CSV file
                if (parser.delete(infos.get(swipedPosition)))
                    message = getString(R.string.file_delete_success);
                else
                    message = getString(R.string.file_delete_error);

                //update RecyclerView
                InfoAdapter adapter = (InfoAdapter) mRecyclerView.getAdapter();
                adapter.remove(swipedPosition);
                //make FAB visible (if hidden)
                scan_fab.animate().translationY(0).setInterpolator(new LinearInterpolator()).start();
                //notify user
                Snackbar.make(viewHolder.itemView, message, BaseTransientBottomBar.LENGTH_SHORT).show();
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

        };
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);
    }

    private void setUpAnimationDecoratorHelper() {
        mRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {

            // we want to cache this and not allocate anything repeatedly in the onDraw method
            Drawable background;
            boolean initiated;

            private void init() {
                background = new ColorDrawable(Color.TRANSPARENT);
                initiated = true;
            }

            @Override
            public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {

                if (!initiated) {
                    init();
                }

                // only if animation is in progress
                if (parent.getItemAnimator().isRunning()) {

                    // some items might be animating down and some items might be animating up to close the gap left by the removed item
                    // this is not exclusive, both movement can be happening at the same time
                    // to reproduce this leave just enough items so the first one and the last one would be just a little off screen
                    // then remove one from the middle

                    // find first child with translationY > 0
                    // and last one with translationY < 0
                    // we're after a rect that is not covered in recycler-view views at this point in time
                    View lastViewComingDown = null;
                    View firstViewComingUp = null;

                    // this is fixed
                    int left = 0;
                    int right = parent.getWidth();

                    // this we need to find out
                    int top = 0;
                    int bottom = 0;

                    // find relevant translating views
                    int childCount = parent.getLayoutManager().getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        View child = parent.getLayoutManager().getChildAt(i);
                        if (child.getTranslationY() < 0) {
                            // view is coming down
                            lastViewComingDown = child;
                        } else if (child.getTranslationY() > 0) {
                            // view is coming up
                            if (firstViewComingUp == null) {
                                firstViewComingUp = child;
                            }
                        }
                    }

                    if (lastViewComingDown != null && firstViewComingUp != null) {
                        // views are coming down AND going up to fill the void
                        top = lastViewComingDown.getBottom() + (int) lastViewComingDown.getTranslationY();
                        bottom = firstViewComingUp.getTop() + (int) firstViewComingUp.getTranslationY();
                    } else if (lastViewComingDown != null) {
                        // views are going down to fill the void
                        top = lastViewComingDown.getBottom() + (int) lastViewComingDown.getTranslationY();
                        bottom = lastViewComingDown.getBottom();
                    } else if (firstViewComingUp != null) {
                        // views are coming up to fill the void
                        top = firstViewComingUp.getTop();
                        bottom = firstViewComingUp.getTop() + (int) firstViewComingUp.getTranslationY();
                    }
                    background.setBounds(left, top, right, bottom);
                    background.draw(c);
                }
                super.onDraw(c, parent, state);
            }
        });
    }
}
