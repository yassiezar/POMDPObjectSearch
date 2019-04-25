package com.example.jaycee.pomdpobjectsearch;

import android.content.Context;

import com.example.jaycee.pomdpobjectsearch.helpers.VectorTools;
import com.example.jaycee.pomdpobjectsearch.policy.Belief;
import com.example.jaycee.pomdpobjectsearch.policy.POMDPPolicy;
import com.example.jaycee.pomdpobjectsearch.policy.State;
import com.example.jaycee.pomdpobjectsearch.policy.Model;

import static com.example.jaycee.pomdpobjectsearch.policy.State.S_STEPS;

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

    public static final int NUM_STATES = 360;
    public static final int NUM_ACTIONS = 4;
    private static final int HORIZON_DISTANCE = 50;
    public static final int GRID_SIZE = 6;
    public static final int ANGLE_INTERVAL = 20;

    private static ActionGenerator actionGenerator = null;

    private POMDPPolicy policy;
    private State state;
    private Belief belief;

    private int id = -1;

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

    public void setTarget(Objects.Observation target, Model model)
    {
        policy.setTarget(target, NUM_STATES);
        state = new State();
        belief = new Belief(NUM_STATES, model);
    }

    public VectorTools.PanAndTilt getAngleAdjustment(Objects.Observation obs, float camPan, float camTilt)
    {
        state.addObservation(obs, camPan, camTilt);
        POMDPPolicy.ActionId actionId;
        if(id == -1 || state.getEncodedState()[S_STEPS] > HORIZON_DISTANCE)
        {
            actionId = policy.getAction(belief.getBelief(), HORIZON_DISTANCE);
        }
        else
        {
            actionId = policy.getAction(id, state);
        }
        int action = actionId.action;
        belief.updateBeliefState(action, obs.getCode());

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
