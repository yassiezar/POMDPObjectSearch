package com.example.jaycee.pomdpobjectsearch;

import com.example.jaycee.pomdpobjectsearch.helpers.ClassHelpers;
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
import java.util.ArrayList;
import java.util.List;
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

    private float[] targetAngles;

    private boolean targetReached = false;
    private boolean targetObjectSet = false;
    private boolean targetObjectFound = false;

    private long observation = -1;
    private long targetObject = 0;

    private Policy policy;

    private List<Long> listTargetFound;

    ClassMetrics metrics = new ClassMetrics();

    public RunnableSoundGenerator(Activity callingActivity)
    {
        this.callingActivity = callingActivity;
        this.listTargetFound = new ArrayList<>();
    }

    @Override
    public void run()
    {
        ClassHelpers.mQuaternion phoneQ = new ClassHelpers.mQuaternion(phonePose.getRotationQuaternion());
        phoneQ.normalise();

        ClassHelpers.mVector cameraVector = new ClassHelpers.mVector(0.f, 0.f, 1.f);
        cameraVector.normalise();
        cameraVector.rotateByQuaternion(phoneQ);
        cameraVector.normalise();

        JNIBridge.playSound(targetPose.getTranslation(), phonePose.getTranslation(), 1.f, getPitch(cameraVector.getEuler()[1] - targetAngles[1]));

        Log.i(TAG, String.format("pan: %f tilt: %f", Math.abs(cameraVector.getEuler()[2] - targetAngles[2]), Math.abs(cameraVector.getEuler()[1] - targetAngles[1])));

        if(Math.abs(cameraVector.getEuler()[2] - targetAngles[2]) <= 0.025 &&            // 0.025 == 3deg
                Math.abs(cameraVector.getEuler()[1] - targetAngles[1]) <= 0.025)
        {
            Log.i(TAG, "Target reached");
            targetReached = true;
        }

        if(observation == targetObject)
        {
            Log.i(TAG, "Target found");
            targetObjectFound = true;
            targetObjectSet = false;
            listTargetFound = new ArrayList<>();
        }
    }

    public void update(Camera camera, Session session)
    {
        phonePose = camera.getDisplayOrientedPose();
        if(targetReached ||
                observation != -1)
        {
            targetReached = false;
            if(anchorTarget != null)
            {
                anchorTarget.detach();
                anchorTarget = null;
            }

            setNewTarget(session);
        }

        this.run();
        metrics.writeWifi();
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

    public void setTargetObject(long target)
    {
        policy = new Policy((int)target);
        listTargetFound = new ArrayList<>();

        this.targetObject = target;
        this.targetObjectSet = true;
        this.targetReached = true;
        this.targetObjectFound = false;
    }

    public void setNewTarget(Session session)
    {
        ClassHelpers.mVector targetV = new ClassHelpers.mVector(0.f, 0.f, 1.f);
        targetV.normalise();
        targetV.rotateByQuaternion(phonePose.getRotationQuaternion());
        targetV.normalise();

        float[] angles = targetV.getEuler();

        Log.i(TAG, "Pre: " + phonePose.toString());
        Log.i(TAG, String.format("current direction (pre): %f %f %f", angles[0], angles[1], angles[2]));

        int action = policy.getAction(encodeState(angles[1], angles[0], observation));
        ClassHelpers.mQuaternion rotationR;
        switch(action)
        {
            case Policy.A_UP:
                rotationR = new ClassHelpers.mQuaternion(1.f, 0.f, 0.f, (float)Math.toRadians(ANGLE_INTERVAL));
                break;
            case Policy.A_DOWN:
                rotationR = new ClassHelpers.mQuaternion(-1.f, 0.f, 0.f, (float)Math.toRadians(ANGLE_INTERVAL));
                break;
            case Policy.A_LEFT:
                rotationR = new ClassHelpers.mQuaternion(0.f,-1.f, 0.f, (float)Math.toRadians(ANGLE_INTERVAL));
                break;
            case Policy.A_RIGHT:
                rotationR = new ClassHelpers.mQuaternion(0.f, 1.f, 0.f, (float)Math.toRadians(ANGLE_INTERVAL));
                break;
            default:
                rotationR = new ClassHelpers.mQuaternion(0.f, 0.f, 0.f, 0.f);
        }
        rotationR.normalise();

        ClassHelpers.mVector cameraVector = new ClassHelpers.mVector(0.f, 0.f, 1.f);
        cameraVector.normalise();

        cameraVector.rotateByQuaternion(rotationR);
        cameraVector.normalise();

        targetAngles = cameraVector.getEuler();
        targetAngles[0] = angles[0];
        targetAngles[1] += angles[1];
        targetAngles[2] += angles[2];

        ClassHelpers.mQuaternion phoneQ = new ClassHelpers.mQuaternion(phonePose.getRotationQuaternion());
        phoneQ.multiply(rotationR);
        phoneQ.normalise();

        float targetX = phonePose.getTranslation()[0] + (float)Math.sin(targetAngles[2]);
        float targetY = phonePose.getTranslation()[1] + (float)Math.sin(targetAngles[1]);
        float targetZ = phonePose.getTranslation()[2] -1.f;

        targetPose = new Pose(new float[] {targetX, targetY, targetZ}, phoneQ.getQuaternionAsFloat());
        anchorTarget = session.createAnchor(targetPose);

        Log.i(TAG, String.valueOf(action));
        Log.i(TAG, "post: " + targetPose.toString());
        Log.i(TAG, String.format("new target (post): %f %f %f", targetAngles[0], targetAngles[1], targetAngles[2]));
    }

    public float getPitch(double tilt)
    {
        float pitch;
        // From config file; HI setting
        int pitchHighLim = 12;
        int pitchLowLim = 6;

        // Compensate for the Tango's default position being 90deg upright
        if(tilt >= Math.PI / 2)
        {
            pitch = (float)(Math.pow(2, 64));
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
        Log.d(TAG, String.format("pitch: %f", pitch));

        return pitch;
    }

    public boolean isTargetObjectSet() { return this.targetObjectSet; }
    public boolean isTargetObjectFound() { return this.targetObjectFound; }
    public void setObservation(long observation)
    {
        if(!listTargetFound.contains(observation))
        {
            listTargetFound.add(observation);
            this.observation = observation;
        }
        else
        {
            this.observation = -1;
        }
    }

    public Anchor getTargetAnchor() { return this.anchorTarget; }

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
