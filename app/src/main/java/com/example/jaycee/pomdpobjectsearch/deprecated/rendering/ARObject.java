package com.example.jaycee.pomdpobjectsearch.deprecated.rendering;

import com.example.jaycee.pomdpobjectsearch.helpers.VectorTools;
import com.google.ar.core.Pose;

@Deprecated
public class ARObject
{
    private static final float ANGLE_INTERVAL = 12f;

    private Pose pose, rotatedPose;

    private String label;

    public ARObject(int x, int y, String label)
    {
        // Set angle to zero if x/y = 0 to avoid zero-division
        // Centre angle by subtracting x/y from 6 (centre of 12x12 grid)
        float xf, yf;
        float yOffset = 0.0f;
        xf = (float)Math.sin(Math.toRadians((5 - x)*ANGLE_INTERVAL));
        yf = (float)Math.sin(Math.toRadians((5 - y)*ANGLE_INTERVAL))+yOffset;

        float theta = (float)Math.atan2(yf, xf);

        float phi = (float)Math.asin(xf / Math.cos(theta));

        float zf = (float)Math.cos(phi);

        float[] translation = {xf, yf, -zf};
        float[] rotation = {0, 0, 0, 1};

        this.pose = new Pose(translation, rotation);
        this.label = label;
    }

    public void getRotatedObject(Pose devicePose)
    {
        float[] rotation = devicePose.getRotationQuaternion();
        float[] centre = this.pose.getTranslation();

        VectorTools.mQuaternion phoneRotationQuaternion = new VectorTools.mQuaternion(devicePose.getRotationQuaternion());
        phoneRotationQuaternion.normalise();
        VectorTools.mVector objectVector = new VectorTools.mVector(centre);
        objectVector.rotateByQuaternion(phoneRotationQuaternion);
        objectVector.normalise();

        centre[0] = -objectVector.x;
        centre[1] = -objectVector.y;
        centre[2] = objectVector.z;

        rotatedPose = new Pose(centre, rotation);
    }

    public String getLabel() { return this.label; }
    public Pose getPose() { return this.pose; }
    public Pose getRotatedPose() { return this.rotatedPose; }
}
