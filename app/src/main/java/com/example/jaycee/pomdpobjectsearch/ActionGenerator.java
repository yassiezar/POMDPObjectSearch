package com.example.jaycee.pomdpobjectsearch;

import android.content.Context;

import com.example.jaycee.pomdpobjectsearch.helpers.VectorTools;
import com.example.jaycee.pomdpobjectsearch.policy.POMDPPolicy;
import com.example.jaycee.pomdpobjectsearch.policy.Policy;
import com.example.jaycee.pomdpobjectsearch.policy.State;

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

    public static final int GRID_SIZE = 6;
    public static final int ANGLE_INTERVAL = 20;

    private Policy policy;
    private State state;

    private ActionGenerator(Context context)
    {
        policy = new POMDPPolicy(context);
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

    public static ActionGenerator getInstance()
    {
        if(actionGenerator == null)
        {
            throw new AssertionError("Action generator not yet initialised");
        }

        return actionGenerator;
    }

    public void setTarget(Objects.Observation target)
    {
        policy.setTarget(target, 5);
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
}
