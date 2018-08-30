package com.example.jaycee.pomdpobjectsearch;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
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
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.example.jaycee.pomdpobjectsearch.rendering.ClassRendererBackground;
import com.example.jaycee.pomdpobjectsearch.rendering.ClassRendererObject;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import java.io.IOException;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ActivityCamera extends AppCompatActivity
{
    private static final String TAG = ActivityCamera.class.getSimpleName();

    private static final int CAMERA_PERMISSION_CODE = 0;
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;

    /* TODO: Make new barcodes to correspond with new values */
    private static final int T_COMPUTER_MONITOR = 1;
    private static final int T_COMPUTER_MOUSE = 3;
    private static final int T_COMPUTER_KEYBOARD = 2;
    private static final int T_DESK = 4;
    private static final int T_LAPTOP = 5;
    private static final int T_MUG = 6;
    private static final int T_OFFICE_SUPPLIES = 7;
    private static final int T_WINDOW = 8;

    private Session session;

    private MyGLSurfaceView surfaceView;
    // private View scannerView;
    private DrawerLayout drawerLayout;

    // private BarcodeDetector detector;

    // private RunnableSoundGenerator runnableSoundGenerator;

    // private ArrayList<ARObject> objectList;

    private boolean requestARCoreInstall = true;
    // private boolean drawObjects = false;

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
        surfaceView.setBarcodeView(findViewById(R.id.view_scanner));
        /*surfaceView.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                switch(event.getAction())
                {
                    case (MotionEvent.ACTION_DOWN):
                    {
                        if(!drawObjects)
                        {
                            Log.i(TAG, "Pressed");
                            try
                            {
                                Pose devicePose = frame.getAndroidSensorPose();

                                for (ARObject object : objectList)
                                {
                                    object.getRotatedObject(devicePose);
                                    session.createAnchor(object.getRotatedPose());
                                }
                            }
                            catch (Exception e)
                            {
                                Log.e(TAG, "Exception on adding AR anchors: " + e);
                            }
                            drawObjects = true;
                        }
                        else drawObjects = false;
                    }
                }
                return true;
            }
        });*/

        drawerLayout = findViewById(R.id.layout_drawer_objects);
        NavigationView navigationView = findViewById(R.id.navigation_view_objects);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener()
        {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item)
            {
                switch (item.getItemId())
                {
                    case R.id.item_object_mug:
                        surfaceView.setTarget(T_MUG);
                        break;
                    case R.id.item_object_laptop:
                        surfaceView.setTarget(T_LAPTOP);
                        break;
                    case R.id.item_object_desk:
                        surfaceView.setTarget(T_DESK);
                        break;
                    case R.id.item_object_office_supplies:
                        surfaceView.setTarget(T_OFFICE_SUPPLIES);
                        break;
                    case R.id.item_object_keyboard:
                        surfaceView.setTarget(T_COMPUTER_KEYBOARD);
                        break;
                    case R.id.item_object_monitor:
                        surfaceView.setTarget(T_COMPUTER_MONITOR);
                        break;
                    case R.id.item_object_mouse:
                        surfaceView.setTarget(T_COMPUTER_MOUSE);
                        break;
                    case R.id.item_object_window:
                        surfaceView.setTarget(T_WINDOW);
                        break;
                }

                surfaceView.setOffsetPose();
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
                surfaceView.setSession(session);
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
}
