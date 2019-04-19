package com.example.jaycee.pomdpobjectsearch;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
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
import android.widget.Toast;

import com.example.jaycee.pomdpobjectsearch.helpers.ImageConverter;
import com.example.jaycee.pomdpobjectsearch.helpers.ImageUtils;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotTrackingException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import java.io.IOException;
import java.util.List;

public class ActivityCamera extends AppCompatActivity implements NewFrameHandler, NewWaypointHandler
{
    private static final String TAG = ActivityCamera.class.getSimpleName();

    private static final int CAMERA_PERMISSION_CODE = 0;
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;

    private static final String TF_MODEL_FILE = "mobilenet/office_detect.tflite";
    private static final String TF_LABELS_FILE = "file:///android_asset/mobilenet/office_labels_list.txt";
    private static final int TF_INPUT_SIZE = 300;
    private static final boolean TF_IS_QUANTISED = false;

    private static final boolean MAINTAIN_ASPECT_RATIO = false;

    private static final int O_NOTHING = 0;

    private static final int T_BACKPACK = 1;
    private static final int T_BOOK = 2;
    private static final int T_BOOKCASE = 3;
    private static final int T_CHAIR = 4;
    private static final int T_DESK = 5;
    private static final int T_DOOR = 6;
    private static final int T_COMPUTER_KEYBOARD = 7;
    private static final int T_LAMP = 8;
    private static final int T_LAPTOP = 9;
    private static final int T_LIGHT_SWITCH = 10;
    private static final int T_COMPUTER_MONITOR = 11;
    private static final int T_COMPUTER_MOUSE = 12;
    private static final int T_MUG = 13;
    private static final int T_PLANT = 14;
    private static final int T_TELEPHONE = 15;
    private static final int T_WHITEBOARD = 16;
    private static final int T_WINDOW = 17;

    private Session session;

    private CameraSurface surfaceView;
    private DrawerLayout drawerLayout;
    private CentreView centreView;

    private SoundGenerator soundGenerator;
    private ObjectClassifier detector;

    private Handler backgroundHandler;
    private HandlerThread backgroundHandlerThread;

    private ImageConverter imageConverter;

    private Bitmap rgbFrameBitmap;
    private Bitmap croppedBitmap;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private boolean requestARCoreInstall = true;
    private boolean processingFrame = false;

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

        centreView = findViewById(R.id.centre_view);

        drawerLayout = findViewById(R.id.layout_drawer_objects);
        NavigationView navigationView = findViewById(R.id.navigation_view_objects);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener()
        {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item)
            {
                int target = 0;
                switch (item.getItemId())
                {
                    case R.id.item_object_backpack:
                        target = T_MUG;
                        break;
                    case R.id.item_object_book:
                        target = T_BOOK;
                        break;
                    case R.id.item_object_bookcase:
                        target = T_BOOKCASE;
                        break;
                    case R.id.item_object_chair:
                        target = T_CHAIR;
                        break;
                    case R.id.item_object_desk:
                        target = T_DESK;
                        break;
                    case R.id.item_object_door:
                        target = T_DOOR;
                        break;
                    case R.id.item_object_keyboard:
                        target = T_COMPUTER_KEYBOARD;
                        break;
                    case R.id.item_object_lamp:
                        target = T_LAMP;
                        break;
                    case R.id.item_object_laptop:
                        target = T_LAPTOP;
                        break;
                    case R.id.item_object_lightswitch:
                        target = T_MUG;
                        break;
                    case R.id.item_object_monitor:
                        target = T_COMPUTER_MONITOR;
                        break;
                    case R.id.item_object_mouse:
                        target = T_COMPUTER_MOUSE;
                        break;
                    case R.id.item_object_mug:
                        target = T_COMPUTER_KEYBOARD;
                        break;
                    case R.id.item_object_plant:
                        target = T_PLANT;
                        break;
                    case R.id.item_object_telephone:
                        target = T_TELEPHONE;
                        break;
                    case R.id.item_object_whiteboard:
                        target = T_WHITEBOARD;
                        break;
                    case R.id.item_object_window:
                        target = T_WINDOW;
                        break;
                }

                try
                {
                    soundGenerator.setTarget(target);
                    item.setCheckable(true);
                }
                catch(NotTrackingException e)
                {
                    Log.e(TAG, "Not tracking: " + e);
                    Toast.makeText(ActivityCamera.this, "Camera not tracking", Toast.LENGTH_LONG).show();
                }

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

        try
        {
            surfaceView.setSession(session);
            surfaceView.onResume();
        }
        catch(Exception e)
        {
            Log.e(TAG, "SurfaceView init error: " + e);
        }

        try
        {
            detector = ObjectDetector.create(getAssets(), TF_MODEL_FILE, TF_LABELS_FILE, TF_INPUT_SIZE, TF_IS_QUANTISED);
        }
        catch(IOException e)
        {
            Log.e(TAG, "Object detector init error: Cannot read file " + e);
        }

        if(!JNIBridge.initSound())
        {
            Log.e(TAG, "OpenAL init error");
        }

        backgroundHandlerThread = new HandlerThread("InferenceThread");
        backgroundHandlerThread.start();
        backgroundHandler = new Handler(backgroundHandlerThread.getLooper());

        soundGenerator = new SoundGenerator(this);
        soundGenerator.setSession(session);
        soundGenerator.run();
    }

    @Override
    protected void onPause()
    {
        if(!isFinishing())
        {
            finish();
        }

        backgroundHandlerThread.quitSafely();
        try
        {
            backgroundHandlerThread.join();
            backgroundHandlerThread = null;
            backgroundHandler = null;
        }
        catch(InterruptedException e)
        {
            Log.e(TAG, "Exception onPause: " + e);
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

        if(detector != null)
        {
            detector.close();
        }

        if(!JNIBridge.killSound())
        {
            Log.e(TAG, "OpenAL kill error");
        }

        super.onPause();
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

    public Anchor getWaypointAnchor()
    {
        /* TODO: Handle nullpointer crash here */
        return soundGenerator.getWaypointAnchor();
    }

    @Override
    public void onNewFrame(final Frame frame)
    {
        Log.d(TAG, "New Frame");
        soundGenerator.setFrame(frame);

        if(!soundGenerator.isTargetSet())
        {
            return;
        }

        if(processingFrame)
        {
            return;
        }

        if(imageConverter == null)
        {
            Log.w(TAG, "Image converter not initialised");
            imageConverter = new ImageConverter(surfaceView.getRenderer().getWidth(), surfaceView.getRenderer().getHeight());
        }

        if(rgbFrameBitmap == null || croppedBitmap == null)
        {
            // TODO: Add compensation for other screen rotations
            int cropSize = TF_INPUT_SIZE;
            int sensorOrientation = 0;      // Assume 0deg rotation for now

            rgbFrameBitmap = Bitmap.createBitmap(surfaceView.getRenderer().getWidth(), surfaceView.getRenderer().getHeight(), Bitmap.Config.ARGB_8888);
            croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

            frameToCropTransform =
                    ImageUtils.getTransformationMatrix(
                            surfaceView.getRenderer().getWidth(), surfaceView.getRenderer().getHeight(),
                            cropSize, cropSize,
                            sensorOrientation, MAINTAIN_ASPECT_RATIO);

            cropToFrameTransform = new Matrix();
            frameToCropTransform.invert(cropToFrameTransform);
        }

        processingFrame = true;
        try
        {
            Log.d(TAG, "Processing new frame");

            // PERFORM DETECTION + INFERENCE
            rgbFrameBitmap.setPixels(imageConverter.getRgbBytes(frame.acquireCameraImage()), 0, surfaceView.getRenderer().getWidth(), 0, 0, surfaceView.getRenderer().getWidth(), surfaceView.getRenderer().getHeight());

            final Canvas canvas = new Canvas(croppedBitmap);
            canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);

            if(detector == null)
            {
                Log.w(TAG, "Detector not initialised.");
                processingFrame = false;
                return;
            }

            runInBackground(new Runnable()
            {
                @Override
                public void run()
                {
                    Log.d(TAG, "Detecting objects");
                    final List<ObjectClassifier.Recognition> results = detector.recogniseImage(croppedBitmap);
                    processingFrame = false;

                    for(ObjectClassifier.Recognition rec : results)
                    {
                        Log.d(TAG, rec.toString());
                    }
                }
            });
        }
        catch(NotYetAvailableException e)
        {
            Log.e(TAG, "Camera not yet ready: " + e);
        }
        processingFrame = false;
    }

    @Override
    public void onNewTimestamp(long timestamp)
    {
        soundGenerator.setTimestamp(timestamp);
    }

    @Override
    public void onTargetFound()
    {
        surfaceView.getRenderer().setDrawWaypoint(false);
    }

    @Override
    public void onNewWaypoint()
    {
        surfaceView.getRenderer().setDrawWaypoint(true);
    }

    public CentreView getCentreView()
    {
        return centreView;
    }

    public synchronized void runInBackground(final Runnable r)
    {
        if(backgroundHandler != null)
        {
            backgroundHandler.post(r);
        }
    }
}
