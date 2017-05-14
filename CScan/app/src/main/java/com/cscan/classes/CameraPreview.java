package com.cscan.classes;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.cscan.ScanActivity;

@SuppressWarnings("deprecation")
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private Context context;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Camera.PreviewCallback previewCallback;
    private Camera.AutoFocusCallback autoFocusCallback;

    public CameraPreview(Context context, Camera camera,
                         Camera.PreviewCallback previewCb,
                         Camera.AutoFocusCallback autoFocusCb) {
        super(context);
        this.context = context;
        mCamera = camera;
        previewCallback = previewCb;
        autoFocusCallback = autoFocusCb;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);

        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        try {
            mCamera.setPreviewDisplay(holder);
        } catch (Exception e) {
            // ignore: Error setting camera preview
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //change preview orientation according to screen orientation
        setCameraDisplayOrientation((Activity) context, ScanActivity.currentCamera, mCamera);

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.startPreview();
        } catch (Exception e) {
            // ignore: tried to start a non-existent preview
        }

        Camera.Parameters params = mCamera.getParameters();
        params.setExposureCompensation(params.getMaxExposureCompensation());

        //unlock auto exposure
        if (params.isAutoExposureLockSupported()) {
            params.setAutoExposureLock(false);
        }
        //unlock auto white balance
        if (params.isAutoWhiteBalanceLockSupported()) {
            params.setAutoWhiteBalanceLock(false);
            params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        }
        mCamera.setParameters(params);
        mCamera.stopPreview();
        try {
            mCamera.setPreviewCallback(previewCallback);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.autoFocus(autoFocusCallback);
        } catch (Exception e) {
            //ignore: Error starting camera preview"
        }
        mCamera.startPreview();
    }

    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}