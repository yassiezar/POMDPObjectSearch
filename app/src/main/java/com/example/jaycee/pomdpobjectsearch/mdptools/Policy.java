package com.example.jaycee.pomdpobjectsearch.mdptools;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.jaycee.pomdpobjectsearch.mdptools.Params.O_COMPUTER_KEYBOARD;
import static com.example.jaycee.pomdpobjectsearch.mdptools.Params.O_COMPUTER_MONITOR;
import static com.example.jaycee.pomdpobjectsearch.mdptools.Params.O_COMPUTER_MOUSE;
import static com.example.jaycee.pomdpobjectsearch.mdptools.Params.O_DESK;
import static com.example.jaycee.pomdpobjectsearch.mdptools.Params.O_LAPTOP;
import static com.example.jaycee.pomdpobjectsearch.mdptools.Params.O_MUG;
import static com.example.jaycee.pomdpobjectsearch.mdptools.Params.O_OFFICE_SUPPLIES;
import static com.example.jaycee.pomdpobjectsearch.mdptools.Params.O_WINDOW;

public class Policy
{
    private static final String TAG = Policy.class.getSimpleName();

    private String fileName = "MDPPolicies/sarsa_";

    private Map<Long, ArrayList<Long>> policy = new HashMap<>();

    public Policy(Context context, int target)
    {
        switch(target)
        {
            case O_COMPUTER_MONITOR:
                this.fileName += "computer_monitor.txt";
                break;
            case O_COMPUTER_KEYBOARD:
                this.fileName += "computer_keyboard.txt";
                break;
            case O_COMPUTER_MOUSE:
                this.fileName += "computer_mouse.txt";
                break;
            case O_DESK:
                this.fileName += "desk.txt";
                break;
            case O_LAPTOP:
                this.fileName += "laptop.txt";
                break;
            case O_MUG:
                this.fileName += "mug.txt";
                break;
            case O_OFFICE_SUPPLIES:
                this.fileName += "office_supplies.txt";
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
            reader = new BufferedReader(new InputStreamReader(context.getResources().getAssets().open(fileName)));

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

    public long getAction(State state)
    {
        // Draw random action from action set from policy
        Random rand = new Random();
        long s = state.getDecodedState();

        if(policy.get(s) == null)
        {
            Log.w(TAG, "Undefined state, executing random action");
            return rand.nextInt(4);
        }
        int nActions = policy.get(s).size();
        return policy.get(s).get(rand.nextInt(nActions));
    }
}
