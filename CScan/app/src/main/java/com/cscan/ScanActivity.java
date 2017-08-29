package com.cscan;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.cscan.classes.CustomTabsBroadcastReceiver;
import com.cscan.classes.Info;
import com.cscan.classes.URIChecker;
import com.cscan.classes.XMLParser;
import com.cscan.classes.CameraPreview;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.util.List;

@SuppressWarnings("deprecation")
public class ScanActivity extends AppCompatActivity {

    public static final String CUSTOM_TAB_PACKAGE_NAME = "com.android.chrome";
    public static final String INTENT_EXTRA_TITLE = "scan_result";

    private static final int AUTOFOCUS_DELAY = 1000; //delay before adjust autofocus (millis)

    private CustomTabsServiceConnection mCustomTabsServiceConnection;
    private CustomTabsClient mCustomTabsClient;
    protected CustomTabsSession mCustomTabsSession;
    private CustomTabsIntent customTabsIntent;

    private XMLParser parser;
    private List<Info> infos;

    private Camera mCamera;
    private Handler autoFocusHandler;

    private ImageScanner scanner;
    private CameraPreview mPreview;
    private FrameLayout preview;

    private ImageButton flashButton;
    private LinearLayout cameraLayout;

    private Camera.Parameters params;
    public static int currentCamera = Camera.CameraInfo.CAMERA_FACING_BACK; //default opened camera

    private SharedPreferences sharedPreferences;
    private boolean scanBarcodes;
    private boolean openLinks;

    private boolean previewing = true;
    private boolean flashOn = false;
    private boolean wasFlashOn = false;

    static {
        System.loadLibrary("iconv");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        //full-screen mode
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        sharedPreferences = getSharedPreferences(
                getString(R.string.cscan_shared_preference_name), MODE_PRIVATE);
        scanBarcodes = sharedPreferences.getBoolean(
                getString(R.string.pref_key_scan_barcode), false);
        openLinks = sharedPreferences.getBoolean(
                getString(R.string.pref_key_open_links), false);

        //toggle flash button
        flashButton = findViewById(R.id.flash_button);
        flashButton.setBackgroundResource(R.drawable.ic_flash_off); //default icon
        flashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flashOn) {
                    flashOff();
                } else {
                    if (hasFlash()) {
                        flashOn();
                    } else {
                        errorToast(getString(R.string.flash_open_error));
                    }
                }
            }
        });

        //switch camera
        ImageButton cameraReverseButton = findViewById(R.id.camera_reverse_button);
        cameraReverseButton.setBackgroundResource(R.drawable.ic_switch_camera);
        cameraReverseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swapCamera();
            }
        });

        cameraLayout = findViewById(R.id.camera_layout);

        parser = new XMLParser(getApplicationContext());
        infos = parser.read();

        //chrome custom tabs
        bindCustomTabsService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (flashOn) {
            flashOff();
            wasFlashOn = true;
        }
        releaseCamera();
    }

    @Override
    protected void onStart() {
        super.onStart();
        openCamera(currentCamera);
        if (wasFlashOn)
            flashOn();
    }

    @Override
    protected void onDestroy() {
        unbindCustomTabsService();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (flashOn)
            flashOff();
        releaseCamera();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return false;
    }

    private void bindCustomTabsService() {
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        Intent intent = new Intent(this, CustomTabsBroadcastReceiver.class); //copy link action
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setToolbarColor(ResourcesCompat.getColor(getResources(), R.color.colorPrimary, null));
        builder.setShowTitle(true);
        //back arrow icon - NOT WORKING
        /*builder.setCloseButtonIcon(BitmapFactory.decodeResource(
                getResources(), R.drawable.ic_arrow_back));*/
        builder.addMenuItem(getString(R.string.action_copy_link), pendingIntent);

        mCustomTabsServiceConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName componentName,
                                                     CustomTabsClient customTabsClient) {
                mCustomTabsClient = customTabsClient;
                mCustomTabsClient.warmup(0);
                mCustomTabsSession = mCustomTabsClient.newSession(null);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mCustomTabsClient = null;
            }
        };

        if (!CustomTabsClient.bindCustomTabsService(
                this, CUSTOM_TAB_PACKAGE_NAME, mCustomTabsServiceConnection))
            mCustomTabsServiceConnection = null;
        customTabsIntent = builder.build();
    }

    private void unbindCustomTabsService() {
        if (mCustomTabsServiceConnection == null) return;
        unbindService(mCustomTabsServiceConnection);
        mCustomTabsClient = null;
        mCustomTabsSession = null;
    }

    private void openCamera(int cameraType) {
        autoFocusHandler = new Handler();

        mCamera = getCameraInstance(cameraType);
        if (mCamera == null) {
            errorToast(getString(R.string.camera_open_error));
            return;
        }

        // Instance barcode scanner
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);

        //qr-code only
        if(!scanBarcodes){
            scanner.setConfig(0, Config.ENABLE, 0); //Disable all the Symbols
            scanner.setConfig(Symbol.QRCODE, Config.ENABLE, 1); //Only QRCODE is enable
        }

        mPreview
                = new CameraPreview(ScanActivity.this, mCamera, previewCb, autoFocusCB);

        preview = findViewById(R.id.cameraPreview);
        preview.addView(mPreview);
        //add views on top of FrameLayout
        preview.removeView(cameraLayout);
        preview.addView(cameraLayout);
    }

    private void swapCamera() {
        if (currentCamera == Camera.CameraInfo.CAMERA_FACING_BACK) {
            currentCamera = Camera.CameraInfo.CAMERA_FACING_FRONT;
            flashButton.setVisibility(View.INVISIBLE); //no flash on front camera

            if (flashOn)//turns flash off (if active)
                flashOff();
        } else {
            currentCamera = Camera.CameraInfo.CAMERA_FACING_BACK;
            flashButton.setVisibility(View.VISIBLE);
        }

        releaseCamera();
        openCamera(currentCamera);
    }

    public static Camera getCameraInstance(int cameraType) {
        Camera c;
        try {
            c = Camera.open(cameraType);
        } catch (Exception e) {
            c = null;
        }
        return c;
    }

    private void releaseCamera() {
        if (mCamera != null && mPreview != null) {
            previewing = false;
            mCamera.setPreviewCallback(null);
            mPreview.surfaceDestroyed(mPreview.getHolder());
            mPreview.getHolder().removeCallback(mPreview);
            mPreview.destroyDrawingCache();
            preview.removeView(mPreview);
            mCamera.release();
            mCamera = null;
            mPreview = null;
        }
    }

    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (previewing) {
                Camera.Parameters params = mCamera.getParameters();
                mCamera.autoFocus(autoFocusCB);

                //prevent AutoBalance stops after AutoFocus called
                if (params.isAutoExposureLockSupported()) {
                    params.setAutoExposureLock(true);
                    mCamera.setParameters(params);

                    params = mCamera.getParameters();
                    params.setAutoExposureLock(false);
                    mCamera.setParameters(params);
                }
            }
        }
    };

    Camera.PreviewCallback previewCb = new Camera.PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = parameters.getPreviewSize();

            Image barcode = new Image(size.width, size.height, "Y800");
            barcode.setData(data);

            int result = scanner.scanImage(barcode);

            if (result != 0) {
                previewing = false;
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();

                SymbolSet syms = scanner.getResults();
                Symbol sym = syms.iterator().next();
                String scanResult = sym.getData();

                int foundPos;
                Info info = new Info(scanResult);

                //check for duplicate item
                if ((foundPos = (parser.find(info))) == -1) {
                    //check for URI
                    if (URIChecker.isURI(scanResult) && openLinks)
                        openLink(scanResult);
                    else { //not a URI
                        setNextError(null); //no errors to report
                        openViewActivity(info);
                    }

                    //save scanned element
                    if (!parser.write(info))
                        setNextError(getString(R.string.pref_key_next_error));
                    infos.add(info);
                } else {
                    //open already saved element
                    setNextError(getString(R.string.file_duplicate_error));
                    openViewActivity(infos.get(foundPos));
                }
            }
        }
    };

    // Mimic continuous auto-focusing
    Camera.AutoFocusCallback autoFocusCB = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.postDelayed(doAutoFocus, AUTOFOCUS_DELAY);
        }
    };

    private void flashOn() {
        flashButton.setBackgroundResource(R.drawable.ic_flash_on);
        //flash on
        params = mCamera.getParameters();
        params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        mCamera.setParameters(params);

        flashOn = true;
    }

    private void flashOff() {
        flashButton.setBackgroundResource(R.drawable.ic_flash_off);
        //flash off
        params = mCamera.getParameters();
        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(params);

        flashOn = false;
    }

    public boolean hasFlash() {
        if (mCamera == null) {
            return false;
        }

        Camera.Parameters parameters = mCamera.getParameters();

        if (parameters.getFlashMode() == null) {
            return false;
        }

        List<String> supportedFlashModes = parameters.getSupportedFlashModes();
        return !(supportedFlashModes == null || supportedFlashModes.isEmpty()
                || supportedFlashModes.size() == 1
                && supportedFlashModes.get(0).equals(Camera.Parameters.FLASH_MODE_OFF));
    }

    public void openLink(String link) {
        Uri url;
        //check for syntax URI error
        link = URIChecker.toLink(link);
        //open link
        url = Uri.parse(link);
        customTabsIntent.launchUrl(this, url);
        finish();
    }

    public void openViewActivity(Info info) {
        if (!info.isNull()) {
            Intent openViewActivity = new Intent(ScanActivity.this, EditActivity.class);
            openViewActivity.putExtra(INTENT_EXTRA_TITLE, info);
            startActivity(openViewActivity);
            finish();
        } else
            errorToast(getString(R.string.generic_error));
    }

    private void errorToast(String message) {
        Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 120);//toasst
        toast.show();
    }

    //notify user in the next activity
    private void setNextError(String errorMsg) {
        if (errorMsg != null)
            sharedPreferences.edit().putString(getString(R.string.pref_key_next_error),
                    errorMsg).apply();
        else
            sharedPreferences.edit().putString(getString(R.string.pref_key_next_error),
                    getString(R.string.pref_no_next_error)).apply();
    }
}