package com.example.jaycee.pomdpobjectsearch;

import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;

public class RunnableSoundGenerator implements Runnable
{
    private static final long ANGLE_INTERVAL = 30;

    private Pose phonePose;
    private Pose targetPose;
    private Anchor anchorTarget;

    private boolean targetReached = false;

    private long observation = 0;

    @Override
    public void run()
    {
        float[] currentPhoneRotation = convertQuaternionToEuler(phonePose.getRotationQuaternion());
        float[] targetRotation = convertQuaternionToEuler(targetPose.getRotationQuaternion());

        if(Math.abs(currentPhoneRotation[0] - targetRotation[0]) <= 3 ||
                Math.abs(currentPhoneRotation[1] - targetRotation[1]) <= 3)
        {
            targetReached = true;
            observation = 0;
        }
    }

    public void update(Camera camera, Session session)
    {
        phonePose = camera.getPose();

        if(targetReached)
        {
            targetReached = false;
            anchorTarget.detach();

            float[] angles = convertQuaternionToEuler(phonePose.getRotationQuaternion());

            long nextState = JNIBridge.getAction(decodeState(angles[0], angles[1], observation));
            long[] action = encodeState(nextState);
            float roll = action[0] * ANGLE_INTERVAL;
            float pitch = action[1] * ANGLE_INTERVAL;

            float[] nextTargetRotation = convertEulerToQuaternion(roll, pitch, 0);

            targetPose = new Pose(new float[] {0.f, 0.f, 0.f}, nextTargetRotation);

            anchorTarget = session.createAnchor(targetPose);
        }
    }

    public long decodeState(float pan, float tilt, float obs)
    {
        return 0;
    }

    public long[] encodeState(long state)
    {
        return new long[] {0, 0, 0};
    }

    public float[] convertEulerToQuaternion(float roll, float pitch, float yaw)
    {
        return new float[] {0.f, 0.f, 0.f, 0.f};
    }

    public float[] convertQuaternionToEuler(float[] q)
    {
        float sinr = 2.0f * (q[3]  * q[0] + q[1] * q[2]);
        float cosr = 1.0f - 2.0f * (q[0] * q[0] + q[1] * q[1]);

        float roll = (float)Math.atan2(sinr, cosr);

        float sinp = 2.f * (q[3] * q[1] - q[2] * q[0]);
        float pitch = 0;
        if(Math.abs(sinp) >= 1.f)
        {
            pitch = (float)Math.copySign(Math.PI / 2, sinp);
        }
        else
        {
            pitch = (float)Math.asin(sinp);
        }

        float siny = 2.f * (q[3] * q[2] + q[0] * q[1]);
        float cosy = 1.f - 2.f * (q[1] * q[1] + q[2] * q[2]);
        float yaw = (float)Math.atan2(siny, cosy);

        return new float[] {roll, pitch, yaw};
    }
}
