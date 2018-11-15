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
import com.example.jaycee.pomdpobjectsearch.views.OverlayView;
import com.example.jaycee.pomdpobjectsearch.helpers.Logger;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public abstract class ActivityCameraBase extends Activity implements ImageReader.OnImageAvailableListener
{
    private static final String TAG = ActivityCameraBase.class.getSimpleName();
    private static final Logger LOGGER = new Logger(TAG);

    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private static final String STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    private static final int PERMISSIONS_REQUEST = 0;

    private Handler handler;
    private HandlerThread handlerThread;
    private boolean isProcessingFrame = false;
    private byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    private int yRowStride;

    private boolean debug = false;

    protected int previewWidth = 0;
    protected int previewHeight = 0;

    private byte[] previewBytes;
    private byte[] processingBytes;

    private Runnable postInferenceCallback;
    private Runnable previewImageConverter, objImageConverter;

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

    protected int[] getRgbBytes() {
        objImageConverter.run();
        return rgbBytes;
    }

    protected int getLuminanceStride() {
        return yRowStride;
    }

    protected byte[] getLuminance() {
        return yuvBytes[0];
    }

    @Override
    public synchronized void onStart()
    {
        LOGGER.d("onStart " + this);
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
            LOGGER.e("Exception onPause: " + e);
        }

        super.onPause();
    }

    @Override
    public synchronized void onStop()
    {
        LOGGER.d("onStop " + this);
        super.onStop();
    }

    @Override
    public synchronized void onDestroy()
    {
        LOGGER.d("onDestroy" + this);
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

    /**
     * Callback for Camera2 API
     */
    @Override
    public void onImageAvailable(final ImageReader reader)
    {

       // Need to have preview sizes set
       if(previewHeight == 0 || previewWidth == 0) return;

       if(rgbBytes == null)
        {
            rgbBytes = new int[previewWidth*previewHeight];
        }

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
            if(image == null)
                return;

            final Image.Plane[] planes = image.getPlanes();

            previewImageConverter = new Runnable() {
                @Override
                public void run()
                {
                    previewBytes = YUV_420_888_data(image);
                    LOGGER.d("Converting image");

                }
            };

            renderFrame(image);

            if(isProcessingFrame)
            {
                image.close();
                return;
            }
            isProcessingFrame = true;

            postInferenceCallback = new Runnable()
            {
                @Override
                public void run()
                {
                    image.close();
                    isProcessingFrame = false;
                }
            };

            Trace.beginSection("ImageAvailable");

            fillBytes(planes, yuvBytes);
            yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            //needed, because I need a int[] array (rgbBytes) for the bitmap
            //TODO: merge with the other converter, to make all at one time
            objImageConverter = new Runnable()
            {
                @Override
                public void run()
                {

                    ImageUtils.convertYUV420ToARGB8888(
                            yuvBytes[0],
                            yuvBytes[1],
                            yuvBytes[2],
                            image.getWidth(), //the image size is 1600x1200
                            image.getHeight(),
                            yRowStride,
                            uvRowStride,
                            uvPixelStride,
                            rgbBytes);

                }
            };

            processImage();
        }
        catch(final Exception e)
        {
            LOGGER.e("Exception on ImageReader: " + e);
            Trace.endSection();
            postInferenceCallback.run();
            return;
        }
        Trace.endSection();
    }

    protected void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes)
    {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            buffer.position(0);
            if (yuvBytes[i] == null) {
                LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    protected byte[] getPreviewBytes()
    {
        if(previewImageConverter!= null)
        {
            previewImageConverter.run();
        }
        return previewBytes;
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

    private static byte[] YUV_420_888_data(Image image)
    {
        final int imageWidth = image.getWidth();
        final int imageHeight = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[imageWidth * imageHeight *
                ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8];
        int offset = 0;

        for (int plane = 0; plane < planes.length; ++plane)
        {
            final ByteBuffer buffer = planes[plane].getBuffer();
            final int rowStride = planes[plane].getRowStride();
            // Experimentally, U and V planes have |pixelStride| = 2, which
            // essentially means they are packed.
            final int pixelStride = planes[plane].getPixelStride();
            final int planeWidth = (plane == 0) ? imageWidth : imageWidth / 2;
            final int planeHeight = (plane == 0) ? imageHeight : imageHeight / 2;
            if (pixelStride == 1 && rowStride == planeWidth)
            {
                // Copy whole plane from buffer into |data| at once.
//                while (buffer.remaining() >= 36)
                buffer.get(data, offset, planeWidth * planeHeight);
                offset += planeWidth * planeHeight;
            }
            else
             {
                // Copy pixels one by one respecting pixelStride and rowStride.
                byte[] rowData = new byte[rowStride];
                for (int row = 0; row < planeHeight - 1; ++row)
                {
                    buffer.get(rowData, 0, rowStride);
                    for (int col = 0; col < planeWidth; ++col)
                    {
                        data[offset++] = rowData[col * pixelStride];
                    }
                }
                // Last row is special in some devices and may not contain the full
                // |rowStride| bytes of data.
                // See http://developer.android.com/reference/android/media/Image.Plane.html#getBuffer()
                buffer.get(rowData, 0, Math.min(rowStride, buffer.remaining()));
                for (int col = 0; col < planeWidth; ++col)
                {
                    data[offset++] = rowData[col * pixelStride];
                }
            }
        }

        return data;
    }

    public boolean isDebug() {
        return debug;
    }

    public void requestRender() {
        final OverlayView overlay = (OverlayView) findViewById(R.id.debug_overlay);
        if (overlay != null) {
            overlay.postInvalidate();
        }
    }

    public void addCallback(final OverlayView.DrawCallback callback) {
        final OverlayView overlay = (OverlayView) findViewById(R.id.debug_overlay);
        if (overlay != null) {
            overlay.addCallback(callback);
        }
    }

    protected abstract void processImage();
    protected abstract void renderFrame(Image image);

    protected abstract void onPreviewSizeChosen(final Size size, final int rotation);
    protected abstract Size getDesiredPreviewSize();
    // protected abstract int getLayoutId();
}
