package com.example.jaycee.pomdpobjectsearch;

import com.example.jaycee.pomdpobjectsearch.mdptools.GuidanceInterface;
import com.example.jaycee.pomdpobjectsearch.rendering.SurfaceRenderer;

import android.content.Context;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

public class SoundGenerator implements Runnable
{
    private static final String TAG = SoundGenerator.class.getSimpleName();

    private static final int O_NOTHING = 0;

    private Context context;
    private SurfaceRenderer renderer;

    private long observation = O_NOTHING;
    private long prevCameraObservation = O_NOTHING;
    private long target = -1;

    private Metrics metrics = new Metrics();

    private Vibrator vibrator;
    private Toast toast;
    private Handler handler = new Handler();

    private boolean stop = false;
    private boolean targetSet = false;
    private boolean targetFound = false;

    private BarcodeListener barcodeListener;
    private GuidanceInterface guidanceInterface;

    SoundGenerator(Context context, SurfaceRenderer renderer)
    {
        this.context = context;
        this.renderer = renderer;
        this.barcodeListener = (BarcodeListener)context;
        this.guidanceInterface = (GuidanceInterface)context;

        this.vibrator= (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void stop()
    {
        this.stop = true;
        handler = null;
    }

    @Override
    public void run()
    {
        if(!isTargetSet() || isTargetFound())
        {
            if(!stop) handler.postDelayed(this, 40);
            return;
        }

        if(!renderer.isRendererReady())
        {
            if(!stop) handler.postDelayed(this, 40);
            return;
        }

        setObservation(barcodeListener.onBarcodeScan());

        if(observation == target)
        {
            Log.i(TAG, "Target found");
            targetFound = true;
            targetSet = false;
            observation = O_NOTHING;
            vibrator.vibrate(350);
            renderer.setDrawWaypoint(false);
            guidanceInterface.onGuidanceEnd();
        }

        guidanceInterface.onNewPoseAvailable();
        long newCameraObservation = this.observation;

        if(guidanceInterface.onWaypointReached() || (newCameraObservation != prevCameraObservation && newCameraObservation != O_NOTHING))
        {
            prevCameraObservation = newCameraObservation;
            guidanceInterface.onGuidanceRequested(newCameraObservation);
            Log.i(TAG, "Setting new waypoint");
        }

        guidanceInterface.onPlaySound();
        if(!stop) handler.postDelayed(this, 40);
    }


    public void setTarget(long target)
    {
        this.target = target;
        this.targetSet = true;
        this.targetFound = false;

        prevCameraObservation = O_NOTHING;

        metrics.updateTarget(target);

        renderer.setDrawWaypoint(true);
    }

    public void setObservation(long observation)
    {
        final String val;
        if(observation == 1)
        {
            val = "Monitor";
        }
        else if(observation == 2)
        {
            val = "Keyboard";
        }
        else if(observation == 3)
        {
            val = "Mouse";
        }
        else if(observation == 4)
        {
            val = "Desk";
        }
        else if(observation == 5)
        {
            val = "Laptop";
        }
        else if(observation == 6)
        {
            val = "Mug";
        }
        else if(observation == 7)
        {
            val = "Office supplies";
        }
        else if(observation == 8)
        {
            val = "Window";
        }
        else
        {
            val = "Unknown";
        }
        this.observation = observation;
        this.metrics.updateObservation(observation);

        if(observation != O_NOTHING && observation != -1)
        {
            ((ActivityCamera)context).runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    if(toast != null)
                    {
                        toast.cancel();
                    }
                    toast = Toast.makeText(context, val, Toast.LENGTH_SHORT);
                    toast.show();
                }
            });
        }
    }

    public boolean isTargetSet() { return this.targetSet; }
    public boolean isTargetFound() { return this.targetFound; }
    public long getTarget() { return this.target; }
}
