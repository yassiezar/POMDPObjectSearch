package com.example.jaycee.pomdpobjectsearch;

import com.example.jaycee.pomdpobjectsearch.helpers.ClassHelpers;
import com.example.jaycee.pomdpobjectsearch.rendering.SurfaceRenderer;
import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
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

public class SoundGenerator implements Runnable
{
    private static final String TAG = SoundGenerator.class.getSimpleName();

    private static final int O_NOTHING = 0;

    private Context context;
    private SurfaceRenderer renderer;

    private Pose phonePose;
    private Waypoint waypoint;
    private Pose offsetPose;
    private Anchor waypointAnchor;
    private Session session;

    private long observation = O_NOTHING;
    private long prevCameraObservation = O_NOTHING;
    private long target = -1;

    private Policy policy;

    private Metrics metrics = new Metrics();
    private State state;

    private Vibrator vibrator;
    private Toast toast;
    private Handler handler = new Handler();

    private boolean stop = false;
    private boolean targetSet = false;
    private boolean targetFound = false;


    SoundGenerator(Context context, SurfaceRenderer renderer)
    {
        this.context = context;
        this.renderer = renderer;

        this.vibrator= (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void stop()
    {
        this.stop = true;
        handler = null;
    }

    @Override
    public void run()
    {
        phonePose = renderer.getDevicePose();
        this.session = renderer.getSession();

        if(!isTargetSet() || !isTargetFound())
        {
            if(!stop) handler.postDelayed(this, 40);
            return;
        }

        if(!renderer.isRendererReady())
        {
            if(!stop) handler.postDelayed(this, 40);
            return;
        }

        setObservation(((ActivityCamera)context).currentBarcodeScan());

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
        Log.d(TAG, String.format("current pan %f tilt %f ", cameraPan, cameraTilt));
        Log.i(TAG, String.format("Object: %d Step: %d Visited: %d", state.getEncodedState()[0], state.getEncodedState()[1], state.getEncodedState()[2]));
        if(waypoint.waypointReached(cameraPan, cameraTilt) || (newCameraObservation != prevCameraObservation && newCameraObservation != O_NOTHING))
        {
            long action = policy.getAction(state);
            Log.i(TAG, String.format("Object found or found waypoint, action: %d", action));
            waypoint.updateWaypoint(phonePose, state, action);
            waypointAnchor = session.createAnchor(waypoint.getPose());
            prevCameraObservation = newCameraObservation;
            state.addObservation(newCameraObservation, cameraPan, cameraTilt);
        }
        ClassHelpers.mVector waypointVector = getRotation(waypoint.getPose(), false);
        float[] waypointRotationAngles = waypointVector.getEuler();
        float waypointTilt = waypointRotationAngles[1];

        JNIBridge.playSound(waypoint.getPose().getTranslation(), cameraVector.asFloat(), gain, getPitch(waypointTilt - cameraTilt));

        metrics.writeWifi();
        if(!stop) handler.postDelayed(this, 40);
    }

    private ClassHelpers.mVector getRotation(Pose pose, boolean isWaypointPose)
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

        if(!isWaypointPose)
        {
            vector.x -= offsetVector.x;
            vector.y -= offsetVector.y;
        }

        return vector;
    }

    public void setTarget(long target, long observation)
    {
        policy = new Policy((int)target);
        state = new State();

        if(waypoint == null)
        {
            waypoint = new Waypoint(phonePose);
        }

        if(waypointAnchor == null)
        {
            waypointAnchor = session.createAnchor(waypoint.getPose());
        }

        this.target = target;
        this.targetSet = true;
        this.targetFound = false;

        prevCameraObservation = observation;

        metrics.updateTarget(target);

        renderer.setDrawWaypoint(true);
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

    public void setObservation(long observation)
    {
        /* TODO: Translate between barcode and state encoding for object observations */
        final String val;
        if(observation == 3)
        {
            val = "Mouse";
        }
        else if(observation == 5)
        {
            val = "Laptop";
        }
        else if(observation == 2)
        {
            val = "Keyboard";
        }
        else if(observation == 1)
        {
            val = "Monitor";
        }
        else if(observation == 8)
        {
            val = "Window";
        }
        else if(observation == 4)
        {
            val = "Desk";
        }
        else if(observation == 6)
        {
            val = "Mug";
        }
        else if(observation == 7)
        {
            val = "Office supplies";
        }
        else
        {
            val = "Unknown";
        }
        this.observation = observation;
        metrics.updateObservation(observation);

        if(observation != O_NOTHING && observation != -1)
        {
            ((ActivityCamera)context).runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    if(toast != null)
                    {
                        toast.cancel();
                    }
                    toast = Toast.makeText(context, val, Toast.LENGTH_SHORT);
                    toast.show();
                }
            });
        }
    }

    public void markOffsetPose() { this.offsetPose = phonePose; }
    public boolean isTargetSet() { return this.targetSet; }
    public boolean isTargetFound() { return this.targetFound; }
    public long getTarget() { return this.target; }
    public Anchor getWaypointAnchor() { return this.waypointAnchor; }

    class Waypoint
    {
        private static final long ANGLE_INTERVAL = 15;

        private Pose pose;

        Waypoint(Pose pose)
        {
            float[] phoneTranslation = pose.getTranslation();
            this.pose = new Pose(new float[] {phoneTranslation[0], phoneTranslation[1], phoneTranslation[2] - 1.f}, pose.getRotationQuaternion());
        }

        Pose getPose() { return pose; }

        void updateWaypoint(Pose phonePose, State state, long action)
        {
            float[] wayPointTranslation = new float[3];
            long[] stateVector = state.getEncodedState();

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
                stateVector[0] += 1;
            }
            if(action == Policy.A_RIGHT)
            {
                wayPointTranslation[0] = waypointVector.x + 1.f*(float)Math.sin(Math.toRadians(ANGLE_INTERVAL));
                wayPointTranslation[1] = waypointVector.y;
                stateVector[0] -= 1;
            }

            if(action == Policy.A_UP)
            {
                wayPointTranslation[0] = waypointVector.x;
                wayPointTranslation[1] = waypointVector.y + 1.f*(float)Math.sin(Math.toRadians(ANGLE_INTERVAL));
                stateVector[1] -= 1;
            }
            if(action == Policy.A_DOWN)
            {
                wayPointTranslation[0] = waypointVector.x;
                wayPointTranslation[1] = waypointVector.y - 1.f*(float)Math.sin(Math.toRadians(ANGLE_INTERVAL));
                stateVector[1] += 1;
            }

            // Wrap the world
            if(Math.asin(wayPointTranslation[1]) > Math.PI/4)
            {
                wayPointTranslation[1] = -(float)Math.sin(Math.sin(Math.PI/4));
            }
            if(Math.asin(wayPointTranslation[1]) < -Math.PI/4)
            {
                wayPointTranslation[1] = (float)Math.sin(Math.sin(Math.PI/4));
            }
            if(Math.asin(wayPointTranslation[0]) > Math.PI/4)
            {
                wayPointTranslation[0] = -(float)Math.sin(Math.sin(Math.PI/4));
            }
            if(Math.asin(wayPointTranslation[0]) < -Math.PI/4)
            {
                wayPointTranslation[0] = (float)Math.sin(Math.sin(Math.PI/4));
            }
            Log.i(TAG, String.format("Current pan: %f Current tilt: %f", Math.asin(wayPointTranslation[0]), Math.asin(wayPointTranslation[1])));

            wayPointTranslation[2] = phonePose.getTranslation()[2] - 1.f;

            pose = new Pose(wayPointTranslation, phonePose.getRotationQuaternion());
        }

        private boolean waypointReached(float pan, float tilt)
        {
            float x = pose.getTranslation()[0];
            float y = pose.getTranslation()[1];

            Log.i(TAG, String.format("x: %f y %f", Math.cos(pan+Math.PI/2) - x, Math.sin(-tilt) - y));
            // Compensate for Z-axis going in negative direction, rotating pan around y-axis
            return Math.abs(Math.sin(-tilt) - y) < 0.1 && Math.abs(Math.cos(pan+Math.PI/2) - x) < 0.1;
        }
    }

    class State
    {
        private static final int ANGLE_INTERVAL = 15;
        private static final int GRID_SIZE = 6;

        private static final int NUM_OBJECTS = 9;
        private static final int MAX_STEPS = 12;

        private static final int S_OBS = 0;
        private static final int S_STEPS = 1;
        private static final int S_STATE_VISITED = 2;

        private long state;

        private long observation = 0;
        private long steps = 0;
        private long stateVisted = 0;

        private int[] panHistory = new int[GRID_SIZE];
        private int[] tiltHistory = new int[GRID_SIZE];

        State()
        {
            for(int i = 0; i < GRID_SIZE; i ++)
            {
                panHistory[i] = 0;
                tiltHistory[i] = 0;
            }
        }

        private long getDecodedState()
        {
            long state = 0;
            long multiplier = 1;

            state += (multiplier * observation);
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

        private void addObservation(long observation, float fpan, float ftilt)
        {
            // Origin is top right, not bottom left
            int pan = (int) (-Math.round(Math.toDegrees(-fpan) / ANGLE_INTERVAL) + GRID_SIZE / 2 - 1);
            int tilt = (int) (-Math.round(Math.toDegrees(-ftilt) / ANGLE_INTERVAL) + GRID_SIZE / 2 - 1);

            this.observation = observation;
            if(this.steps != MAX_STEPS-1) this.steps ++;

            if(panHistory[pan] == 1 && tiltHistory[tilt] == 1) this.stateVisted = 1;
            else this.stateVisted = 0;

            panHistory[pan] = 1;
            tiltHistory[tilt] = 1;

            this.state = getDecodedState();
        }
    }

    class Policy
    {
        private static final int O_MUG = 6;
        private static final int O_LAPTOP = 5;
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

        long getAction(State state)
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
}
