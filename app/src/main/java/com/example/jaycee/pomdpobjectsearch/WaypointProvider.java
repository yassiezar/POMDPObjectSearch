package com.example.jaycee.pomdpobjectsearch;

import android.content.Context;
import android.util.Log;

import com.example.jaycee.pomdpobjectsearch.helpers.ClassHelpers;
import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WaypointProvider
{
    private static final String TAG = WaypointProvider.class.getSimpleName();

    private static final int GRID_SIZE = 6;
    private static final int ANGLE_INTERVAL = 20;

    private Pose pose;

    private State state;
    private Policy policy;

    private Lock lock = new ReentrantLock();

    public ActivityBase.Observation observation = ActivityBase.Observation.O_NOTHING;

    public WaypointProvider(Pose pose)
    {
        float[] phoneTranslation = pose.getTranslation();
        this.pose = new Pose(new float[]{phoneTranslation[0], phoneTranslation[1], phoneTranslation[2] - 1.f}, pose.getRotationQuaternion());
    }

    public WaypointProvider() {}

    public Lock getLock() { return this.lock; }

    public void setTarget(ActivityBase.Observation target, Context context)
    {
        if(target == ActivityBase.Observation.O_NOTHING)
        {
            state = null;
            policy = null;
        }
        state = new State();
        policy = new Policy(target, context);
    }

    Pose getWaypointPose()
    {
        return pose;
    }

    void updateWaypoint(Pose phonePose, ActivityBase.Observation observation)
    {
        ClassHelpers.mQuaternion phoneRotationQuaternion = new ClassHelpers.mQuaternion(phonePose.getRotationQuaternion());
        phoneRotationQuaternion.normalise();
        ClassHelpers.mVector cameraVector = new ClassHelpers.mVector(0.f, 0.f, 1.f);
        cameraVector.rotateByQuaternion(phoneRotationQuaternion);
        float[] phoneRotationAngles = cameraVector.getEuler();
        float cameraPan = phoneRotationAngles[2];
        float cameraTilt = phoneRotationAngles[1];

        this.observation = observation;
        state.addObservation(observation, cameraPan, cameraTilt);
        long action = policy.getAction(state);

        float[] wayPointTranslation = new float[3];

        // Assume the current waypoint is where the camera is pointing.
        // Reasonable since this function only called when pointing to new target
        // Discretise pan/tilt into grid
        int pan = (int) ((Math.floor(Math.toDegrees(cameraPan) / ANGLE_INTERVAL)) + GRID_SIZE / 2 - 1);
        int tilt = (int) ((Math.floor(Math.toDegrees(cameraTilt) / ANGLE_INTERVAL)) + GRID_SIZE / 2 - 1);

        if (action == Policy.A_LEFT)
        {
            pan -= 1;
        }
        else if (action == Policy.A_RIGHT)
        {
            pan += 1;
        }
        else if (action == Policy.A_UP)
        {
            tilt += 1;
        }
        else if (action == Policy.A_DOWN)
        {
            tilt -= 1;
        }

        // Wrap the world
        if (pan < 0) pan = GRID_SIZE - 1;
        if (pan > GRID_SIZE - 1) pan = 0;
        if (tilt < 0) tilt = GRID_SIZE - 1;
        if (tilt > GRID_SIZE - 1) tilt = 0;

        float z = phonePose.getTranslation()[2] - 1.f;
        wayPointTranslation[0] = (float) Math.sin(Math.toRadians(ANGLE_INTERVAL * (pan - GRID_SIZE / 2.0 + 1)));
        wayPointTranslation[1] = (float) Math.sin(Math.toRadians(ANGLE_INTERVAL * (tilt - GRID_SIZE / 2.0 + 1)));
        wayPointTranslation[2] = z;

        // Log.i(TAG, String.format("new pan: %d new tilt: %d", pan, tilt));
        //Log.i(TAG, String.format("translation x %f translation y: %f", wayPointTranslation[0], wayPointTranslation[1]));

        pose = new Pose(wayPointTranslation, new float[]{0.f, 0.f, 0.f, 1.f});
    }

    public boolean waypointReached(Pose phonePose)
    {
        if(pose == null)
        {
            return true;
        }

        ClassHelpers.mQuaternion phoneRotationQuaternion = new ClassHelpers.mQuaternion(phonePose.getRotationQuaternion());
        phoneRotationQuaternion.normalise();
        ClassHelpers.mVector cameraVector = new ClassHelpers.mVector(0.f, 0.f, 1.f);

        cameraVector.rotateByQuaternion(phoneRotationQuaternion);
        float[] phoneRotationAngles = cameraVector.getEuler();
        float cameraPan = phoneRotationAngles[2];
        float cameraTilt = phoneRotationAngles[1];

        float x = pose.getTranslation()[0];
        float y = pose.getTranslation()[1];

        // Compensate for Z-axis going in negative direction, rotating pan around y-axis
        return Math.abs(Math.sin(cameraTilt) - y) < 0.1 && Math.abs(Math.cos(-cameraPan + Math.PI / 2) - x) < 0.1;
    }

    class Policy
    {
        private static final int A_UP = 0;
        private static final int A_DOWN = 1;
        private static final int A_LEFT = 2;
        private static final int A_RIGHT = 3;

        private String fileName = "MDPPolicies/sarsa_";

        private Map<Long, ArrayList<Long>> policy = new HashMap<>();

        public Policy(ActivityBase.Observation target, Context context)
        {
            switch (target)
            {
                case T_COMPUTER_MONITOR:
                    this.fileName += "computer_monitor.txt";
                    break;
                case T_COMPUTER_KEYBOARD:
                    this.fileName += "computer_keyboard.txt";
                    break;
                case T_COMPUTER_MOUSE:
                    this.fileName += "computer_mouse.txt";
                    break;
                case T_DESK:
                    this.fileName += "desk.txt";
                    break;
                case T_LAPTOP:
                    this.fileName += "laptop.txt";
                    break;
                case T_MUG:
                    this.fileName += "mug.txt";
                    break;
                case T_WINDOW:
                    this.fileName += "window.txt";
                    break;
            }

            BufferedReader reader = null;
            try
            {
                // Extract policy state-action pairs from text file using regex
                Pattern pattern = Pattern.compile("(\\d+)\\s(\\d)\\s(1.0|0.25)");
                reader = new BufferedReader(new InputStreamReader(context.getAssets().open(fileName)));

                String line;
                while ((line = reader.readLine()) != null)
                {
                    // Save all non-zero prob actions into a hashmap to sample from later
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find())
                    {
                        if (Double.valueOf(matcher.group(3)) > 0.0)
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

        long getAction(State state)
        {
            // Draw random action from action set from policy
            Random rand = new Random();
            long s = state.getDecodedState();

            if (policy.get(s) == null)
            {
                Log.w(TAG, "Undefined state, executing random action");
                return rand.nextInt(4);
            }
            int nActions = policy.get(s).size();
            return policy.get(s).get(rand.nextInt(nActions));
        }
    }

    class State
    {
        private static final int NUM_OBJECTS = 9;
        private static final int MAX_STEPS = 12;

        private static final int S_OBS = 0;
        private static final int S_STEPS = 1;
        private static final int S_STATE_VISITED = 2;

        private long state;

        private ActivityBase.Observation observation = ActivityBase.Observation.O_NOTHING;

        private long steps = 0;
        private long stateVisted = 0;

        private int[] panHistory = new int[GRID_SIZE];
        private int[] tiltHistory = new int[GRID_SIZE];

        State()
        {
            for (int i = 0; i < GRID_SIZE; i++)
            {
                panHistory[i] = 0;
                tiltHistory[i] = 0;
            }
        }

        private long getDecodedState()
        {
            long state = 0;
            long multiplier = 1;

            state += (multiplier * observation.getCode());
            multiplier *= NUM_OBJECTS;
            state += (multiplier * steps);
            multiplier *= MAX_STEPS;
            state += (multiplier * stateVisted);

            return state;
        }

        private long[] getEncodedState()
        {
            long[] stateVector = new long[3];
            long state = this.state;

            stateVector[S_OBS] = state % NUM_OBJECTS;
            state /= NUM_OBJECTS;
            stateVector[S_STEPS] = state % MAX_STEPS;
            state /= MAX_STEPS;
            stateVector[S_STATE_VISITED] = state % 2;

            return stateVector;
        }

        private void addObservation(ActivityBase.Observation observation, float fpan, float ftilt)
        {
            // Origin is top right, not bottom left
            int pan = (int) ((Math.floor(Math.toDegrees(fpan) / ANGLE_INTERVAL)) + GRID_SIZE / 2 - 1);
            int tilt = (int) ((Math.floor(Math.toDegrees(ftilt) / ANGLE_INTERVAL)) + GRID_SIZE / 2 - 1);

            if (pan < 0) pan = GRID_SIZE - 1;
            if (pan > GRID_SIZE - 1) pan = 0;
            if (tilt < 0) tilt = GRID_SIZE - 1;
            if (tilt > GRID_SIZE - 1) tilt = 0;

            this.observation = observation;
            if (this.steps != MAX_STEPS - 1) this.steps++;

            if (panHistory[pan] == 1 && tiltHistory[tilt] == 1) this.stateVisted = 1;
            else this.stateVisted = 0;

            panHistory[pan] = 1;
            tiltHistory[tilt] = 1;

            this.state = getDecodedState();
        }
    }
}
