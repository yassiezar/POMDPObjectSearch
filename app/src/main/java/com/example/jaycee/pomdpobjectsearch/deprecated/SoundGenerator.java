package com.example.jaycee.pomdpobjectsearch.deprecated;

import com.example.jaycee.pomdpobjectsearch.ActivityGuided;
import com.example.jaycee.pomdpobjectsearch.views.Arrow;
import com.example.jaycee.pomdpobjectsearch.JNIBridge;
import com.example.jaycee.pomdpobjectsearch.Metrics;
import com.example.jaycee.pomdpobjectsearch.helpers.VectorTools;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;

import android.content.Context;
import android.os.Handler;
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

@Deprecated
public class SoundGenerator implements Runnable
{
    private static final String TAG = SoundGenerator.class.getSimpleName();

    public interface NewWaypointHandler
    {
        void onNewWaypoint();
        void onTargetFound();
        void onArrowDirectionChange(Arrow.Direction direction);
    }

    private static final int O_NOTHING = 0;

    private static final double ANGLE_INTERVAL = 20;
    private static final int GRID_SIZE = 6;

    private Context context;

    private Frame frame;
    private Pose phonePose;
    private Waypoint waypoint;
    private Anchor waypointAnchor;
    private Session session;

    private long observation = O_NOTHING;
    private long prevCameraObservation = O_NOTHING;
    private long target = -1;
    private long timestamp;

    private Policy policy;

    private Metrics metrics = new Metrics();
    private State state;

    private NewWaypointHandler waypointHandler;

    private Toast toast;
    private Handler handler = new Handler();

    private boolean stop = false;
    private boolean isTargetSet = false;
    private boolean targetFound = false;


    SoundGenerator(Context context)
    {
        this.context = context;
        this.waypointHandler = (NewWaypointHandler)context;
    }

    public void stop()
    {
        this.stop = true;
        this.handler = null;
        this.waypointAnchor = null;
    }

    @Override
    public void run()
    {
        try
        {
            phonePose = frame.getAndroidSensorPose();

            if (!isTargetSet() || isTargetFound())
            {
                if (!stop) handler.postDelayed(this, 40);
                return;
            }

/*            if (!renderer.isRendererReady())
            {
                if (!stop) handler.postDelayed(this, 40);
                return;
            }*/

            float gain = 1.f;
            if (observation == target)
            {
                targetFound = true;
                isTargetSet = false;
                observation = O_NOTHING;
                gain = 0.f;

                waypointHandler.onTargetFound();
                waypointAnchor.detach();
                waypoint = null;
                state = null;
            }

            if (waypoint != null)
            {
                // Get Euler angles from vector wrt axis system
                // pitch = tilt, yaw = pan
                VectorTools.mVector cameraVector = getCameraVector(phonePose);
                float[] phoneRotationAngles = cameraVector.getEuler();
                float cameraPan = phoneRotationAngles[2];
                float cameraTilt = phoneRotationAngles[1];
                long newCameraObservation = this.observation;

                // Get current state and generate new waypoint if agent is in new state or sees new object
/*            Log.d(TAG, String.format("current pan %f tilt %f ", cameraPan, cameraTilt));
            Log.d(TAG, String.format("Object: %d Step: %d Visited: %d", state.getEncodedState()[0], state.getEncodedState()[1], state.getEncodedState()[2]));
            Log.d(TAG, String.format("x: %f y %f", Math.toDegrees(cameraPan), Math.toDegrees(cameraTilt)));*/
                if (waypoint.waypointReached(cameraPan, cameraTilt) || (newCameraObservation != prevCameraObservation && newCameraObservation != O_NOTHING))
                {
                    if (waypointAnchor != null)
                    {
                        waypointAnchor.detach();
                    }

                    long action = policy.getAction(state);
                    //Log.d(TAG, String.format("Object found or found waypoint, action: %d", action));
                    long[] testState = state.getEncodedState();
                    // Log.i(TAG, String.format("State %d obs %d steps %d prev %d", state.getDecodedState(), testState[0], testState[1], testState[2]));
                    waypoint.updateWaypoint(-cameraPan, cameraTilt, action);
                    waypointAnchor = session.createAnchor(waypoint.getPose());
                    prevCameraObservation = newCameraObservation;
                    state.addObservation(newCameraObservation, cameraPan, cameraTilt);
                    // Log.i(TAG, "Setting new waypoint");
                }
                VectorTools.mVector waypointVector = new VectorTools.mVector(waypoint.pose.getTranslation());
                float[] waypointRotationAngles = waypointVector.getEuler();
                float waypointTilt = waypointRotationAngles[1];

                if (waypointTilt > Math.PI / 2)
                {
                    waypointTilt -= (float) Math.PI;
                }
                else if (waypointTilt < Math.PI / 2)
                {
                    waypointTilt += (float) Math.PI;
                }

                // Set direction arrow
                VectorTools.mVector vectorToWaypoint = waypointVector.translate(cameraVector);
/*            Log.d(TAG, String.format("x %f y %f z %f", vectorToWaypoint.x, vectorToWaypoint.y, vectorToWaypoint.z));
            Log.d(TAG, String.format("x %f y %f z %f", cameraVector.x, cameraVector.y, cameraVector.z));
            Log.d(TAG, String.format("x %f y %f z %f", waypointVector.x, waypointVector.y, waypointVector.z));*/
                ((ActivityGuided) context).getCentreView().resetArrows();
                if (vectorToWaypoint.x > 0.1)
                {
                    ((ActivityGuided) context).getCentreView().setArrowAlpha(Arrow.Direction.RIGHT, 255);
                }
                else if (vectorToWaypoint.x < -0.1)
                {
                    ((ActivityGuided) context).getCentreView().setArrowAlpha(Arrow.Direction.LEFT, 255);
                }
                if (vectorToWaypoint.y > 0.1)
                {
                    ((ActivityGuided) context).getCentreView().setArrowAlpha(Arrow.Direction.UP, 255);
                }
                else if (vectorToWaypoint.y < -0.1)
                {
                    ((ActivityGuided) context).getCentreView().setArrowAlpha(Arrow.Direction.DOWN, 255);
                }

                float elevationAngle = cameraTilt + waypointTilt;
                float pitch = getPitch(elevationAngle);

                // Log.d(TAG, String.format("waypoint x %f old %f", vectorToWaypoint.x, waypoint.getPose().getTranslation()[0]));
                //Log.d(TAG, String.format("waypoint tilt %f phone tilt %f", waypointTilt, cameraTilt));
                //Log.d(TAG, String.format("Gain %f elevation %f pitch %f", gain, elevationAngle, pitch));
                JNIBridge.playSoundFFFF(vectorToWaypoint.x, phonePose.getTranslation(), gain, pitch);

                // Interlace second tone to notify user that target is close
                float targetSize = 0.1f;
                float volumeGrad = -1 / targetSize;
                float volumeMax = 1f;
                gain = 0.f;
                if (elevationAngle < targetSize && elevationAngle > 0)
                {
                    gain = volumeGrad * (elevationAngle) + volumeMax;
                }
                else if (elevationAngle > -targetSize && elevationAngle < 0)
                {
                    gain = -volumeGrad * (elevationAngle) + volumeMax;
                }
                Log.d(TAG, String.format("Gain %f elevation %f pitch %f", gain, elevationAngle, pitch));
                JNIBridge.playSoundFF(gain, pitch * 2);

                metrics.updateWaypointPosition(waypoint.getPose());
                metrics.updatePhonePose(phonePose);
                metrics.updateTimestamp(timestamp);
                metrics.writeWifi();
            }
        }
        catch(NullPointerException e)
        {
            Log.d(TAG, "Frame not initialised");
        }

        if (!stop) handler.postDelayed(this, 40);
    }

    private VectorTools.mVector getCameraVector(Pose pose)
    {
        // Get rotation angles and convert to pan/tilt angles
        // Start by rotating vector by quaternion (camera vector = -z)
        VectorTools.mQuaternion phoneRotationQuaternion = new VectorTools.mQuaternion(pose.getRotationQuaternion());
        phoneRotationQuaternion.normalise();
        VectorTools.mVector vector = new VectorTools.mVector(0.f, 0.f, 1.f);
        vector.rotateByQuaternion(phoneRotationQuaternion);

        return vector;
    }

    public void setTarget(long target)
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
        this.isTargetSet = true;
        this.targetFound = false;

        prevCameraObservation = O_NOTHING;

        metrics.updateTarget(target);

        waypointHandler.onNewWaypoint();
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
            pitch = (float)(Math.pow(2, 6));
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
        // Log.d(TAG, String.format("pitch: %f", pitch));

        return pitch;

        // Compensate for the Tango's default position being 90deg upright
/*        elevation -= Math.PI / 2;*/
    }

    public void setObservation(long observation)
    {
        final String val;
        if(observation == 1)
        {
            val = "Monitor";
        }
        else if(observation == 2)
        {
            val = "Keyboard";
        }
        else if(observation == 3)
        {
            val = "Mouse";
        }
        else if(observation == 4)
        {
            val = "Desk";
        }
        else if(observation == 5)
        {
            val = "Laptop";
        }
        else if(observation == 6)
        {
            val = "Mug";
        }
        else if(observation == 7)
        {
            val = "Office supplies";
        }
        else if(observation == 8)
        {
            val = "Window";
        }
        else
        {
            val = "Unknown";
        }
        this.observation = observation;
        this.metrics.updateObservation(observation);

        if(observation != O_NOTHING && observation != -1)
        {
            ((ActivityGuided)context).runOnUiThread(new Runnable()
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

    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setFrame(Frame frame) { this.frame = frame; }
    public void setSession(Session session) { this.session = session; }

    public boolean isTargetSet() { return this.isTargetSet; }
    public boolean isTargetFound() { return this.targetFound; }
    public Anchor getWaypointAnchor() { return this.waypointAnchor; }
    public long getTarget() { return this.target; }

    class Waypoint
    {
        private Pose pose;

        Waypoint(Pose pose)
        {
            float[] phoneTranslation = pose.getTranslation();
            this.pose = new Pose(new float[] {phoneTranslation[0], phoneTranslation[1], phoneTranslation[2] - 1.f}, pose.getRotationQuaternion());
        }

        Pose getPose() { return pose; }

        void updateWaypoint(float fpan, float ftilt, long action)
        {
            float[] wayPointTranslation = new float[3];
            //long[] stateVector = state.getEncodedState();

            // Assume the current waypoint is where the camera is pointing.
            // Reasonable since this function only called when pointing to new target
            // Discretise pan/tilt into grid
            int pan = (int)((Math.floor(Math.toDegrees(fpan)/ANGLE_INTERVAL)) + GRID_SIZE/2 - 1);
            int tilt = (int)((Math.floor(Math.toDegrees(ftilt)/ANGLE_INTERVAL)) + GRID_SIZE/2 - 1);

            Log.i(TAG, String.format("x: %f y %f", Math.toDegrees(fpan), Math.toDegrees(ftilt)));
            Log.i(TAG, String.format("raw pan %f raw tilt %f", Math.toDegrees(fpan)/ANGLE_INTERVAL + GRID_SIZE/2 - 1, Math.toDegrees(ftilt)/ANGLE_INTERVAL + GRID_SIZE/2 - 1));
            Log.i(TAG, String.format("old pan: %d old tilt %d", pan, tilt));

            /*waypointVector.x /= waypointVector.z;
            waypointVector.y /= waypointVector.z;
            waypointVector.z /= waypointVector.z;*/

            if(action == Policy.A_LEFT)
            {
                pan -= 1;
            }
            else if(action == Policy.A_RIGHT)
            {
                pan += 1;
            }
            else if(action == Policy.A_UP)
            {
                tilt += 1;
            }
            else if(action == Policy.A_DOWN)
            {
                tilt -= 1;
            }

            // Wrap the world
            if(pan < 0) pan = GRID_SIZE-1;
            if(pan > GRID_SIZE-1) pan = 0;
            if(tilt < 0) tilt = GRID_SIZE-1;
            if(tilt > GRID_SIZE-1) tilt = 0;

            float z =  phonePose.getTranslation()[2] - 1.f;
            wayPointTranslation[0] = (float)Math.sin(Math.toRadians(ANGLE_INTERVAL*(pan - GRID_SIZE/2.0 + 1)));
            wayPointTranslation[1] = (float)Math.sin(Math.toRadians(ANGLE_INTERVAL*(tilt - GRID_SIZE/2.0 + 1)));
            wayPointTranslation[2] = z;

            // Log.i(TAG, String.format("new pan: %d new tilt: %d", pan, tilt));
            //Log.i(TAG, String.format("translation x %f translation y: %f", wayPointTranslation[0], wayPointTranslation[1]));

            pose = new Pose(wayPointTranslation, new float[]{0.f, 0.f, 0.f, 1.f});
        }

        private boolean waypointReached(float pan, float tilt)
        {
            float x = pose.getTranslation()[0];
            float y = pose.getTranslation()[1];

            // Log.d(TAG, String.format("x: %f y %f", Math.sin(pan) - x, Math.sin(tilt) - y));
            // Compensate for Z-axis going in negative direction, rotating pan around y-axis
            return Math.abs(Math.sin(tilt) - y) < 0.1 && Math.abs(Math.cos(-pan+Math.PI/2) - x) < 0.1;
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
            int pan = (int)((Math.floor(Math.toDegrees(fpan)/ANGLE_INTERVAL)) + GRID_SIZE/2 - 1);
            int tilt = (int)((Math.floor(Math.toDegrees(ftilt)/ANGLE_INTERVAL)) + GRID_SIZE/2 - 1);

            if(pan < 0) pan = GRID_SIZE - 1;
            if(pan > GRID_SIZE - 1) pan = 0;
            if(tilt < 0) tilt = GRID_SIZE - 1;
            if(tilt > GRID_SIZE - 1) tilt = 0;

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
        private static final int O_COMPUTER_MONITOR = 1;
        private static final int O_COMPUTER_KEYBOARD = 2;
        private static final int O_COMPUTER_MOUSE = 3;
        private static final int O_DESK = 4;
        private static final int O_LAPTOP = 5;
        private static final int O_MUG = 6;
        private static final int O_OFFICE_SUPPLIES = 7;
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
