package com.example.jaycee.pomdpobjectsearch;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.MenuItem;

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

    private Classifier classifier;

    // private BoundingBoxView boundingBoxView; //to write bounding box of the found object

    // private DrawerLayout drawerLayout;
    // private CentreView centreView;

    // private SoundGenerator soundGenerator;
    // private ObjectDetector objectDetector;

    // private boolean requestARCoreInstall = true;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        surfaceView = findViewById(R.id.surfaceview);

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

        classifier = YoloDetector.create(this);
    }

    @Override
    protected void processImage()
    {
        runInBackground(new Runnable()
        {
            @Override
            public void run()
            {
                if(classifier == null)
                {
                    Log.d(TAG, "Classifier not initialised");
                    readyForNextImage();

                    return;
                }

                final long startTime = SystemClock.uptimeMillis();
                Log.d(TAG, "Processing");
                Recognition[] objectResults = JNIBridge.classifyNew(getProcessingBytes(), previewWidth, previewHeight);
                long lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                if(objectResults.length > 0)
                {
                    // Log.d(TAG, String.format("Object Result %s", objectResults[0].toString()));
                }
                Log.d(TAG, String.format("time %d", lastProcessingTimeMs));
                readyForNextImage();
            }
        });
    }

    @Override
    protected void renderFrame(Image image)
    {
        frameHandler.onPreviewFrame(getPreviewBytes(), image.getWidth(), image.getHeight());
    }
}
