package com.example.jaycee.pomdpobjectsearch;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.jaycee.pomdpobjectsearch.helpers.ImageUtils;

import java.nio.ByteBuffer;

public abstract class ActivityCameraBase extends Activity implements ImageReader.OnImageAvailableListener
{
    private static final String TAG = ActivityCameraBase.class.getSimpleName();

    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private static final String STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    private static final int PERMISSIONS_REQUEST = 0;

    private int previewWidth = 0;
    private int previewHeight = 0;
    private int yRowStride;

    private int[] rgbBytes = null;

    private byte[][] yuvBytes = new byte[3][];

    private boolean isProcessingFrame = false;

    private Handler handler;
    private HandlerThread handlerThread;

    private Runnable postInferenceCallback;
    private Runnable imageConverter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_camera);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);

        if(!hasPermission())
        {
            requestPermission();
        }
    }

    @Override
    public synchronized void onResume()
    {
        super.onResume();

        handlerThread = new HandlerThread("InferenceThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public synchronized void onPause()
    {
        if(!isFinishing())
        {
            finish();
        }

        handlerThread.quitSafely();
        try
        {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        }
        catch(final InterruptedException e)
        {
            Log.e(TAG, "Exception onPause: " + e);
        }

        super.onPause();
    }

    protected synchronized void runInBackground(final Runnable r)
    {
        if(handler != null)
        {
            handler.post(r);
        }
    }

    private boolean hasPermission()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            return checkSelfPermission(CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(STORAGE_PERMISSION) == PackageManager.PERMISSION_GRANTED;
        }
        else
        {
            return true;
        }
    }

    private void requestPermission()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (shouldShowRequestPermissionRationale(CAMERA_PERMISSION) ||
                    shouldShowRequestPermissionRationale(STORAGE_PERMISSION))
            {
                Toast.makeText(ActivityCameraBase.this,
                        "Camera AND storage permission are required for this demo", Toast.LENGTH_LONG).show();
            }
            requestPermissions(new String[] {CAMERA_PERMISSION, STORAGE_PERMISSION}, PERMISSIONS_REQUEST);
        }
    }

    // Camera2 API callback
    @Override
    public void onImageAvailable(final ImageReader reader)
    {
        // Need to have preview sizes set
        if(previewHeight == 0 || previewWidth == 0) return;

        if(rgbBytes == null)
        {
            rgbBytes = new int[previewWidth*previewHeight];
        }

        try
        {
            final Image image = reader.acquireLatestImage();

            if(image == null) return;
            // renderFrame();

            if(isProcessingFrame)
            {
                image.close();
                return;
            }

            isProcessingFrame = true;
            Trace.beginSection("ImageAvailable");

            final Image.Plane[] planes = image.getPlanes();
            fillBytes(planes, yuvBytes);

            yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            imageConverter = new Runnable()
            {
                @Override
                public void run()
                {
                    ImageUtils.convertYUV420ToARGB8888(yuvBytes[0], yuvBytes[1], yuvBytes[2],
                            previewWidth, previewHeight,
                            yRowStride, uvRowStride, uvPixelStride,
                            rgbBytes);
                }
            };

            postInferenceCallback = new Runnable()
            {
                @Override
                public void run()
                {
                    image.close();
                    isProcessingFrame = false;
                }
            };

            // CALL CLASSIFIER HERE
            // processImage();
        }
        catch(final Exception e)
        {
            Log.e(TAG, "Exception on imagereader: " + e);
            Trace.endSection();
            return;
        }
        Trace.endSection();
    }

    protected void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes)
    {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; i++)
        {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null)
            {
                Log.d(TAG, String.format("Initializing buffer %d at size %d", i, buffer.capacity()));
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    protected void readyForNextImage()
    {
        if (postInferenceCallback != null)
        {
            postInferenceCallback.run();
        }
    }

    protected int getScreenOrientation()
    {
        switch (getWindowManager().getDefaultDisplay().getRotation())
        {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    // protected abstract void processImage();

    // protected abstract void onPreviewSizeChosen(final Size size, final int rotation);
    // protected abstract Size getDesiredPreviewFrameSize();
    // protected abstract int getLayoutId();
}
