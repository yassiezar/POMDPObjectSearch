package com.example.jaycee.pomdpobjectsearch;

import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;

import android.app.Activity;
import android.util.Log;
import android.util.SparseIntArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;

public class RunnableSoundGenerator implements Runnable
{
    private static final String TAG = RunnableSoundGenerator.class.getSimpleName();
    private static final long ANGLE_INTERVAL = 15;
    private static final long GRID_SIZE = 6;

    private Activity callingActivity;

    private Pose phonePose;
    private Pose targetPose;
    private Anchor anchorTarget;

    private boolean targetReached = false;
    private boolean targetObjectSet = false;
    private boolean targetObjectFound = false;

    private long observation = -1;
    private long targetObject = 0;

    private Policy policy;

    public RunnableSoundGenerator(Activity callingActivity)
    {
        this.callingActivity = callingActivity;
    }

    @Override
    public void run()
    {
        float[] currentPhoneRotation = convertQuaternionToEuler(phonePose.getRotationQuaternion());
        float[] targetRotation = convertQuaternionToEuler(targetPose.getRotationQuaternion());

        JNIBridge.playSound(targetPose.getTranslation(), phonePose.getTranslation(), 1.f, getPitch(Math.toRadians(targetRotation[1] - currentPhoneRotation[1])));

        Log.d(TAG, String.format("phone: %f %f %f", currentPhoneRotation[0], currentPhoneRotation[1], currentPhoneRotation[2]));
        Log.d(TAG, String.format("target: %f %f %f", targetRotation[0], targetRotation[1], targetRotation[2]));

        if(Math.abs(currentPhoneRotation[0] - targetRotation[0]) <= 3 &&
                Math.abs(currentPhoneRotation[1] - targetRotation[1]) <= 3)
        {
            Log.i(TAG, "Target reached");
            targetReached = true;
            if(observation == targetObject)
            {
                targetObjectFound = true;
            }
        }
    }

    public void update(Camera camera, Session session)
    {
        phonePose = camera.getDisplayOrientedPose();
        if(targetReached)
        {
            targetReached = false;
            if(anchorTarget != null)
            {
                anchorTarget.detach();
            }

            setNewTarget(session);
        }

        this.run();
    }

    public long encodeState(float fpan, float ftilt, long obs)
    {
        int pan = Math.round(fpan);
        int tilt = Math.round(ftilt);

        if(obs == -1)
        {
            Random rand = new Random();
            obs = rand.nextInt(7);
        }

        long state = 0;
        long multiplier = 1;

        state += (multiplier * pan);
        multiplier *= GRID_SIZE;
        state += (multiplier * tilt);
        multiplier *= GRID_SIZE;
        state += (multiplier * obs);

        return state;
    }

    public long[] decodeState(long state)
    {
        return new long[] {0, 0, 0};
    }

    public float[] convertEulerToQuaternion(float roll, float pitch, float yaw)
    {
        double cy = Math.cos(yaw * 0.5);
        double sy = Math.sin(yaw * 0.5);
        double cr = Math.cos(roll * 0.5);
        double sr = Math.sin(roll * 0.5);
        double cp = Math.cos(pitch * 0.5);
        double sp = Math.sin(pitch * 0.5);

        double qw = cy * cr * cp + sy * sr * sp;
        double qx = cy * sr * cp - sy * cr * sp;
        double qy = cy * cr * sp + sy * sr * cp;
        double qz = sy * cr * cp - cy * sr * sp;

        return new float[] {(float)qx, (float)qy, (float)qz, (float)qw};
    }

    public float[] convertQuaternionToEuler(float[] q)
    {
        float sinr = 2.f * (q[3] * q[1] - q[2] * q[0]);
        float roll;
        if(Math.abs(sinr) >= 1.f)
        {
            roll = (float)Math.copySign(Math.PI / 2, sinr);
        }
        else
        {
            roll = (float)Math.asin(sinr);
        }

        float sinp = 2.0f * (q[3]  * q[0] + q[1] * q[2]);
        float cosp = 1.0f - 2.0f * (q[0] * q[0] + q[1] * q[1]);

        float pitch = (float)Math.atan2(sinp, cosp);

        float siny = 2.f * (q[3] * q[2] + q[0] * q[1]);
        float cosy = 1.f - 2.f * (q[1] * q[1] + q[2] * q[2]);
        float yaw = (float)Math.atan2(siny, cosy);

        return new float[] {(float)(roll*180/Math.PI), (float)(pitch*180/Math.PI), (float)(yaw*180/Math.PI)};
    }

    public void setTargetObject(long target)
    {
        policy = new Policy((int)target);
        this.targetObject = target;
        this.targetObjectSet = true;
        this.targetReached = true;
        this.targetObjectFound = false;
    }

    public void setNewTarget(Session session)
    {
        float[] angles = convertQuaternionToEuler(phonePose.getRotationQuaternion());

        int action = policy.getAction(encodeState(angles[0], angles[1], observation));
        float tilt, pan;
        switch(action)
        {
            case Policy.A_UP:
                pan = angles[0];
                tilt = angles[1] + 1.f * ANGLE_INTERVAL;
                break;
            case Policy.A_DOWN:
                pan = angles[0];
                tilt = angles[1] + (-1.f) * ANGLE_INTERVAL;
                break;
            case Policy.A_LEFT:
                pan = angles[0] + (-1.f) * ANGLE_INTERVAL;
                tilt = angles[1];
                break;
            case Policy.A_RIGHT:
                pan = angles[0] + 1.f * ANGLE_INTERVAL;
                tilt = angles[1];
                break;
            default:
                pan = angles[0];
                tilt = angles[1];
        }

        float[] nextTargetRotation = convertEulerToQuaternion(pan, tilt, angles[2]);

        float targetX = phonePose.getTranslation()[0] + (float)Math.sin(pan);
        float targetY = phonePose.getTranslation()[1] + (float)Math.sin(tilt);
        float targetZ = 1.f;

        targetPose = new Pose(new float[] {targetX, targetY, targetZ}, nextTargetRotation);

        anchorTarget = session.createAnchor(targetPose);
    }

    public float[] multiplyQuaternions(float[] q1, float[] q2)
    {
        float qw = q1[0]*q2[0] - q1[1]*q2[1] - q1[2]*q2[2] - q1[3]*q2[3];
        float qx = q1[0]*q2[1] + q1[1]*q2[0] - q1[2]*q2[3] + q1[3]*q2[2];
        float qy = q1[0]*q2[2] + q1[1]*q2[3] + q1[2]*q2[0] - q1[3]*q2[1];
        float qz = q1[0]*q2[3] - q1[1]*q2[2] + q1[2]*q2[1] + q1[3]*q2[0];

        return new float[] {qx, qy, qz, qw};
    }

    public float[] normaliseQuaternion(float[] q)
    {
        float norm = q[0] + q[1] + q[2] + q[3];

        return new float[] {q[0]/norm, q[1]/norm, q[2]/norm, q[3]/norm};
    }

    public float getPitch(double elevation)
    {
        // Log.i(TAG, String.format("elevation: %f", elevation));
        float pitch;
        // From config file; HI setting
        int pitchHighLim = 12;
        int pitchLowLim = 6;

        // Compensate for the Tango's default position being 90deg upright
        if(elevation >= Math.PI / 2)
        {
            pitch = (float)(Math.pow(2, 64));
        }

        else if(elevation <= -Math.PI / 2)
        {
            pitch = (float)(Math.pow(2, pitchHighLim));
        }

        else
        {
            double gradientAngle = Math.toDegrees(Math.atan((pitchHighLim - pitchLowLim) / Math.PI));

            float grad = (float)(Math.tan(Math.toRadians(gradientAngle)));
            float intercept = (float)(pitchHighLim - Math.PI / 2 * grad);

            pitch = (float)(Math.pow(2, grad * -elevation + intercept));
        }
        Log.i(TAG, String.format("pitch: %f", pitch));

        return pitch;
    }

    public boolean isTargetObjectSet() { return this.targetObjectSet; }
    public void setObservation(int observation) { this.observation = observation; }
    public boolean isTargetObjectFound() { return this.targetObjectFound; }

    class Policy
    {
        private static final int O_COMPUTER_MONITOR = 0;
        private static final int O_DESK = 1;
        private static final int O_WINDOW = 2;
        private static final int O_KETTLE = 3;
        private static final int O_SINK = 4;
        private static final int O_TOILET = 5;
        private static final int O_HAND_DRYER = 6;

        private static final int A_UP = 0;
        private static final int A_DOWN = 1;
        private static final int A_LEFT = 2;
        private static final int A_RIGHT = 3;

        private int target;

        private String fileName = "MDPPolicies/policy_";

        private SparseIntArray policy = new SparseIntArray();

        public Policy(int target)
        {
            this.target = target;

            switch(target)
            {
                case O_COMPUTER_MONITOR:
                    this.fileName += "computer_monitor.txt";
                    break;
                case O_DESK:
                    this.fileName += "desk.txt";
                    break;
                case O_WINDOW:
                    this.fileName += "window.txt";
                    break;
                case O_KETTLE:
                    this.fileName += "kettle.txt";
                    break;
                case O_SINK:
                    this.fileName += "sink.txt";
                    break;
                case O_TOILET:
                    this.fileName += "toilet.txt";
                    break;
                case O_HAND_DRYER:
                    this.fileName += "hand_dryer.txt";
                    break;
            }

            BufferedReader reader = null;
            try
            {
                reader = new BufferedReader(new InputStreamReader(callingActivity.getResources().getAssets().open(fileName)));

                String line;
                while ((line = reader.readLine()) != null)
                {
                    int space1 = line.indexOf('\t');
                    int space2 = line.indexOf('\t', space1 + 1);

                    if(line.charAt(space2 + 1) != '1')
                    {
                        continue;
                    }

                    int key = Integer.parseInt(line.substring(0, space1));
                    int value = Integer.parseInt(line.substring(space1+1, space2));

                    policy.put(key, value);
                }
            }
            catch (IOException e)
            {
                Log.e(TAG, "Could not open policy file: " + e);
            }
            finally
            {
                if (reader != null)
                {
                    try
                    {
                        reader.close();
                    }
                    catch (IOException e)
                    {
                        Log.e(TAG, "Error closing the file: " + e);
                    }
                }
            }
        }

        public int getAction(long state)
        {
            return policy.get((int)state);
        }
    }
}
