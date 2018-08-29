package com.example.jaycee.pomdpobjectsearch;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;

import com.example.jaycee.pomdpobjectsearch.rendering.ClassRendererBackground;
import com.example.jaycee.pomdpobjectsearch.rendering.ClassRendererObject;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;

import java.io.IOException;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLSurfaceView extends GLSurfaceView implements SurfaceHolder.Callback
{
    private static final String TAG = MyGLSurfaceView.class.getSimpleName();

    private Camera camera;
    private View barcodeView;

    private Context context;

    private RunnableSoundGenerator runnableSoundGenerator;

    private GLRenderer renderer;

    public MyGLSurfaceView(Context context)
    {
        super(context);
        renderer = new GLRenderer();
        this.context = context;
        runnableSoundGenerator = new RunnableSoundGenerator(context);

        getHolder().addCallback(this);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setFocusable(true);
        setPreserveEGLContextOnPause(true);
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    public MyGLSurfaceView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        renderer = new GLRenderer();
        this.context = context;
        runnableSoundGenerator = new RunnableSoundGenerator(context);

        getHolder().addCallback(this);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setFocusable(true);
        setPreserveEGLContextOnPause(true);
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        try
        {
            camera = Camera.open();
            camera.setPreviewDisplay(holder);
        }
        catch(Exception e)
        {
            camera.release();
            camera = null;
            Log.e(TAG, "failed to open Camera: "+ e);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (camera != null)
        {
            // Call stopPreview() to stop updating the preview surface.
            camera.stopPreview();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        if(camera != null)
        {
            Camera.Parameters parameters = camera.getParameters();

            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

            List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
            Camera.Size previewSize = previewSizes.get(0);
            parameters.setPreviewSize(previewSize.width, previewSize.height);
            requestLayout();

            camera.setParameters(parameters);
            camera.setDisplayOrientation(90);

            camera.startPreview();
        }
    }

    public int scanBarcode() { return renderer.scanBarcode(); }
    public void setSession(Session session) { renderer.setSession(session); }
    public void setBarcodeView(View barcodeView) { this.barcodeView = barcodeView; }
    public void setTarget(int target) { runnableSoundGenerator.setTarget(target, renderer.scanBarcode()); }
    public void setOffsetPose(Pose pose) { runnableSoundGenerator.setOffsetPose(pose); }

    class GLRenderer implements GLSurfaceView.Renderer
    {
        private final String TAG = GLRenderer.class.getSimpleName();
        private final int O_NOTHING = 0;

        private Session session;

        private final ClassRendererBackground backgroundRenderer = new ClassRendererBackground();
        private final ClassRendererObject objectRenderer = new ClassRendererObject();

        private final float[] anchorMatrix = new float[16];

        private boolean viewportChanged = false;

        private int width, height;

        GLRenderer()
        {
        }

        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig)
        {
            GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

            try
            {
                backgroundRenderer.createOnGlThread(context);
                objectRenderer.createOnGlThread(context, "models/andy.obj", "models/andy.png");
                objectRenderer.setMaterialProperties(0.f, 2.f, 0.5f, 6.f);
            }
            catch(IOException e)
            {
                Log.e(TAG, "Failed to read asset file. ", e);
            }
        }

        @Override
        public void onSurfaceChanged(GL10 gl10, int width, int height)
        {
            viewportChanged = true;
            this.width = width;
            this.height = height;
            Log.i(TAG, "MAde it here");
            GLES20.glViewport(0, 0, width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl10)
        {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            if(session == null)
            {
                return;
            }

            if(viewportChanged)
            {
                try
                {
                    viewportChanged = false;
                    int displayRotation = context.getSystemService(WindowManager.class).getDefaultDisplay().getRotation();
                    session.setDisplayGeometry(displayRotation, width, height);
                }
                catch(NullPointerException e)
                {
                    Log.e(TAG, "defaultDisplay exception: " + e);
                }
            }

            session.setCameraTextureName(backgroundRenderer.getTextureId());
            try
            {
                Frame frame = session.update();
                com.google.ar.core.Camera camera = frame.getCamera();

                backgroundRenderer.draw(frame);

                float[] projectionMatrix = new float[16];
                camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f);

                float[] viewMatrix = new float[16];
                camera.getViewMatrix(viewMatrix, 0);

                // Compute lighting from average intensity of the image.
                // The first three components are color scaling factors.
                // The last one is the average pixel intensity in gamma space.
                final float[] colourCorrectionRgba = new float[4];
                frame.getLightEstimate().getColorCorrection(colourCorrectionRgba, 0);

                float scaleFactor = 1.f;

            /*if(camera.getTrackingState() == TrackingState.TRACKING && drawObjects)
            {
                for(ARObject object : objectList)
                {
                    object.getRotatedPose().toMatrix(anchorMatrix, 0);
                    objectRenderer.updateModelMatrix(anchorMatrix, scaleFactor);
                    objectRenderer.draw(viewMatrix, projectionMatrix, colourCorrectionRgba);
                }
            }*/

                runnableSoundGenerator.updatePhonePose(camera, session);
                if(runnableSoundGenerator.isTargetSet())
                {
                    runnableSoundGenerator.setObservation(scanBarcode());

                    if(camera.getTrackingState() == TrackingState.TRACKING)
                    {
                        runnableSoundGenerator.getWaypointAnchor().getPose().toMatrix(anchorMatrix, 0);

                        objectRenderer.updateModelMatrix(anchorMatrix, scaleFactor);
                        objectRenderer.draw(viewMatrix, projectionMatrix, colourCorrectionRgba);
                    }
                    else
                    {
                        Log.w(TAG, "No target set.");
                    }
                }

                if(camera.getTrackingState() == TrackingState.TRACKING &&
                        runnableSoundGenerator.isTargetSet() &&
                        !runnableSoundGenerator.isTargetFound())
                {
                    runnableSoundGenerator.setTimestamp(frame.getTimestamp());
                    runnableSoundGenerator.update();
                }
                else
                {
                    Log.w(TAG, "Camera not tracking or target not set       . ");
                }

            }
            catch(CameraNotAvailableException e)
            {
                Log.e(TAG, "Camera not available: " + e);
            }
            catch(Throwable t)
            {
                Log.e(TAG, "Exception on OpenGL thread: " + t);
            }
        }

        int scanBarcode()
        {
            View scannerView = barcodeView;

            BarcodeDetector detector = new BarcodeDetector.Builder(context).setBarcodeFormats(Barcode.ALL_FORMATS).build();

            Bitmap bitmap = backgroundRenderer.getBitmap(width, height);

            com.google.android.gms.vision.Frame bitmapFrame = new com.google.android.gms.vision.Frame.Builder().setBitmap(bitmap).build();
            SparseArray<Barcode> barcodes = detector.detect(bitmapFrame);

            int val = O_NOTHING;

            if(barcodes.size() > 0)
            {
                for(int i = 0; i < barcodes.size(); i ++)
                {
                    int key = barcodes.keyAt(i);

                    Rect scannerArea = new Rect(scannerView.getLeft(), scannerView.getTop(), scannerView.getRight(), scannerView.getBottom());
                    if(scannerArea.contains(barcodes.get(key).getBoundingBox()))
                    {
                        val = Integer.parseInt(barcodes.get(key).displayValue);
                        Log.d(TAG, "Object found: " + val);
                    }
                }
            }

            return val;
        }

        public void setSession(Session session) { this.session = session; }
    }
}
