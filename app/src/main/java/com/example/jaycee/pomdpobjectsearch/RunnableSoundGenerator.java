package com.example.jaycee.pomdpobjectsearch;

import com.example.jaycee.pomdpobjectsearch.helpers.ClassHelpers;
import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;

import android.app.Activity;
import android.content.Context;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RunnableSoundGenerator implements Runnable
{
    private static final String TAG = RunnableSoundGenerator.class.getSimpleName();
    private static final long ANGLE_INTERVAL = 15;

    private static final int O_NOTHING = 0;
    private static final int O_DESK = 11;
    private static final int O_LAPTOP = 5;

    private static final int NUM_OBJECTS = 9;
    private static final int MAX_STEPS = 10;
    private static final int HISTORY_LEN = 256;

    private Activity callingActivity;

    private Pose phonePose;
    private Pose waypointPose;
    private Pose offsetPose;
    private Anchor waypointAnchor;
    private Session session;

    private boolean targetSet = false;
    private boolean targetFound = false;

    private long[] historyObservations = new long[256];
    private long observation = O_NOTHING;
    private long prevCameraObservation = O_NOTHING;
    private long steps = 0;
    private long history = 1;
    private long target = -1;
    private long waypointState = decodeState(0, 0, 0);                     /* TODO: Set proper intital condition */

    private Policy policy;

    private ClassMetrics metrics = new ClassMetrics();

    private Vibrator vibrator;
    private Toast toast;

    public RunnableSoundGenerator(Activity callingActivity)
    {
        this.callingActivity = callingActivity;
        this.vibrator= (Vibrator)callingActivity.getSystemService(Context.VIBRATOR_SERVICE);
        waypointPose = new Pose(new float[] {0.f, 0.f, -1.f}, new float[] {0.f, 0.f, 0.f, 1.f});
    }

    @Override
    public void run()
    {
        float gain = 1.f;
        if(observation == target)
        {
            Log.i(TAG, "Target found");
            targetFound = true;
            targetSet = false;
            observation = O_NOTHING;
            vibrator.vibrate(350);
            gain = 0.f;
        }

        // Get Euler angles from vector wrt axis system
        // pitch = tilt, yaw = pan
        ClassHelpers.mVector cameraVector = getRotation(phonePose, false);
        float[] phoneRotationAngles = cameraVector.getEuler();
        float cameraPan = phoneRotationAngles[2];
        float cameraTilt = phoneRotationAngles[1];
        long newCameraObservation = this.observation;

        // Get current state and generate new waypoint if agent is in new state or sees new object
        long currentState = decodeState(newCameraObservation, steps, history);
        long[] currentStateArr = encodeState(currentState);
        //long[] waypointArr = encodeState(waypointState);
        Log.i(TAG, String.format("current pan %d tilt %d obs %d ", currentStateArr[0], currentStateArr[1], currentStateArr[2]));
        Log.d(TAG, String.format("current pan %f tilt %f ", cameraPan, cameraTilt));
        Log.i(TAG, String.format("Current state %d Waypoint state %d", currentState, waypointState));
        if(equalPositionState(currentState, waypointState) || (newCameraObservation != prevCameraObservation && newCameraObservation != O_NOTHING))// && newCameraObservation != currentStateArr[2]))
        {
            long action = policy.getAction(currentState);
            Log.i(TAG, String.format("Object found or found waypoint, action: %d", action));
            waypointPose = getNewWaypoint(phonePose, currentState, action);
            waypointAnchor = session.createAnchor(waypointPose);
            prevCameraObservation = newCameraObservation;
            steps += 1;
            history *= primeObservation(newCameraObservation);
        }
        ClassHelpers.mVector waypointVector = getRotation(waypointPose, false);
        float[] waypointRotationAngles = waypointVector.getEuler();
        float waypointTilt = waypointRotationAngles[1];

        // float tiltRequired = (float)Math.atan2(cameraVector.y - waypointVector.y, cameraVector.z - waypointVector.z);
        // Log.i(TAG, String.format("Tilt required %f", tiltRequired));

        JNIBridge.playSound(waypointPose.getTranslation(), cameraVector.asFloat(), gain, getPitch(waypointTilt - cameraTilt));
    }

    public void update(Camera camera, Session session)
    {
        phonePose = camera.getDisplayOrientedPose();
        metrics.writeWifi();
        this.session = session;

        this.run();
    }

    private ClassHelpers.mVector getRotation(Pose pose, boolean waypointPose)
    {
        // Get rotation angles and convert to pan/tilt angles
        // Start by rotating vector by quaternion (camera vector = -z)
        ClassHelpers.mQuaternion phoneRotationQuaternion = new ClassHelpers.mQuaternion(pose.getRotationQuaternion());
        phoneRotationQuaternion.normalise();
        ClassHelpers.mVector vector = new ClassHelpers.mVector(0.f, 0.f, -1.f);
        vector.rotateByQuaternion(phoneRotationQuaternion);
        vector.normalise();

        // Add initial offset pose
        ClassHelpers.mQuaternion offsetRotationQuaternion = new ClassHelpers.mQuaternion(offsetPose.getRotationQuaternion());
        offsetRotationQuaternion.normalise();
        ClassHelpers.mVector offsetVector = new ClassHelpers.mVector(0.f, 0.f, -1.f);
        offsetVector.rotateByQuaternion(offsetRotationQuaternion);
        offsetVector.normalise();

        if(!waypointPose)
        {
            vector.x -= offsetVector.x;
            vector.y -= offsetVector.y;
        }

        return vector;
    }

/*    public long decodeState(float fpan, float ftilt, long obs)
    {
        // Origin is top right, not bottom left
        int pan = (int)(-Math.round(Math.toDegrees(-fpan) / ANGLE_INTERVAL) + GRID_SIZE/2 - 1);
        int tilt = (int)(-Math.round(Math.toDegrees(-ftilt) / ANGLE_INTERVAL) + GRID_SIZE/2 - 1);

        long state = 0;
        long multiplier = 1;

        state += (multiplier * pan);
        multiplier *= GRID_SIZE;
        state += (multiplier * tilt);
        multiplier *= GRID_SIZE;
        state += (multiplier * obs);

        return state;
    }*/

    private long decodeState(long obs, long steps, long history)
    {
        long state = 0;
        long multiplier = 1;

        state += (multiplier * obs);
        multiplier *= NUM_OBJECTS;
        state += (multiplier * steps);
        multiplier *= MAX_STEPS;
        state += (multiplier * history);

        return state;
    }

    private long[] encodeState(long state)
    {
        long[] stateVector = new long[3];
        stateVector[0] = state % NUM_OBJECTS;
        state /= NUM_OBJECTS;
        stateVector[1] = state % MAX_STEPS;
        state /= MAX_STEPS;
        stateVector[2] = state % HISTORY_LEN;

        return stateVector;
    }

    private boolean equalPositionState(long s1, long s2)
    {
        long[] state1 = encodeState(s1);
        long[] state2 = encodeState(s2);

        return (state1[0] == state2[0]) && (state1[1] == state2[1]);
    }

    public void setTarget(long target)
    {
        policy = new Policy((int)target);

        this.target = target;
        this.targetSet = true;
        this.targetFound = false;

        metrics.updateTarget(target);
    }

    private Pose getNewWaypoint(Pose phonePose, long s, long action)
    {
        float[] wayPointTranslation = new float[3];
        long[] state = encodeState(s);

        // Assume the current waypoint is where the camera is pointing.
        // Reasonable since this function only called when pointing to new target
        ClassHelpers.mVector waypointVector = getRotation(phonePose, true);
        waypointVector.x /= waypointVector.z;
        waypointVector.y /= waypointVector.z;
        waypointVector.z /= waypointVector.z;

        if(action == Policy.A_LEFT)
        {
            wayPointTranslation[0] = waypointVector.x - 1.f*(float)Math.sin(Math.toRadians(ANGLE_INTERVAL));
            wayPointTranslation[1] = waypointVector.y;
            state[0] += 1;
        }
        if(action == Policy.A_RIGHT)
        {
            wayPointTranslation[0] = waypointVector.x + 1.f*(float)Math.sin(Math.toRadians(ANGLE_INTERVAL));
            wayPointTranslation[1] = waypointVector.y;
            state[0] -= 1;
        }

        if(action == Policy.A_UP)
        {
            wayPointTranslation[0] = waypointVector.x;
            wayPointTranslation[1] = waypointVector.y + 1.f*(float)Math.sin(Math.toRadians(ANGLE_INTERVAL));
            state[1] -= 1;
        }
        if(action == Policy.A_DOWN)
        {
            wayPointTranslation[0] = waypointVector.x;
            wayPointTranslation[1] = waypointVector.y - 1.f*(float)Math.sin(Math.toRadians(ANGLE_INTERVAL));
            state[1] += 1;
        }

        waypointState = decodeState(state[0], state[1], state[2]);

        // wayPointTranslation[0] = phonePose.getTranslation()[0] - 1.f;
        // wayPointTranslation[1] = phonePose.getTranslation()[1] - 1.f;
        wayPointTranslation[2] = phonePose.getTranslation()[2] - 1.f;

        return new Pose(wayPointTranslation, phonePose.getRotationQuaternion());
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
                    String val;
                    if(observation == 13)
                    {
                        val = "Door handle";
                    }
                    else if(observation == 7)
                    {
                        val = "Mouse";
                    }
                    else if(observation == 12)
                    {
                        val = "Door";
                    }
                    else if(observation == 18)
                    {
                        val = "Laptop";
                    }
                    else if(observation == 6)
                    {
                        val = "Keyboard";
                    }
                    else if(observation == 5)
                    {
                        val = "Monitor";
                    }
                    else if(observation == 23)
                    {
                        val = "Window";
                    }
                    else if(observation == 11)
                    {
                        val = "Desk";
                    }
                    else if(observation == 22)
                    {
                        val = "Table";
                    }
                    else
                    {
                        val = "Unknown";
                    }

                    if(toast != null)
                    {
                        toast.cancel();
                    }
                    toast = Toast.makeText(callingActivity, val, Toast.LENGTH_SHORT);
                    toast.show();
                }
            });
        }
    }

    public void setOffsetPose(Pose pose) { this.offsetPose = pose; }
    public boolean isTargetSet() { return this.targetSet; }
    public boolean isTargetFound() { return this.targetFound; }
    public long getTarget() { return this.target; }
    public Anchor getWaypointAnchor() { return this.waypointAnchor; }

    class State
    {

    }

    class Policy
    {
        private static final int O_MUG = 6;
        private static final int O_LAPTOP = 4;
        private static final int O_WINDOW = 8;

        private static final int A_UP = 0;
        private static final int A_DOWN = 1;
        private static final int A_LEFT = 2;
        private static final int A_RIGHT = 3;

        private String fileName = "MDPPolicies/sarsa_";

        private Map<Long, ArrayList<Long>> policy = new HashMap<>();

        public Policy(int target)
        {
            switch(target)
            {
                case O_MUG:
                    this.fileName += "mug.txt";
                    break;
                case O_LAPTOP:
                    this.fileName += "laptop.txt";
                    break;
                case O_WINDOW:
                    this.fileName += "window.txt";
                    break;
            }

            BufferedReader reader = null;
            try
            {
                // Extract policy state-action pairs from text file using regex
                Pattern pattern = Pattern.compile("(\\d+)\\s(\\d)\\s(1.0|0.25)");
                reader = new BufferedReader(new InputStreamReader(callingActivity.getResources().getAssets().open(fileName)));

                String line;
                while ((line = reader.readLine()) != null)
                {
                    // Save all non-zero prob actions into a hashmap to sample from later
                    Matcher matcher = pattern.matcher(line);
                    if(matcher.find())
                    {
                        if(Double.valueOf(matcher.group(3)) > 0.0)
                        {
                            long state = Long.valueOf(matcher.group(1));
                            long action = Long.valueOf(matcher.group(2));

                            policy.putIfAbsent(state, new ArrayList<Long>());
                            policy.get(state).add(action);
                        }
                    }
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

        long getAction(long state)
        {
            // Draw random action from action set from policy
            Random rand = new Random();

            int nActions = policy.get(state).size();
            return policy.get(state).get(rand.nextInt(nActions));
        }
    }
}
