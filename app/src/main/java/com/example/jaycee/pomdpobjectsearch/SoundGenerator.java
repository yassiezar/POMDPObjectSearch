package com.example.jaycee.pomdpobjectsearch;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.example.jaycee.pomdpobjectsearch.helpers.VectorTools;
import com.example.jaycee.pomdpobjectsearch.views.Arrow;
import com.google.ar.core.Pose;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.example.jaycee.pomdpobjectsearch.helpers.VectorTools.CameraVector.getCameraVector;

public class SoundGenerator implements Runnable
{
    private static final String TAG = SoundGenerator.class.getSimpleName();

    private static SoundGenerator soundGenerator = null;

    private static final int SOUND_REFRESH_RATE = 40;       // 40 Hz
    private static final int SOUND_HI_LIM = 12;             // Logarithmic hi limit for pitch, in the form of 2^HI
    private static final int SOUND_LO_LIM = 6;

    private Pose waypointPose, phonePose;

    private Handler handler = new Handler();
    private Context context;
    private Lock lock = new ReentrantLock();

    private boolean stopped;

    private SoundGenerator(Context context)
    {
        this.context = context;
        this.stopped = false;
    }

    public synchronized static SoundGenerator create(Context context)
    {
        if(soundGenerator != null)
        {
            Log.w(TAG, "Already initialised soundgenerator");
            return soundGenerator;
        }
        soundGenerator = new SoundGenerator(context);
        return soundGenerator;
    }

    public static SoundGenerator getInstance()
    {
        if(soundGenerator == null)
        {
            throw new AssertionError("Sound generator not initialised");
        }
        return soundGenerator;
    }

    public Lock getLock() { return this.lock; }

    @Override
    public void run()
    {
        if(stopped)
        {
            return;
        }
        if(phonePose == null || waypointPose == null)
        {
            if (!stopped) handler.postDelayed(this, SOUND_REFRESH_RATE);
            return;
        }
        // Get camera pointing vector from phone pose

        VectorTools.mVector cameraVector = getCameraVector(phonePose);
        float cameraTilt = (float)VectorTools.CameraVector.getCameraVectorPanAndTilt(phonePose).tilt;

        // Get waypoint position in terms of elevation and pan angles
        VectorTools.mVector waypointVector = new VectorTools.mVector(waypointPose.getTranslation());
        float[] waypointRotationAngles = waypointVector.getEuler();
        float waypointTilt = waypointRotationAngles[1];

        if (waypointTilt > Math.PI/2)
        {
            waypointTilt -= (float) Math.PI;
        }
        else if (waypointTilt < Math.PI/2)
        {
            waypointTilt += (float) Math.PI;
        }

        // Get vector between the waypoint and the camera
        VectorTools.mVector vectorToWaypoint = waypointVector.translate(cameraVector);

        //Log.d(TAG, vectorToWaypoint.toString());

        ((ActivityGuided) context).updateArrows(null);
        if (vectorToWaypoint.x > 0.1)
        {
            ((ActivityGuided) context).updateArrows(Arrow.Direction.RIGHT);
        }
        else if (vectorToWaypoint.x < -0.1)
        {
            ((ActivityGuided) context).updateArrows(Arrow.Direction.LEFT);
        }
        if (vectorToWaypoint.y > 0.1)
        {
            ((ActivityGuided) context).updateArrows(Arrow.Direction.UP);
        }
        else if (vectorToWaypoint.y < -0.1)
        {
            ((ActivityGuided) context).updateArrows(Arrow.Direction.DOWN);
        }

        float elevationAngle = cameraTilt + waypointTilt;
        float pitch = getPitch(elevationAngle);

        float gain = 1.f;
        JNIBridge.playSoundFFFF(vectorToWaypoint.x, phonePose.getTranslation(), gain, pitch);

        // Interlace second tone to notify user that target is close
        float targetSize = 0.1f;
        float volumeGrad = -1/targetSize;
        float volumeMax = 1f;
        if (elevationAngle < targetSize && elevationAngle > 0)
        {
            gain = volumeGrad*(elevationAngle) + volumeMax;
        }
        else if (elevationAngle > -targetSize && elevationAngle < 0)
        {
            gain = -volumeGrad*(elevationAngle) + volumeMax;
        }
        Log.d(TAG, String.format("Gain %f elevation %f pitch %f", gain, elevationAngle, pitch));
//        JNIBridge.playSoundFF(gain, pitch*2);
        if (!stopped) handler.postDelayed(this, SOUND_REFRESH_RATE);
    }

    private float getPitch(double tilt)
    {
        float pitch;

        // Compensate for the Tango's default position being 90deg upright
        if(tilt >= Math.PI/2)
        {
            pitch = (float)(Math.pow(2, SOUND_LO_LIM));
        }

        else if(tilt <= -Math.PI/2)
        {
            pitch = (float)(Math.pow(2, SOUND_HI_LIM));
        }

        else
        {
            double gradientAngle = Math.toDegrees(Math.atan((SOUND_HI_LIM - SOUND_LO_LIM) / Math.PI));

            float grad = (float)(Math.tan(Math.toRadians(gradientAngle)));
            float intercept = (float)(SOUND_HI_LIM - Math.PI/2*grad);

            pitch = (float)(Math.pow(2, -tilt*grad + intercept));
        }

        return pitch;
    }

    public void stop()
    {
        this.stopped = true;
        float[] dummyLocation = new float[] {0.f, 0.f, 0.f};
        JNIBridge.playSoundFFFF(0.f, dummyLocation, 0, 0);
        JNIBridge.playSoundFF(0, 0);
    }

    public void start()
    {
        this.stopped = false;
        this.run();
    }

    public void setWaypointPose(Pose pose) { this.waypointPose = pose; }
    public void setPhonePose(Pose pose) { this.phonePose = pose; }
}
