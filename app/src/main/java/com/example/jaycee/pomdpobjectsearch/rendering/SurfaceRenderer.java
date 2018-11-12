package com.example.jaycee.pomdpobjectsearch.rendering;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.WindowManager;

import com.example.jaycee.pomdpobjectsearch.ActivityCamera;
import com.example.jaycee.pomdpobjectsearch.CameraSurface;
import com.example.jaycee.pomdpobjectsearch.helpers.ClassHelpers;
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

    private Pose devicePose;
    private Anchor debugObjectAnchor;

    private BackgroundRenderer backgroundRenderer;
    private ObjectRenderer objectRenderer;
    private ObjectRenderer waypointRenderer;

    private ARObject debugObject;

    private int width, height;

    private int scannerWidth, scannerHeight;
    private int scannerX, scannerY;

    private long timestamp;

    private final float[] anchorMatrix = new float[16];

    private boolean drawObjects = false;
    private boolean drawWaypoint = false;
    private boolean viewportChanged = false;
    private boolean rendererReady = false;

    public SurfaceRenderer(Context context, CameraSurface surfaceView)
    {
        this.context = context;
        this.surfaceView = surfaceView;

        this.scannerWidth = surfaceView.getWidth();
        this.scannerHeight = surfaceView.getHeight();
        this.scannerX = 0;
        this.scannerY = 0;

        init();
    }

    public void init()
    {
        backgroundRenderer = new BackgroundRenderer(scannerX, scannerY, scannerWidth, scannerHeight);
        objectRenderer = new ObjectRenderer();
        waypointRenderer = new ObjectRenderer();
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
            waypointRenderer.createOnGlThread(context, "models/andy.obj", "models/andy.png");

            objectRenderer.setMaterialProperties(0.f, 2.f, 0.5f, 6.f);
            waypointRenderer.setMaterialProperties(0.f, 2.f, 0.5f, 6.f);
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
        debugObject = new ARObject(5, 5, "Centre");

        // If surface changed, renderer is not ready
        rendererReady = false;
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
            Frame frame = session.update();
            Camera camera = frame.getCamera();
            devicePose = frame.getAndroidSensorPose();
            timestamp = frame.getTimestamp();

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
            if(camera.getTrackingState() == TrackingState.TRACKING)
            {
                if(drawObjects)
                {
                    // Get pointing vector
                    ClassHelpers.mVector currentPointingVector = new ClassHelpers.mVector(devicePose.getTranslation());
                    ClassHelpers.mQuaternion currentPhoneRotation = new ClassHelpers.mQuaternion(devicePose.getRotationQuaternion());
                    currentPointingVector.rotateByQuaternion(currentPhoneRotation);
                    currentPointingVector.y *= -1;
                    currentPointingVector.x *= -1;

                    // Construct arrow pose
                    currentPointingVector.denormalise();
                    Pose indicatorPose = new Pose(currentPointingVector.asFloat(), devicePose.getRotationQuaternion());

                    indicatorPose.toMatrix(anchorMatrix, 0);
                    objectRenderer.updateModelMatrix(anchorMatrix, scaleFactor);
                    objectRenderer.draw(viewMatrix, projectionMatrix, colourCorrectionRgba);
                }

                if(drawWaypoint)
                {
/*                    Pose waypointPose = ((ActivityCamera)context).getWaypointAnchor().getPose();

                    // Draw the waypoints as an Andyman
                    waypointPose.toMatrix(anchorMatrix, 0);
                    waypointRenderer.updateModelMatrix(anchorMatrix, scaleFactor);
                    waypointRenderer.draw(viewMatrix, projectionMatrix, colourCorrectionRgba);*/
                }
            }
            else
            {
                Log.d(TAG, "Camera not tracking or target not set. ");
            }

            // Indicate renderer is ready after first frame is drawn
            rendererReady = true;
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
            return IntBuffer.allocate(width*height);
        }
    }

    public void toggleDrawObjects()
    {
        if(debugObjectAnchor != null)
        {
            debugObjectAnchor.detach();
        }
        debugObject.getRotatedObject(devicePose);
        debugObjectAnchor = surfaceView.getSession().createAnchor(debugObject.getRotatedPose());

        this.drawObjects = !this.drawObjects;
    }

    public boolean isRendererReady() { return this.rendererReady; }
    public void setDrawWaypoint(boolean drawWaypoint) { this.drawWaypoint = drawWaypoint; }

    public Session getSession() { return surfaceView.getSession(); }
    public Pose getDevicePose() { return this.devicePose; }
    public long getTimestamp() { return this.timestamp; }

    public int getWidth() { return this.width; }
    public int getHeight() { return this.height; }
}
