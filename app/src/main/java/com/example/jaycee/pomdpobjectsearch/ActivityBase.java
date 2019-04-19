package com.example.jaycee.pomdpobjectsearch;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import com.example.jaycee.pomdpobjectsearch.helpers.ImageConverter;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.DeadlineExceededException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

public abstract class ActivityBase extends AppCompatActivity implements FrameHandler
{
    private static final String TAG = ActivityBase.class.getSimpleName();

    protected enum Observation
    {
        O_NOTHING (0),
        T_COMPUTER_MONITOR (1),
        T_COMPUTER_KEYBOARD (2),
        T_COMPUTER_MOUSE (3),
        T_DESK (4),
        T_LAPTOP (5),
        T_MUG (6),
        T_WINDOW (7),
        T_LAMP (8),
        T_BACKPACK (9),
        T_CHAIR (10),
        T_COUCH (11),
        T_PLANT (12),
        T_TELEPHONE (13),
        T_WHITEBOARD (14),
        T_DOOR (15);

        private final int obsCode;
        Observation(int obsCode) { this.obsCode = obsCode; }

        public int getCode() { return this.obsCode; }
    }

    private Observation target = Observation.O_NOTHING;

    protected CameraSurface surfaceView;
    private DrawerLayout drawerLayout;
    private CentreView centreView;

    private Handler backgroundHandler;
    private HandlerThread backgroundHandlerThread;

    private Session session;
    private Frame frame = Frame.getFrame();

    private FrameScanner frameScanner;

    private ImageConverter imageConverter;

    private boolean processingFrame = false;
    private boolean requestARCoreInstall = true;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

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
            Observation target;
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item)
            {
                switch (item.getItemId())
                {
                    case R.id.item_object_backpack:
                        target = Observation.T_BACKPACK;
                        break;
                    case R.id.item_object_chair:
                        target = Observation.T_CHAIR;
                        break;
                    case R.id.item_object_couch:
                        target = Observation.T_COUCH;
                        break;
                    case R.id.item_object_desk:
                        target = Observation.T_DESK;
                        break;
                    case R.id.item_object_door:
                        target = Observation.T_DOOR;
                        break;
                    case R.id.item_object_keyboard:
                        target = Observation.T_COMPUTER_KEYBOARD;
                        break;
                    case R.id.item_object_lamp:
                        target = Observation.T_LAMP;
                        break;
                    case R.id.item_object_laptop:
                        target = Observation.T_LAPTOP;
                        break;
                    case R.id.item_object_monitor:
                        target = Observation.T_COMPUTER_MONITOR;
                        break;
                    case R.id.item_object_mouse:
                        target = Observation.T_COMPUTER_MOUSE;
                        break;
                    case R.id.item_object_mug:
                        target = Observation.T_MUG;
                        break;
                    case R.id.item_object_plant:
                        target = Observation.T_PLANT;
                        break;
                    case R.id.item_object_telephone:
                        target = Observation.T_TELEPHONE;
                        break;
                    case R.id.item_object_whiteboard:
                        target = Observation.T_WHITEBOARD;
                        break;
                    case R.id.item_object_window:
                        target = Observation.T_WINDOW;
                        break;
                }

                item.setCheckable(true);
                setTarget(target);

                drawerLayout.closeDrawers();

                return true;
            }
        });
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

        // PERFORM DETECTION + INFERENCE
        try
        {
            this.frame.getLock().lock();
            com.google.ar.core.Frame arFrame = frame.getArFrame();
            int[] imageBytes = imageConverter.getRgbBytes(arFrame.acquireCameraImage());
            frameScanner.updateBitmap(imageBytes);
        }
        catch(DeadlineExceededException e)
        {
            Log.e(TAG, "Deadline exceeded for image");
        }
        catch(NotYetAvailableException e)
        {
            Log.e(TAG, "Camera not yet ready: " + e);
        }
        finally
        {
            this.frame.getLock().unlock();
        }

        runInBackground(new Runnable()
        {
            @Override
            public void run()
            {
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
            this.frame.setFrame(frame);
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

    public void setTarget(Observation target) { this.target = target; }
    public Observation getTarget() { return this.target; }

    @Override
    public void onNewTimestamp(long timestamp) { }
}
