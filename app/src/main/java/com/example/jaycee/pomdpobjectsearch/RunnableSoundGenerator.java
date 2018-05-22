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
import java.util.HashMap;
import java.util.Map;

public class RunnableSoundGenerator implements Runnable
{
    private static final String TAG = RunnableSoundGenerator.class.getSimpleName();
    private static final long ANGLE_INTERVAL = 30;

    private Activity callingActivity;

    private Pose phonePose;
    private Pose targetPose;
    private Anchor anchorTarget;

    private boolean targetReached = false;
    private boolean targetSet = false;

    private long observation = 0;
    private long target = 0;

    private Policy policy;

    public RunnableSoundGenerator(Activity callingActivity)
    {
        this.callingActivity = callingActivity;
    }

    @Override
    public void run()
    {
        float[] currentPhoneRotation = convertQuaternionToEuler(phonePose.getRotationQuaternion());
        //float[] targetRotation = convertQuaternionToEuler(targetPose.getRotationQuaternion());

        /*
        if(Math.abs(currentPhoneRotation[0] - targetRotation[0]) <= 3 ||
                Math.abs(currentPhoneRotation[1] - targetRotation[1]) <= 3)
        {
            targetReached = true;
            observation = 0;
        }
        */
        Log.d(TAG, String.format("roll: %f pitch: %f yaw: %f", currentPhoneRotation[0], currentPhoneRotation[1], currentPhoneRotation[2]));
    }

    public void update(Camera camera, Session session)
    {
        phonePose = camera.getDisplayOrientedPose();
        if(targetReached)
        {
            targetReached = false;
            anchorTarget.detach();

            float[] angles = convertQuaternionToEuler(phonePose.getRotationQuaternion());

            //long action = JNIBridge.getAction(decodeState(angles[0], angles[1], observation));
            long action = policy.getAction(decodeState(angles[0], angles[1], observation));
            //float roll = action[0] * ANGLE_INTERVAL;
            //float pitch = action[1] * ANGLE_INTERVAL;

            //float[] nextTargetRotation = convertEulerToQuaternion(roll, pitch, 0);

            //targetPose = new Pose(new float[] {0.f, 0.f, 0.f}, nextTargetRotation);

            //anchorTarget = session.createAnchor(targetPose);
        }

        this.run();
    }

    public long decodeState(float pan, float tilt, float obs)
    {
        return 0;
    }

    public long[] encodeState(long state)
    {
        return new long[] {0, 0, 0};
    }

    public float[] convertEulerToQuaternion(float roll, float pitch, float yaw)
    {
        return new float[] {0.f, 0.f, 0.f, 0.f};
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

        return new float[] {roll, pitch, yaw};
    }

    public void setTarget(long target)
    {
        policy = new Policy((int)target);

        this.target = target;

        this.targetSet = true;
    }

    public boolean isTargetSet() { return this.targetSet; }

    class Policy
    {
        private static final int O_COMPUTER_MONITOR = 0;
        private static final int O_DESK = 1;
        private static final int O_WINDOW = 2;
        private static final int O_KETTLE = 3;
        private static final int O_SINK = 4;
        private static final int O_TOILET = 5;
        private static final int O_HAND_DRYER = 6;

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

        public long getAction(long state)
        {
            return policy.get((int)state);
        }
    }
}
