package com.example.jaycee.pomdpobjectsearch;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.example.jaycee.pomdpobjectsearch.helpers.ClassHelpers;
import com.google.ar.core.Config;
import com.google.ar.core.Pose;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SoundGenerator2 implements Runnable
{
    private static final String TAG = SoundGenerator2.class.getSimpleName();

    private Pose waypointPose, phonePose;

    private Handler handler = new Handler();
    private Context context;
    private Lock lock = new ReentrantLock();

    private boolean stopped;

    public SoundGenerator2(Context context)
    {
        this.context = context;
        stopped = false;
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
            if (!stopped) handler.postDelayed(this, 40);
            return;
        }
        // Get camera pointing vector from phone pose
        ClassHelpers.mQuaternion phoneRotationQuaternion = new ClassHelpers.mQuaternion(phonePose.getRotationQuaternion());
        phoneRotationQuaternion.normalise();
        ClassHelpers.mVector cameraVector = new ClassHelpers.mVector(0.f, 0.f, 1.f);
        cameraVector.rotateByQuaternion(phoneRotationQuaternion);

        float[] phoneRotationAngles = cameraVector.getEuler();
        float cameraTilt = phoneRotationAngles[1];

        // Get waypoint position in terms of elevation and pan angles
        ClassHelpers.mVector waypointVector = new ClassHelpers.mVector(waypointPose.getTranslation());
        float[] waypointRotationAngles = waypointVector.getEuler();
        float waypointTilt = waypointRotationAngles[1];

        if (waypointTilt > Math.PI / 2)
        {
            waypointTilt -= (float) Math.PI;
        }
        else if (waypointTilt < Math.PI / 2)
        {
            waypointTilt += (float) Math.PI;
        }

        // Get vector between the waypoint and the camera
        ClassHelpers.mVector vectorToWaypoint = waypointVector.translate(cameraVector);

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
        float volumeGrad = -1 / targetSize;
        float volumeMax = 1f;
        if (elevationAngle < targetSize && elevationAngle > 0)
        {
            gain = volumeGrad * (elevationAngle) + volumeMax;
        }
        else if (elevationAngle > -targetSize && elevationAngle < 0)
        {
            gain = -volumeGrad * (elevationAngle) + volumeMax;
        }
        Log.d(TAG, String.format("Gain %f elevation %f pitch %f", gain, elevationAngle, pitch));
        JNIBridge.playSoundFF(gain, pitch * 2);
        if (!stopped) handler.postDelayed(this, 40);
    }

    private float getPitch(double tilt)
    {
        float pitch;
        // From config file; HI setting
        int pitchHighLim = 12;
        int pitchLowLim = 6;

        // Compensate for the Tango's default position being 90deg upright
        if(tilt >= Math.PI / 2)
        {
            pitch = (float)(Math.pow(2, 6));
        }

        else if(tilt <= -Math.PI / 2)
        {
            pitch = (float)(Math.pow(2, pitchHighLim));
        }

        else
        {
            double gradientAngle = Math.toDegrees(Math.atan((pitchHighLim - pitchLowLim) / Math.PI));

            float grad = (float)(Math.tan(Math.toRadians(gradientAngle)));
            float intercept = (float)(pitchHighLim - Math.PI / 2 * grad);

            pitch = (float)(Math.pow(2, grad * -tilt + intercept));
        }

        return pitch;
    }

    public void stop()
    {
        this.stopped = true;
        JNIBridge.playSoundFFFF(0.f, phonePose.getTranslation(), 0, 0);
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
