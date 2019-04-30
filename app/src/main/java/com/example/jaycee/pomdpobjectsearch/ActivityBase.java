package com.example.jaycee.pomdpobjectsearch;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.jaycee.pomdpobjectsearch.imageprocessing.FrameHandler;
import com.example.jaycee.pomdpobjectsearch.imageprocessing.FrameScanner;
import com.example.jaycee.pomdpobjectsearch.imageprocessing.ImageConverter;
import com.example.jaycee.pomdpobjectsearch.views.CentreView;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.DeadlineExceededException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

public abstract class ActivityBase extends AppCompatActivity implements FrameHandler
{
    private static final String TAG = ActivityBase.class.getSimpleName();

    private Objects.Observation target = Objects.Observation.O_NOTHING;

    protected CameraSurface surfaceView;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private CentreView centreView;
    private FrameScanner frameScanner;
    private ImageConverter imageConverter;

    private Frame frame;
    private Metrics metrics;

    private Handler backgroundHandler;
    private HandlerThread backgroundHandlerThread;

    private Vibrator vibrator;

    private Session session;
    private Toast toast;

    private boolean processingFrame = false;
    private boolean requestARCoreInstall = true;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        surfaceView = findViewById(R.id.surfaceview);

        centreView = findViewById(R.id.centre_view);

        drawerLayout = findViewById(R.id.layout_drawer_objects);

        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // getSupportActionBar().setHomeButtonEnabled(true);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);

        NavigationView navigationView = findViewById(R.id.navigation_view_objects);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener()
        {
            Objects.Observation target;
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item)
            {
                switch (item.getItemId())
                {
                    case R.id.item_object_backpack:
                        target = Objects.Observation.T_BACKPACK;
                        break;
                    case R.id.item_object_chair:
                        target = Objects.Observation.T_CHAIR;
                        break;
                    case R.id.item_object_couch:
                        target = Objects.Observation.T_COUCH;
                        break;
                    case R.id.item_object_desk:
                        target = Objects.Observation.T_DESK;
                        break;
                    case R.id.item_object_door:
                        target = Objects.Observation.T_DOOR;
                        break;
                    case R.id.item_object_keyboard:
                        target = Objects.Observation.T_COMPUTER_KEYBOARD;
                        break;
                    case R.id.item_object_lamp:
                        target = Objects.Observation.T_LAMP;
                        break;
                    case R.id.item_object_laptop:
                        target = Objects.Observation.T_LAPTOP;
                        break;
                    case R.id.item_object_monitor:
                        target = Objects.Observation.T_COMPUTER_MONITOR;
                        break;
                    case R.id.item_object_mouse:
                        target = Objects.Observation.T_COMPUTER_MOUSE;
                        break;
                    case R.id.item_object_mug:
                        target = Objects.Observation.T_MUG;
                        break;
                    case R.id.item_object_plant:
                        target = Objects.Observation.T_PLANT;
                        break;
                    case R.id.item_object_telephone:
                        target = Objects.Observation.T_TELEPHONE;
                        break;
                    case R.id.item_object_whiteboard:
                        target = Objects.Observation.T_WHITEBOARD;
                        break;
                    case R.id.item_object_window:
                        target = Objects.Observation.T_WINDOW;
                        break;
                    default: target = null;
                }

                item.setCheckable(true);
                if(target != null) setTarget(target);

                drawerLayout.closeDrawers();

                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
//            case android.R.id.home:
//                drawerLayout.openDrawer(GravityCompat.START);
//                return true;
        if(actionBarDrawerToggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(vibrator == null)
        {
            this.vibrator = (Vibrator)this.getSystemService(Context.VIBRATOR_SERVICE);
        }

        try
        {
            frame = Frame.create();
        }
        catch(AssertionError e)
        {
            frame = Frame.getInstance();
        }

        metrics = new Metrics();

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

                session = new Session(this);

                // Set config settings
                Config conf = new Config(session);
                conf.setFocusMode(Config.FocusMode.AUTO);
                session.configure(conf);
            }
            catch(UnavailableUserDeclinedInstallationException | UnavailableArcoreNotInstalledException e)
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

        backgroundHandlerThread = new HandlerThread("InferenceThread");
        backgroundHandlerThread.start();
        backgroundHandler = new Handler(backgroundHandlerThread.getLooper());
    }

    @Override
    protected synchronized void onPause()
    {
        if(!isFinishing())
        {
            finish();
        }

        if(backgroundHandler != null)
        {
            backgroundHandlerThread.quitSafely();
            try
            {
                Log.i(TAG, "Closing detector thread");
                backgroundHandlerThread.join();
                backgroundHandlerThread = null;
                backgroundHandler = null;
            }
            catch(InterruptedException e)
            {
                Log.e(TAG, "Exception onPause: " + e);
            }

        }

        if(vibrator != null)
        {
            vibrator.cancel();
            vibrator = null;
        }

        if(session != null)
        {
            surfaceView.onPause();
            session.pause();
        }

        if(frameScanner != null)
        {
            frameScanner.close();
        }

        super.onPause();
    }

    public void scanFrameForObjects()
    {
        if(processingFrame)
        {
            return;
        }

        if(imageConverter == null)
        {
            Log.w(TAG, "Image converter not initialised");
            imageConverter = new ImageConverter(surfaceView.getRenderer().getWidth(), surfaceView.getRenderer().getHeight());
        }

        if(frameScanner == null)
        {
            int previewWidth = 640;
            int previewHeight = 480;
            frameScanner = new FrameScanner(previewWidth, previewHeight, this);
        }

        processingFrame = true;

        Log.d(TAG, "Processing new frame");

        runInBackground(new Runnable()
        {
            @Override
            public void run()
            {
                // PERFORM DETECTION + INFERENCE
                try
                {
                    frame.getLock().lock();
                    if(frame.isImageClosed())
                    {
                        Log.w(TAG, "Image is closed");
                        return;
                    }
                    int[] imageBytes = imageConverter.getRgbBytes(frame.getImage());
                    frameScanner.updateBitmap(imageBytes);
                }
                catch(DeadlineExceededException e)
                {
                    Log.e(TAG, String.format("Deadline exceeded for image"));
                }
                finally
                {
                    frame.getLock().unlock();
                }

                frameScanner.scanFrame();
                processingFrame = false;
            }
        });
    }

    @Override
    public void onNewFrame(final com.google.ar.core.Frame frame)
    {
        try
        {
            this.frame.getLock().lock();
            this.frame.updateFrame(frame);
        }
        finally
        {
            this.frame.getLock().unlock();
        }
    }

    protected synchronized void runInBackground(final Runnable r)
    {
        if(backgroundHandler != null)
        {
            backgroundHandler.post(r);
        }
    }

    public CentreView getCentreView()
    {
        return centreView;
    }
    public Session getSession() { return this.session; }
    public Frame getFrame() { return this.frame; }
    public Vibrator getVibrator() { return this.vibrator; }
    public Metrics getMetrics() { return this.metrics; }

    public void setTarget(Objects.Observation target) { this.target = target; }
    public Objects.Observation getTarget() { return this.target; }

    @Override
    public void onNewTimestamp(long timestamp) { }

    public void displayToast(final String msg)
    {
        this.runInBackground(new Runnable()
        {
            @Override
            public void run()
            {
                if(toast != null)
                {
                    toast.cancel();
                }
                toast = Toast.makeText(ActivityBase.this, msg, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }
}
