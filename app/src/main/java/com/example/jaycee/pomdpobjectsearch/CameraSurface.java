package com.example.jaycee.pomdpobjectsearch;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Size;
import android.view.SurfaceHolder;

import com.example.jaycee.pomdpobjectsearch.helpers.Logger;
import com.example.jaycee.pomdpobjectsearch.rendering.CameraRenderer;
import com.google.ar.core.Session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CameraSurface extends GLSurfaceView implements SurfaceHolder.Callback
{
    private static final String TAG = CameraSurface.class.getSimpleName();
    private static final Logger LOGGER = new Logger(TAG);

    private static final int MINIMUM_PREVIEW_SIZE = 320;

    private Context context;

    private Session session;

    private CameraRenderer renderer;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private ImageReader imageReader;

    private Size previewSize;
    private Size inputSize;

    private Semaphore cameraOpenCloseLock = new Semaphore(1);

    private String cameraId;
    private Integer sensorOrientation;

    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    public CameraSurface(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        this.context = context;

        renderer = new CameraRenderer(this);
        inputSize = ((ActivityCamera)context).getDesiredPreviewSize();

        getHolder().addCallback(this);

        setPreserveEGLContextOnPause(true);
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder)
    {
        super.surfaceCreated(surfaceHolder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height)
    {
        super.surfaceChanged(surfaceHolder, format, width, height);

        if(imageReader == null)
        {
            imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2);
            imageReader.setOnImageAvailableListener((ActivityCamera)context, backgroundHandler);
            openCamera();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder)
    {
        super.surfaceDestroyed(surfaceHolder);
        renderer.destroyRenderer();
    }

    public void openCamera()
    {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return;

        CameraManager manager = (CameraManager)context.getSystemService(Context.CAMERA_SERVICE);
        try
        {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS))
            {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            for (String cameraId : manager.getCameraIdList())
            {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                final StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                final Size largest =  Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.YUV_420_888)), new CompareSizesByArea());

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT)
                {
                    continue;
                }
                sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                previewSize = inputSize; /* chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                                inputSize.getWidth(),
                                inputSize.getHeight());*/
                this.cameraId = cameraId;
            }
            manager.openCamera(cameraId, stateCallback, backgroundHandler);
            ((ActivityCamera)context).onPreviewSizeChosen(previewSize, sensorOrientation);
        }
        catch (CameraAccessException e)
        {
            LOGGER.e("Cannot access the camera." + e.toString());
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    public void closeCamera()
    {
        try
        {
            cameraOpenCloseLock.acquire();

            if (null != cameraCaptureSession)
            {
                cameraCaptureSession.close();
                cameraCaptureSession = null;
            }
            if (null != cameraDevice)
            {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (imageReader != null)
            {
                imageReader.close();
                imageReader = null;
            }
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        }
        finally
        {
            cameraOpenCloseLock.release();
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback()
    {
        @Override
        public void onOpened(@NonNull CameraDevice camera)
        {
            // This method is called when the camera is opened.  We start camera preview here.
            cameraOpenCloseLock.release();
            cameraDevice = camera;
            createCaptureSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera)
        {
            cameraOpenCloseLock.release();
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraOpenCloseLock.release();
            camera.close();
            cameraDevice = null;
        }
    };

    private void createCaptureSession()
    {
        try
        {
            if (null == cameraDevice || null == imageReader) return;
            cameraDevice.createCaptureSession(Collections.singletonList(imageReader.getSurface()),
                    sessionStateCallback, backgroundHandler);

        }
        catch (CameraAccessException e)
        {
            LOGGER.e("createCaptureSession " + e.toString());
        }
    }

    public void startBackgroundThread()
    {
        backgroundThread = new HandlerThread("CameraBackgroundThread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    public void stopBackgroundThread()
    {
        backgroundThread.quitSafely();
        try
        {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        startBackgroundThread();
    }

    @Override
    public void onPause()
    {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback()
    {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session)
        {
            cameraCaptureSession = session;
            try
            {
                CaptureRequest captureRequest = createCaptureRequest();
                if (captureRequest != null)
                {
                    session.setRepeatingRequest(captureRequest, null, backgroundHandler);
                }
                else
                {
                    LOGGER.e("captureRequest is null");
                }
            }
            catch (CameraAccessException e)
            {
                LOGGER.e("onConfigured " + e.toString());
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session)
        {
            LOGGER.e("onConfigureFailed");
        }
    };

    private CaptureRequest createCaptureRequest()
    {
        if (null == cameraDevice) return null;
        try
        {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.addTarget(imageReader.getSurface());

            return builder.build();
        }
        catch (CameraAccessException e)
        {
            LOGGER.e(e.getMessage());
            return null;
        }
    }

    private static Size chooseOptimalSize(final Size[] choices, final int width, final int height)
    {
        final int minSize = Math.max(Math.min(width, height), MINIMUM_PREVIEW_SIZE);
        final Size desiredSize = new Size(width, height);

        // Collect the supported resolutions that are at least as big as the preview Surface
        boolean exactSizeFound = false;
        final List<Size> bigEnough = new ArrayList<Size>();
        final List<Size> tooSmall = new ArrayList<Size>();
        for (final Size option : choices)
        {
            if (option.equals(desiredSize))
            {
                // Set the size but don't return yet so that remaining sizes will still be logged.
                exactSizeFound = true;
            }

            if (option.getHeight() >= minSize && option.getWidth() >= minSize)
            {
                bigEnough.add(option);
            }
            else
            {
                tooSmall.add(option);
            }
        }

        LOGGER.i("Desired size: " + desiredSize + ", min size: " + minSize + "x" + minSize);
        LOGGER.i("Valid preview sizes: [" + TextUtils.join(", ", bigEnough) + "]");
        LOGGER.i("Rejected preview sizes: [" + TextUtils.join(", ", tooSmall) + "]");

        if (exactSizeFound)
        {
            LOGGER.i("Exact size match found.");
            return desiredSize;
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0)
        {
            final Size chosenSize = Collections.min(bigEnough, new CompareSizesByArea());
            LOGGER.i("Chosen size: " + chosenSize.getWidth() + "x" + chosenSize.getHeight());
            return chosenSize;
        }
        else
        {
            LOGGER.e("Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    public CameraRenderer getRenderer()
    {
        return renderer;
    }

    public void setSession(Session session) { this.session = session; }
    public Session getSession() { return this.session; }
    public Integer getSensorOrientation() { return this.sensorOrientation; }
    public void setSensorOrientation(int orientation) { this.sensorOrientation = orientation; }

    static class CompareSizesByArea implements Comparator<Size>
    {
        @Override
        public int compare(final Size lhs, final Size rhs)
        {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum(
                    (long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }
}
