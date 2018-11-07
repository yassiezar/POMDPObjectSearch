package com.example.jaycee.pomdpobjectsearch;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
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
import android.util.Size;
import android.view.MenuItem;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ActivityCamera extends ActivityCameraBase implements ImageReader.OnImageAvailableListener, FrameHandler
{
    private static final String TAG = ActivityCamera.class.getSimpleName();

    private Size DESIRED_PREVIEW_SIZE = new Size(1440, 2560);

/*    private static final int O_NOTHING = 0;

    private static final int T_COMPUTER_MONITOR = 1;
    private static final int T_COMPUTER_MOUSE = 3;
    private static final int T_COMPUTER_KEYBOARD = 2;
    private static final int T_DESK = 4;
    private static final int T_MUG = 6;
    private static final int T_OFFICE_SUPPLIES = 7;
    private static final int T_WINDOW = 8;

    private Session session;*/

    private CameraSurface surfaceView;

    private Integer sensorOrientation;

    private Bitmap rgbFrameBitmap;

    // private BoundingBoxView boundingBoxView; //to write bounding box of the found object

    // private DrawerLayout drawerLayout;
    // private CentreView centreView;

    // private SoundGenerator soundGenerator;
    // private ObjectDetector objectDetector;

    // private boolean requestARCoreInstall = true;

    String cfgFilePath;
    String weightFilepat;
    float confidence_threshold = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        surfaceView = findViewById(R.id.surfaceview);

        cfgFilePath = getPath(".cfg", this);
        weightFilepat = getPath(".weights", this);

        // boundingBoxView = findViewById(R.id.bounding);

        // centreView = findViewById(R.id.centre_view);

        // drawerLayout = findViewById(R.id.layout_drawer_objects);

/*        NavigationView navigationView = findViewById(R.id.navigation_view_objects);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener()
        {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item)
            {
                int target = 0;
                switch (item.getItemId())
                {
                    case R.id.item_object_mug:
                        target = T_MUG;
                        break;
                    case R.id.item_object_desk:
                        target = T_DESK;
                        break;
                    case R.id.item_object_office_supplies:
                        target = T_OFFICE_SUPPLIES;
                        break;
                    case R.id.item_object_keyboard:
                        target = T_COMPUTER_KEYBOARD;
                        break;
                    case R.id.item_object_monitor:
                        target = T_COMPUTER_MONITOR;
                        break;
                    case R.id.item_object_mouse:
                        target = T_COMPUTER_MOUSE;
                        break;
                    case R.id.item_object_window:
                        target = T_WINDOW;
                        break;
                }

                soundGenerator.setTarget(target);
                soundGenerator.markOffsetPose();
                item.setCheckable(true);

                drawerLayout.closeDrawers();

                return true;
            }
        }); */
    }

    @Override
    public void onResume()
    {
        super.onResume();

/*        if(session == null)
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
        }*/

        // surfaceView.setSession(session);
        surfaceView.onResume();
        JNIBridge.create(cfgFilePath, weightFilepat, confidence_threshold);

/*        if(!JNIBridge.initSound())
        {
            Log.e(TAG, "OpenAL init error");
        }*/

        // soundGenerator = new SoundGenerator(this, surfaceView.getRenderer());
        // soundGenerator.run();
    }

    @Override
    public void onPause()
    {
/*        if(objectDetector != null)
        {
            objectDetector.stop();
            objectDetector = null;
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
        }*/

        Log.i(TAG, "Activity onPause");
        surfaceView.onPause();
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
/*        switch (item.getItemId())
        {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }*/
        return super.onOptionsItemSelected(item);
    }

/*    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        // This overrides default action
        Log.d(TAG, "Screen orientation change");
        sensorOrientation = newConfig.orientation;
    }*/

/*    public void stopObjectDetector()
    {
        if(objectDetector != null)
        {
            objectDetector.stop();
            objectDetector = null;
        }
    }

    public void startObjectDetector()
    {
        objectDetector = new ObjectDetector(this, surfaceView.getWidth(), surfaceView.getHeight(), surfaceView.getRenderer(), boundingBoxView);
        objectDetector.run();
    }

    public int currentObjectDetector()
    {
        if(objectDetector != null)
        {
            return objectDetector.getCode();
        }

        return O_NOTHING;
    }

    public Anchor getWaypointAnchor()
    {
        return soundGenerator.getWaypointAnchor();
    }

    public CentreView getCentreView()
    {
        return centreView;
    }*/

    // CHECK THAT THIS IS CALLED
    @Override
    public void onPreviewFrame(byte[] data, int width, int height)
    {
        // Integer rotation = surfaceView.getSensorOrientation();
        surfaceView.getRenderer().drawFrame(data, width, height, sensorOrientation);
        surfaceView.requestRender();
    }

    @Override
    public Size getDesiredPreviewSize() { return DESIRED_PREVIEW_SIZE; }

    @Override
    public void onPreviewSizeChosen(final Size size, final int orientation)
    {
        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        Log.i(TAG, String.format("Initializing at size %dx%d", previewWidth, previewHeight));

        sensorOrientation = orientation - getScreenOrientation();
        Log.i(TAG, String.format("Camera orientation relative to screen canvas: %d", sensorOrientation));

        // rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
    }

    @Override
    protected void processImage()
    {
        Log.d(TAG, "Processing");
        runInBackground(new Runnable()
        {
            @Override
            public void run()
            {
                // Simulate 30fps delay
                final long startTime = SystemClock.uptimeMillis();
                float[] objectResults = JNIBridge.classifyNew(getProcessingBytes(), previewWidth, previewHeight);
                long lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                for(float result : objectResults)
                {
                    Log.d(TAG, String.format("Object Result %f", result));
                }
                Log.d(TAG, String.format("time %d", lastProcessingTimeMs));
                readyForNextImage();
            }
        });
    }

    public static String getPath(String fileType, Context context)
    {
        AssetManager assetManager = context.getAssets();
        String[] pathNames = {};
        String fileName = "";
        try {
            pathNames = assetManager.list("yolo");
        } catch (IOException e) {
            e.printStackTrace();
        }
        for ( String filePath : pathNames ) {
            if ( filePath.endsWith(fileType)) {
                fileName = filePath;
                break;
            }
        }
        BufferedInputStream inputStream;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open("yolo/" + fileName));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();

            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), fileName);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            // Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            //Log.i(TAG, "Failed to upload a file");
        }
        return "";
    }
}
