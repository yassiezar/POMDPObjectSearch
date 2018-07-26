package com.example.jaycee.pomdpobjectsearch;

import com.example.jaycee.pomdpobjectsearch.helpers.ClassHelpers;
import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;

import android.app.Activity;
import android.content.Context;
import android.os.Vibrator;
import android.util.Log;
import android.util.SparseIntArray;
import android.widget.Toast;

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
    private static final long GRID_SIZE = 12;

    private static final int O_NOTHING = 0;

    private Activity callingActivity;

    private Pose phonePose;
    private Pose targetPose;
    private Anchor anchorTarget;
    private Frame frame;

    private float[] targetAngles;

    private boolean targetReached = false;
    private boolean targetSet = false;
    private boolean targetFound = false;

    private long observation = O_NOTHING;
    private long target = -1;
    private long state = 0;

    private Policy policy;

    private ClassMetrics metrics = new ClassMetrics();

    private Vibrator vibrator;

    public RunnableSoundGenerator(Activity callingActivity)
    {
        this.callingActivity = callingActivity;
        this.vibrator= (Vibrator)callingActivity.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public void run()
    {
/*        ClassHelpers.mQuaternion phoneQ = new ClassHelpers.mQuaternion(phonePose.getRotationQuaternion());
        phoneQ.normalise();

        ClassHelpers.mVector cameraVector = new ClassHelpers.mVector(0.f, 0.f, 1.f);
        cameraVector.normalise();
        cameraVector.rotateByQuaternion(phoneQ);
        cameraVector.normalise();

        float[] target = new float[]{targetPose.getTranslation()[0] + (targetPose.getTranslation()[2] - phonePose.getTranslation()[2])*((float)Math.sin(cameraVector.getEuler()[2])), targetPose.getTranslation()[1], targetPose.getTranslation()[2]};

        Log.d(TAG, String.format("pan: %f tilt: %f", Math.abs(cameraVector.getEuler()[2] - targetAngles[2]), Math.abs(cameraVector.getEuler()[1] - targetAngles[1])));

        if(Math.abs(cameraVector.getEuler()[2] - targetAngles[2]) <= 0.15 &&            // 0.13 =~ 7.5deg
                Math.abs(cameraVector.getEuler()[1] - targetAngles[1]) <= 0.15)
        {
            Log.i(TAG, "Target reached");
            targetReached = true;
        }

        float gain = 1.f;
        Log.d(TAG, String.format("Target + Observation: %d %d", targetObject, observation));
        if(observation == targetObject)
        {
            Log.i(TAG, "Target found");
            targetObjectFound = true;
            targetObjectSet = false;
            listTargetFound = new ArrayList<>();
            targetObject = -1;
            observation = 0;
            vibrator.vibrate(500);
            gain = 0.f;
        }
        JNIBridge.playSound(target, phonePose.getTranslation(), gain, getPitch(cameraVector.getEuler()[1] - targetAngles[1]));

        metrics.updateTimestamp(frame.getTimestamp());
        metrics.updatePhonePosition(phonePose.getTranslation()[0], phonePose.getTranslation()[1], phonePose.getTranslation()[2]);
        metrics.updatePhoneOrientation(phonePose.getRotationQuaternion()[0], phonePose.getRotationQuaternion()[1], phonePose.getRotationQuaternion()[2], phonePose.getRotationQuaternion()[3]);*/

        // Get phone's current rotation angles and convert to pan/tilt angles
        // Start by rotating camera vector by quaternion (camera vector = -z)
        ClassHelpers.mQuaternion phoneRotationQuaternion = new ClassHelpers.mQuaternion(phonePose.getRotationQuaternion());
        phoneRotationQuaternion.normalise();
        ClassHelpers.mVector cameraVector = new ClassHelpers.mVector(0.f, 0.f, -1.f);
        cameraVector.rotateByQuaternion(phoneRotationQuaternion);
        cameraVector.normalise();

        // Get Euler angles from vector wrt axis system
        // pitch = tilt, yaw = pan
        float[] phoneRotationAngles = cameraVector.getEuler();
        float pan = phoneRotationAngles[2];
        float tilt = phoneRotationAngles[1];
        long observation = this.observation;

        // Get current state
        long currentState = decodeState(pan, tilt, observation);
        if(currentState != state)
        {
            state = currentState;
        }
    }

    public void update(Camera camera, Session session)
    {
        phonePose = camera.getDisplayOrientedPose();
        if(targetReached || observation != O_NOTHING)
        {
            targetReached = false;
            setNewTarget(session);
        }

        this.run();
        metrics.writeWifi();
    }

    public long decodeState(float fpan, float ftilt, long obs)
    {
        Log.d(TAG, String.format("Pan %f Tilt %f obs %d", fpan, ftilt, obs));

        int pan = (int)(GRID_SIZE - (int)Math.round(Math.toDegrees(fpan) + 90) / ANGLE_INTERVAL);
        int tilt = (int)(GRID_SIZE - (int)Math.round(Math.toDegrees(ftilt) + 90) / ANGLE_INTERVAL);

        long state = 0;
        long multiplier = 1;

        state += (multiplier * pan);
        multiplier *= GRID_SIZE;
        state += (multiplier * tilt);
        multiplier *= GRID_SIZE;
        state += (multiplier * obs);

        return state;
    }

    public long[] encodeState(long state)
    {
        long[] stateVector = new long[3];
        stateVector[0] = state % GRID_SIZE;
        state /= GRID_SIZE;
        stateVector[1] = state % GRID_SIZE;
        state /= GRID_SIZE;
        stateVector[2] = state;

        return stateVector;
    }

    public void setTarget(long target)
    {
        policy = new Policy((int)target);

        this.target = target;
        this.targetSet = true;
        this.targetReached = true;
        this.targetFound = false;

        metrics.updateTarget(target);
    }

    public void setNewTarget(Session session)
    {
/*        ClassHelpers.mVector targetV = new ClassHelpers.mVector(0.f, 0.f, 1.f);
        targetV.normalise();
        targetV.rotateByQuaternion(phonePose.getRotationQuaternion());
        targetV.normalise();

        float[] angles = targetV.getEuler();

        Log.d(TAG, "Pre: " + phonePose.toString());
        Log.d(TAG, String.format("current direction (pre): %f %f %f", angles[0], angles[1], angles[2]));

        final int action = policy.getAction(encodeState(angles[2], angles[1], observation));
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
                rotationR = new ClassHelpers.mQuaternion(0.f,1.f, 0.f, (float)Math.toRadians(ANGLE_INTERVAL));
                break;
            case Policy.A_RIGHT:
                rotationR = new ClassHelpers.mQuaternion(0.f,-1.f, 0.f, (float)Math.toRadians(ANGLE_INTERVAL));
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
        float targetZ = phonePose.getTranslation()[2] - 1.f;

        metrics.updateTargetPosition(targetX, targetY, targetZ);

        targetPose = new Pose(new float[] {targetX, targetY, targetZ}, phoneQ.getQuaternionAsFloat());
        anchorTarget = session.createAnchor(targetPose);

        Log.d(TAG, String.valueOf(action));
        Log.d(TAG, "post: " + targetPose.toString());
        Log.i(TAG, String.format("new target (post): %f %f %f", targetX, targetY, targetZ));

        long[] curState = decodeState(encodeState(angles[1], angles[2], observation));
        long[] nextState = decodeState(encodeState(targetY, targetX, targetObject));
        Log.i(TAG, String.format("Current state: %d", encodeState(angles[1], angles[2], observation)));
        Log.i(TAG, String.format("%d %d %d", curState[0], curState[1], curState[2]));
        Log.i(TAG, String.format("Next state: %d", encodeState(targetY, targetX, targetObject)));
        Log.i(TAG, String.format("%d %d %d", nextState[0], nextState[1], nextState[2]));

        *//*callingActivity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                String sAction = "";
                if(action == 0)
                {
                    sAction = "up";
                }
                if(action == 1)
                {
                    sAction = "down";
                }
                if(action == 2)
                {
                    sAction = "left";
                }
                if(action == 3)
                {
                    sAction = "right";
                }

                Toast.makeText(callingActivity, sAction, Toast.LENGTH_SHORT).show();
            }
        });*/
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

    public void setObservation(final long observation)
    {
        this.observation = observation;
        metrics.updateObservation(observation);

        if(observation != O_NOTHING && observation != -1)
        {
            callingActivity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    String val = "";
                    if(observation == 13)
                    {
                        val = "Door handle";
                    }
                    if(observation == 7)
                    {
                        val = "Mouse";
                    }
                    if(observation == 12)
                    {
                        val = "Door";
                    }
                    if(observation == 18)
                    {
                        val = "Laptop";
                    }
                    if(observation == 6)
                    {
                        val = "Keyboard";
                    }
                    if(observation == 5)
                    {
                        val = "Monitor";
                    }
                    if(observation == 23)
                    {
                        val = "Window";
                    }
                    if(observation == 11)
                    {
                        val = "Desk";
                    }
                    if(observation == 22)
                    {
                        val = "Table";
                    }
                    else
                    {
                        val = "Unknown";
                    }
                    Toast.makeText(callingActivity, val, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public boolean isTargetSet() { return this.targetSet; }
    public boolean isTargetFound() { return this.targetFound; }
    public Anchor getTargetAnchor() { return this.anchorTarget; }
    public long getTarget() { return this.target; }
    public void setFrame(Frame frame) { this.frame = frame; }

    class Policy
    {
        private static final int O_DOOR = 12;
        private static final int O_LAPTOP = 18;
        private static final int O_CHAIR = 8;

        private static final int A_UP = 0;
        private static final int A_DOWN = 1;
        private static final int A_LEFT = 2;
        private static final int A_RIGHT = 3;


        private String fileName = "MDPPolicies/sarsa_";

        private SparseIntArray policy = new SparseIntArray();

        public Policy(int target)
        {
            switch(target)
            {
                case O_DOOR:
                    this.fileName += "door.txt";
                    break;
                case O_LAPTOP:
                    this.fileName += "laptop.txt";
                    break;
                case O_CHAIR:
                    this.fileName += "chair.txt";
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
