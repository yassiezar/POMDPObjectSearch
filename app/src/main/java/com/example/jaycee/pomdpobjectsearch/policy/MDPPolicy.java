package com.example.jaycee.pomdpobjectsearch.policy;

import android.content.Context;
import android.util.Log;

import com.example.jaycee.pomdpobjectsearch.Objects;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MDPPolicy
{
    private static final String TAG = MDPPolicy.class.getSimpleName();

    private Map<Long, ArrayList<Long>> policy = new HashMap<>();

    private Context context;

    public MDPPolicy(Context context)
    {
        this.context = context;
    }

    public void setTarget(Objects.Observation target, int numStates)
    {
        BufferedReader reader = null;
        try
        {
            String fileName = "MDPPolicies/sarsa_" + target.getFileName();
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

    public long getAction(State state)
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
