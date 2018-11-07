package com.example.jaycee.pomdpobjectsearch;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.Toolbar;

import com.example.jaycee.pomdpobjectsearch.helpers.ImageUtils;

import java.nio.ByteBuffer;

public abstract class ActivityCameraBase extends Activity implements ImageReader.OnImageAvailableListener
{
    private static final String TAG = ActivityCameraBase.class.getSimpleName();

    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private static final String STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    private static final int PERMISSIONS_REQUEST = 0;

    protected int previewWidth = 0;
    protected int previewHeight = 0;

    private byte[] previewBytes;
    private byte[] processingBytes;

    private boolean isProcessingFrame = false;

    private Handler handler;
    private HandlerThread handlerThread;

    private Runnable postInferenceCallback;
    private Runnable previewImageConverter, imageProcessingConverter;

    protected FrameHandler frameHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_camera);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setActionBar(toolbar);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);

        frameHandler = (FrameHandler)this;
    }

    @Override
    public synchronized void onStart()
    {
        Log.d(TAG, "onStart " + this);
        super.onStart();
    }

    @Override
    public synchronized void onResume()
    {
        super.onResume();

        if(!hasPermission())
        {
            requestPermission();
        }

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

    @Override
    public synchronized void onStop()
    {
        Log.d(TAG, "onStop " + this);
        super.onStop();
    }

    @Override
    public synchronized void onDestroy()
    {
        Log.d(TAG, "onDestroy" + this);
        super.onDestroy();
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

/*        if(rgbBytes == null)
        {
            rgbBytes = new int[previewWidth*previewHeight];
        }*/

        if(previewBytes == null)
        {
            previewBytes = new byte[previewHeight*previewWidth*4];
        }

        if(processingBytes == null)
        {
            processingBytes = new byte[previewHeight*previewWidth*4];
        }

        try
        {
            final Image image = reader.acquireLatestImage();
            if(image == null) return;

            previewImageConverter = new Runnable()
            {
                @Override
                public void run()
                {
                    previewBytes = YUV_420_888_data(image);
                }
            };

            frameHandler.onPreviewFrame(getPreviewBytes(), image.getWidth(), image.getHeight());

            if(isProcessingFrame)
            {
                image.close();
                return;
            }

            isProcessingFrame = true;
            Trace.beginSection("ImageAvailable");
            postInferenceCallback = new Runnable()
            {
                @Override
                public void run()
                {
                    image.close();
                    isProcessingFrame = false;
                }
            };

            processingBytes = previewBytes.clone();
            processImage();
        }
        catch(final Exception e)
        {
            Log.e(TAG, "Exception on ImageReader: " + e);
            Trace.endSection();
            return;
        }
        Trace.endSection();
    }

    protected byte[] getPreviewBytes()
    {
        if(previewImageConverter!= null)
        {
            previewImageConverter.run();
        }
        return previewBytes;
    }

    protected byte[] getProcessingBytes()
    {
        return processingBytes;
    }

/*    protected void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes)
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
    }*/

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

    private static byte[] YUV_420_888_data(Image image) {
        final int imageWidth = image.getWidth();
        final int imageHeight = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[imageWidth * imageHeight *
                ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8];
        int offset = 0;

        for (int plane = 0; plane < planes.length; ++plane) {
            final ByteBuffer buffer = planes[plane].getBuffer();
            final int rowStride = planes[plane].getRowStride();
            // Experimentally, U and V planes have |pixelStride| = 2, which
            // essentially means they are packed.
            final int pixelStride = planes[plane].getPixelStride();
            final int planeWidth = (plane == 0) ? imageWidth : imageWidth / 2;
            final int planeHeight = (plane == 0) ? imageHeight : imageHeight / 2;
            if (pixelStride == 1 && rowStride == planeWidth) {
                // Copy whole plane from buffer into |data| at once.
                buffer.get(data, offset, planeWidth * planeHeight);
                offset += planeWidth * planeHeight;
            } else {
                // Copy pixels one by one respecting pixelStride and rowStride.
                byte[] rowData = new byte[rowStride];
                for (int row = 0; row < planeHeight - 1; ++row) {
                    buffer.get(rowData, 0, rowStride);
                    for (int col = 0; col < planeWidth; ++col) {
                        data[offset++] = rowData[col * pixelStride];
                    }
                }
                // Last row is special in some devices and may not contain the full
                // |rowStride| bytes of data.
                // See http://developer.android.com/reference/android/media/Image.Plane.html#getBuffer()
                buffer.get(rowData, 0, Math.min(rowStride, buffer.remaining()));
                for (int col = 0; col < planeWidth; ++col) {
                    data[offset++] = rowData[col * pixelStride];
                }
            }
        }

        return data;
    }

    protected abstract void processImage();

    protected abstract void onPreviewSizeChosen(final Size size, final int rotation);
    protected abstract Size getDesiredPreviewSize();
    // protected abstract int getLayoutId();
}
