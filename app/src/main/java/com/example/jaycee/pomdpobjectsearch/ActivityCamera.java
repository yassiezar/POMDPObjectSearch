package com.example.jaycee.pomdpobjectsearch;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Messenger;
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
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ActivityCamera extends AppCompatActivity implements GLSurfaceView.Renderer
{
    private static final String TAG = ActivityCamera.class.getSimpleName();

    private static final int CAMERA_PERMISSION_CODE = 0;
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;

    private static final int T_COMPUTER_MONITOR = 0;
    private static final int T_DESK = 1;
    private static final int T_WINDOW = 2;
    private static final int T_KETTLE = 3;
    private static final int T_SINK = 4;
    private static final int T_TOILET = 5;
    private static final int T_HAND_DRYER = 6;

    private Session session;

    private GLSurfaceView surfaceView;
    private DrawerLayout drawerLayout;

    private final ClassRendererBackground backgroundRenderer = new ClassRendererBackground();
    private final SnackbarHelper snackbarHelper = new SnackbarHelper(this);

    BarcodeDetector detector;

    private RunnableSoundGenerator runnableSoundGenerator = new RunnableSoundGenerator(this);

    private boolean requestARCoreInstall = true;
    private boolean viewportChanged = false;

    private int width, height;

    private HandlerMDPIntentService handlerMdpIntentService = new HandlerMDPIntentService(this);

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
        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        surfaceView.setRenderer(this);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        drawerLayout = findViewById(R.id.layout_drawer_objects);
        NavigationView navigationView = findViewById(R.id.navigation_view_objects);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener()
        {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item)
            {
                switch (item.getItemId())
                {
                    case R.id.item_object_computer_monitor:
                        runnableSoundGenerator.setTarget(T_COMPUTER_MONITOR);
                        break;
                    case R.id.item_object_desk:
                        runnableSoundGenerator.setTarget(T_DESK);
                        break;
                    case R.id.item_object_window:
                        runnableSoundGenerator.setTarget(T_WINDOW);
                        break;
                    case R.id.item_object_kettle:
                        runnableSoundGenerator.setTarget(T_KETTLE);
                        break;
                    case R.id.item_object_sink:
                        runnableSoundGenerator.setTarget(T_SINK);
                        break;
                    case R.id.item_object_toilet:
                        runnableSoundGenerator.setTarget(T_TOILET);
                        break;
                    case R.id.item_object_hand_dryer:
                        runnableSoundGenerator.setTarget(T_HAND_DRYER);
                        break;
                }

                item.setCheckable(true);

                drawerLayout.closeDrawers();

                return true;
            }
        });

        detector = new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.ALL_FORMATS).build();
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

        surfaceView.onResume();

        boolean initSound = JNIBridge.initSound();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if(session != null)
        {
            surfaceView.onPause();
            session.pause();
        }

        boolean killSound = JNIBridge.killSound();
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig)
    {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        try
        {
            backgroundRenderer.createOnGlThread(this);
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
            Frame frame = session.update();
            Camera camera = frame.getCamera();

            backgroundRenderer.draw(frame);

            Bitmap bitmap = backgroundRenderer.getBitmap(width, height);

            com.google.android.gms.vision.Frame bitmapFrame = new com.google.android.gms.vision.Frame.Builder().setBitmap(bitmap).build();
            SparseArray<Barcode> barcodes = detector.detect(bitmapFrame);

            if(barcodes.size() > 0)
            {
                int key = barcodes.keyAt(0);
                Log.d(TAG, "Barcode found: " + barcodes.get(key).displayValue);
            }

            if(camera.getTrackingState() == TrackingState.TRACKING &&
                    (handlerMdpIntentService.getMdpLearned() || runnableSoundGenerator.isTargetSet()))
            {
                runnableSoundGenerator.update(camera, session);
            }
            else
            {
                Log.w(TAG, "Camera not tracking or target is not set. ");
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
        return ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION)== PackageManager.PERMISSION_GRANTED;
    }

    public void requestCameraPermission()
    {
        ActivityCompat.requestPermissions(this, new String[] {CAMERA_PERMISSION}, CAMERA_PERMISSION_CODE);
    }
}