package com.example.jaycee.pomdpobjectsearch;

import android.content.Context;
import android.util.Log;

import com.example.jaycee.pomdpobjectsearch.helpers.VectorTools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionGenerator
{
    private enum Actions
    {
        A_UP(0),
        A_DOWN(1),
        A_LEFT(2),
        A_RIGHT(3);

        private int code;
        Actions(int code)
        {
            this.code = code;
        }

        public int getCode() { return this.code; }
    }
    private static final String TAG = ActionGenerator.class.getSimpleName();

    private static ActionGenerator actionGenerator = null;

    private static final int GRID_SIZE = 6;
    private static final int ANGLE_INTERVAL = 20;

    private Policy policy;
    private State state;

    private ActionGenerator(Context context)
    {
        policy = new Policy(context);
    }

    public synchronized static ActionGenerator create(final Context context)
    {
        if(actionGenerator != null)
        {
            throw new AssertionError("Already initialised action generator");
        }

        actionGenerator = new ActionGenerator(context);

        return actionGenerator;
    }

    public static ActionGenerator getInstnce()
    {
        if(actionGenerator == null)
        {
            throw new AssertionError("Action generator not yet initialised");
        }

        return actionGenerator;
    }

    public void setTarget(Objects.Observation target)
    {
        policy.setTarget(target);
        state = new State();
    }

    public VectorTools.PanAndTilt getAngleAdjustment(Objects.Observation obs, float camPan, float camTilt)
    {
        state.addObservation(obs, camPan, camTilt);
        long action = policy.getAction(state);

        // Get new angles
        int pan = (int) ((Math.floor(Math.toDegrees(camPan) / ANGLE_INTERVAL)) + GRID_SIZE / 2 - 1);
        int tilt = (int) ((Math.floor(Math.toDegrees(camTilt) / ANGLE_INTERVAL)) + GRID_SIZE / 2 - 1);

        if (action == Actions.A_LEFT.getCode())
        {
            pan -= 1;
        }
        else if (action == Actions.A_RIGHT.getCode())
        {
            pan += 1;
        }
        else if (action == Actions.A_UP.getCode())
        {
            tilt += 1;
        }
        else if (action == Actions.A_DOWN.getCode())
        {
            tilt -= 1;
        }

        if (pan < 0) pan = GRID_SIZE - 1;
        if (pan > GRID_SIZE - 1) pan = 0;
        if (tilt < 0) tilt = GRID_SIZE - 1;
        if (tilt > GRID_SIZE - 1) tilt = 0;

        VectorTools.PanAndTilt newAngles = new VectorTools.PanAndTilt();
        newAngles.pan = ANGLE_INTERVAL * (pan - GRID_SIZE / 2.0 + 1);
        newAngles.tilt = ANGLE_INTERVAL * (tilt - GRID_SIZE / 2.0 + 1);

        return newAngles;
    }

    class Policy
    {
        private String fileName = "MDPPolicies/sarsa_";

        private Map<Long, ArrayList<Long>> policy = new HashMap<>();

        private Context context;

        public Policy(Context context)
        {
            this.context = context;
        }

        public void setTarget(Objects.Observation target)
        {
            BufferedReader reader = null;
            try
            {
                fileName += target.getFileName();
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

        private Objects.Observation observation = Objects.Observation.O_NOTHING;

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

        private void addObservation(Objects.Observation observation, float fpan, float ftilt)
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
