package com.example.jaycee.pomdpobjectsearch;

import android.content.Context;
import android.util.Log;

import com.example.jaycee.pomdpobjectsearch.helpers.VectorTools;
import com.example.jaycee.pomdpobjectsearch.policy.Belief;
import com.example.jaycee.pomdpobjectsearch.policy.POMDPPolicy;
import com.example.jaycee.pomdpobjectsearch.policy.State;
import com.example.jaycee.pomdpobjectsearch.policy.Model;

import static com.example.jaycee.pomdpobjectsearch.App.ANGLE_INTERVAL;
import static com.example.jaycee.pomdpobjectsearch.App.A_DOWN;
import static com.example.jaycee.pomdpobjectsearch.App.A_LEFT;
import static com.example.jaycee.pomdpobjectsearch.App.A_RIGHT;
import static com.example.jaycee.pomdpobjectsearch.App.A_UP;
import static com.example.jaycee.pomdpobjectsearch.App.GRID_SIZE;
import static com.example.jaycee.pomdpobjectsearch.App.HORIZON_DISTANCE;
import static com.example.jaycee.pomdpobjectsearch.policy.State.S_STEPS;

public class ActionGenerator
{
    private static final String TAG = ActionGenerator.class.getSimpleName();

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
            Log.w(TAG, "Already initialised action generator");
            return actionGenerator;
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

    public boolean setTarget(Objects.Observation target, Model model)
    {
        if(!policy.setTarget(target))
        {
            return false;
        }
        state = new State();
        belief = new Belief(model);

        return true;
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

        if (action == A_LEFT.code)
        {
            pan -= 1;
        }
        else if (action == A_RIGHT.code)
        {
            pan += 1;
        }
        else if (action == A_UP.code)
        {
            tilt += 1;
        }
        else if (action == A_DOWN.code)
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
