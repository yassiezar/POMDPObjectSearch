package com.example.jaycee.pomdpobjectsearch;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.example.jaycee.pomdpobjectsearch.rendering.ARObject;
import com.example.jaycee.pomdpobjectsearch.rendering.SurfaceRenderer;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import java.util.ArrayList;

public class ActivityCamera extends AppCompatActivity
{
    private static final String TAG = ActivityCamera.class.getSimpleName();

    private static final int CAMERA_PERMISSION_CODE = 0;
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;

    private static final int T_COMPUTER_MONITOR = 1;
    private static final int T_COMPUTER_MOUSE = 3;
    private static final int T_COMPUTER_KEYBOARD = 2;
    private static final int T_DESK = 4;
    private static final int T_LAPTOP = 5;
    private static final int T_MUG = 6;
    private static final int T_OFFICE_SUPPLIES = 7;
    private static final int T_WINDOW = 8;

    private Session session;
    private Frame frame;

    private CameraSurface surfaceView;
    private DrawerLayout drawerLayout;

    private SoundGenerator soundGenerator;
    private BarcodeScanner barcodeScanner;

    private boolean requestARCoreInstall = true;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);

        surfaceView = findViewById(R.id.surfaceview);

        drawerLayout = findViewById(R.id.layout_drawer_objects);
        NavigationView navigationView = findViewById(R.id.navigation_view_objects);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener()
        {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item)
            {
                int code = barcodeScanner.getCode();
                switch (item.getItemId())
                {
                    case R.id.item_object_mug:
                        soundGenerator.setTarget(T_MUG, code);
                        break;
                    case R.id.item_object_laptop:
                        soundGenerator.setTarget(T_LAPTOP, code);
                        break;
                    case R.id.item_object_desk:
                        soundGenerator.setTarget(T_DESK, code);
                        break;
                    case R.id.item_object_office_supplies:
                        soundGenerator.setTarget(T_OFFICE_SUPPLIES, code);
                        break;
                    case R.id.item_object_keyboard:
                        soundGenerator.setTarget(T_COMPUTER_KEYBOARD, code);
                        break;
                    case R.id.item_object_monitor:
                        soundGenerator.setTarget(T_COMPUTER_MONITOR, code);
                        break;
                    case R.id.item_object_mouse:
                        soundGenerator.setTarget(T_COMPUTER_MOUSE, code);
                        break;
                    case R.id.item_object_window:
                        soundGenerator.setTarget(T_WINDOW, code);
                        break;
                }

                soundGenerator.setOffsetPose(frame.getAndroidSensorPose());
                item.setCheckable(true);

                drawerLayout.closeDrawers();

                return true;
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if(session == null)
        {
            try
            {
                switch(ArCoreApk.getInstance().requestInstall(this, requestARCoreInstall))
                {
                    case INSTALLED:
                        break;
                    case INSTALL_REQUESTED:
                        requestARCoreInstall = false;
                        return;
                }

                if(!hasCameraPermission())
                {
                    requestCameraPermission();
                    return;
                }
                session = new Session(this);

                // Set config settings
                Config conf = new Config(session);
                conf.setFocusMode(Config.FocusMode.AUTO);
                session.configure(conf);
            }
            catch(UnavailableUserDeclinedInstallationException | UnavailableArcoreNotInstalledException  e)
            {
                Log.e(TAG, "Please install ARCore.");
                return;
            }
            catch(UnavailableDeviceNotCompatibleException e)
            {
                Log.e(TAG, "This device does not support ARCore.");
                return;
            }
            catch(UnavailableApkTooOldException e)
            {
                Log.e(TAG, "Please update the app.");
                return;
            }
            catch(UnavailableSdkTooOldException e)
            {
                Log.e(TAG, "Please update ARCore. ");
                return;
            }
            catch(Exception e)
            {
                Log.e(TAG, "Failed to create AR session.");
            }
        }

        try
        {
            session.resume();
        }
        catch(CameraNotAvailableException e)
        {
            session = null;
            Log.e(TAG, "Camera not available. Please restart app.");
            return;
        }

        surfaceView.setSession(session);
        surfaceView.onResume();

        soundGenerator = new SoundGenerator(this, surfaceView.getRenderer());
        // soundGenerator.run();

        if(!JNIBridge.initSound())
        {
            Log.e(TAG, "OpenAL init error");
        }
    }

    @Override
    protected void onPause()
    {
        if(barcodeScanner != null)
        {
            barcodeScanner.stop();
            barcodeScanner = null;
        }

        if(soundGenerator != null)
        {
            soundGenerator.stop();
            soundGenerator = null;
        }

        if(session != null)
        {
            surfaceView.onPause();
            session.pause();
        }

        if(!JNIBridge.killSound())
        {
            Log.e(TAG, "OpenAL kill error");
        }

        super.onPause();
    }

/*    @Override
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
                int displayRotation = this.getSystemService(WindowManager.class).getDefaultDisplay().getRotation();
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
            frame = session.update();
            Camera camera = frame.getCamera();

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

            if(camera.getTrackingState() == TrackingState.TRACKING && drawObjects)
            {
                for(ARObject object : objectList)
                {
                    object.getRotatedPose().toMatrix(anchorMatrix, 0);
                    objectRenderer.updateModelMatrix(anchorMatrix, scaleFactor);
                    objectRenderer.draw(viewMatrix, projectionMatrix, colourCorrectionRgba);
                }
            }

            soundGenerator.updatePhonePose(camera, session);
            if(soundGenerator.isTargetSet())
            {
                soundGenerator.setObservation(scanBarcode());

                if(camera.getTrackingState() == TrackingState.TRACKING)
                {
                    soundGenerator.getWaypointAnchor().getPose().toMatrix(anchorMatrix, 0);

                    objectRenderer.updateModelMatrix(anchorMatrix, scaleFactor);
                    objectRenderer.draw(viewMatrix, projectionMatrix, colourCorrectionRgba);
                }
                else
                {
                    Log.d(TAG, "No target set.");
                }
            }

            if(camera.getTrackingState() == TrackingState.TRACKING &&
                    soundGenerator.isTargetSet() &&
                    !soundGenerator.isTargetFound())
            {
                soundGenerator.update();
            }
            else
            {
                Log.d(TAG, "Camera not tracking or target not set. ");
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
    }*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean hasCameraPermission()
    {
        return ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestCameraPermission()
    {
        ActivityCompat.requestPermissions(this, new String[] {CAMERA_PERMISSION}, CAMERA_PERMISSION_CODE);
    }

    public void stopBarcodeScanner()
    {
        if(barcodeScanner != null)
        {
            barcodeScanner.stop();
            barcodeScanner = null;
        }
    }

    public void startBarcodeScanner()
    {
        barcodeScanner = new BarcodeScanner(this, 525, 525, surfaceView.getRenderer());
        barcodeScanner.run();
    }
}
