package com.example.jaycee.pomdpobjectsearch.rendering;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.WindowManager;

import com.example.jaycee.pomdpobjectsearch.CameraSurface;
import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;

import java.io.IOException;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class SurfaceRenderer implements GLSurfaceView.Renderer
{
    private static final String TAG = SurfaceRenderer.class.getSimpleName();

    private Context context;
    private CameraSurface surfaceView;

    private Frame frame;
    private Camera camera;
    private Anchor debugObjectAnchor;

    private BackgroundRenderer backgroundRenderer;
    private ObjectRenderer objectRenderer;

    private ARObject debugObject;

    private int width, height;

    private int scannerWidth, scannerHeight;
    private int scannerX, scannerY;

    private final float[] anchorMatrix = new float[16];

    private boolean drawObjects = false;
    private boolean viewportChanged = false;

    public SurfaceRenderer(Context context, CameraSurface surfaceView)
    {
        this.context = context;
        this.surfaceView = surfaceView;

        this.scannerWidth = 525;
        this.scannerHeight = 525;
        this.scannerX = 450;
        this.scannerY = 1017;

        init();
    }

    public void init()
    {
        backgroundRenderer = new BackgroundRenderer(scannerX, scannerY, scannerWidth, scannerHeight);
        objectRenderer = new ObjectRenderer();
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig)
    {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        Log.i(TAG, "Surface created");
        try
        {
            backgroundRenderer.createOnGlThread(context);
            objectRenderer.createOnGlThread(context, "models/arrow/Arrow.obj", "models/arrow/Arrow_S.tga");
            // objectRenderer.createOnGlThread(this, "models/andy.obj", "models/andy.png");
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
        Log.i(TAG, "Surface changed");
        viewportChanged = true;
        this.width = width;
        this.height = height;
        GLES20.glViewport(0, 0, width, height);

        // Create AR debug object
        debugObject = new ARObject(6, 6, "Centre");
    }

    @Override
    public void onDrawFrame(GL10 gl10)
    {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        Session session = surfaceView.getSession();
        if(session == null)
        {
            Log.w(TAG, "No session available for draw.");
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
                Log.e(TAG, "Default display exception: " + e);
            }
        }

        session.setCameraTextureName(backgroundRenderer.getTextureId());
        try
        {
            frame = session.update();
            camera = frame.getCamera();

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

            // Update the debug object
            if(camera.getTrackingState() == TrackingState.TRACKING && drawObjects)
            {
                /* TODO: Detach old anchors */
/*                if(debugObjectAnchor != null)
                {
                    debugObjectAnchor.detach();
                }*/
                Pose devicePose = frame.getAndroidSensorPose();
                debugObject.getRotatedObject(devicePose);
                session.createAnchor(debugObject.getRotatedPose());

                debugObject.getRotatedPose().toMatrix(anchorMatrix, 0);
                objectRenderer.updateModelMatrix(anchorMatrix, scaleFactor);
                objectRenderer.draw(viewMatrix, projectionMatrix, colourCorrectionRgba);
            }
        }
        catch(CameraNotAvailableException e)
        {
            Log.e(TAG, "Camera not available: " + e);
        }
        catch(Throwable t)
        {
            Log.e(TAG, "Exception on the GL Thread: " + t);
        }
    }

    public IntBuffer getCurrentFrameBuffer()
    {
        try
        {
            return backgroundRenderer.getCurrentFrameBuffer().duplicate();
        }
        catch(NullPointerException e)
        {
            Log.e(TAG, "Frame buffer not yet initialised: " + e);
            return IntBuffer.allocate(scannerWidth*scannerHeight);
        }
    }

    public void toggleDrawObjects() { this.drawObjects = !this.drawObjects; }

    public int getWidth() { return this.width; }
    public int getHeight() { return this.height; }
}
