package com.example.jaycee.pomdpobjectsearch;

public class ClassHelpers
{
    private static final String TAG = ClassHelpers.class.getSimpleName();

    public static class mQuaternion
    {
        public float x, y, z, w;

        public mQuaternion(float x, float y, float z, float theta)
        {
            this.x = (float)(x * Math.sin(theta/2));
            this.y = (float)(y * Math.sin(theta/2));
            this.z = (float)(z * Math.sin(theta/2));
            this.w = (float)(Math.cos(theta));
        }

        public mQuaternion(float[] q)
        {
            this.x = q[0];
            this.y = q[1];
            this.z = q[2];
            this.w = q[3];
        }

        /*
        public float[] getEuler()
        {
            float roll, pitch, yaw;
            if(this.x*this.y + this.z*this.w == 0.5)
            {
                yaw = (float)(2 * Math.atan2(this.x, this.w));
                roll = 0;
            }

            else if(this.x*this.y + this.z*this.w == -0.5)
            {
                yaw = (float)(-2 * Math.atan2(this.x, this.w));
                roll = 0;
            }
            else
            {
                // yaw (y-axis rotation)
                yaw = (float)Math.atan2(2*this.y*this.w - 2*this.x*this.z, 1 - 2*this.y*this.y - 2*this.z*this.z);

                // roll (z-axis rotation)
                roll = (float)Math.atan2(2*this.x*this.w - 2*this.y*this.z, 1 - 2*this.x*this.x - 2*this.z*this.z);

            }

            // pitch (x-axis rotation)
            pitch = (float)Math.asin(2*this.x*this.y + 2*this.z*this.w);

            return new float[] {(float)(Math.toDegrees(roll)), (float)(Math.toDegrees(pitch)), (float)(Math.toDegrees(yaw))};
        }
        */

        public void normalise()
        {
            float fact = (float)Math.sqrt(x*x + y*y + z*z + w*w);
            this.x /= fact;
            this.y /= fact;
            this.z /= fact;
            this.w /= fact;
        }

        public void multiply(mQuaternion r)
        {
            float tmpx = r.x*this.w + r.y*this.z - r.z*this.y + r.w*this.x;
            float tmpy = -r.x*this.z + r.y*this.w + r.z*this.x + r.w*this.y;
            float tmpz = r.x*this.y - r.y*this.x + r.z*this.w + r.w*this.z;
            float tmpw  = this.w*r.w - this.x*r.x - this.y*r.y - this.z*r.z;

            this.x = tmpx;
            this.y = tmpy;
            this.z = tmpz;
            this.w = tmpw;
        }

        public float[] getQuaternionAsFloat()
        {
            return new float[] {this.x, this.y, this.z, this.w};
        }

        public float[] getConjugate()
        {
            return new float[] {-this.x, -this.y, -this.z, this.w};
        }
    }

    public static class mVector
    {
        float x, y, z;

        public mVector(float x, float y, float z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public void rotateByQuaternion(mQuaternion q)
        {
            float x = (1 - 2 * q.y * q.y - 2 * q.z * q.z) * this.x +
                    2 * (q.x * q.y + q.w * q.z) * this.y +
                    2 * (q.x * q.z - q.w * q.y) * this.z;
            float y = 2 * (q.x * q.y - q.w * q.z) * this.x +
                    (1 - 2 * q.x * q.x - 2 * q.z * q.z) * this.y +
                    2 * (q.y * q.z + q.w * q.x) * this.z;
            float z = 2 * (q.x * q.z + q.w * q.y) * this.x +
                    2 * (q.y * q.z - q.w * q.x) * this.y +
                    (1 - 2 * q.x * q.x - 2 * q.y * q.y) * this.z;


            this.x = x;
            this.y = y;
            this.z = z;
        }

        public void rotateByQuaternion(float[] q1)
        {
            mQuaternion q = new mQuaternion(q1);

            float x = (1 - 2 * q.y * q.y - 2 * q.z * q.z) * this.x +
                    2 * (q.x * q.y + q.w * q.z) * this.y +
                    2 * (q.x * q.z - q.w * q.y) * this.z;
            float y = 2 * (q.x * q.y - q.w * q.z) * this.x +
                    (1 - 2 * q.x * q.x - 2 * q.z * q.z) * this.y +
                    2 * (q.y * q.z + q.w * q.x) * this.z;
            float z = 2 * (q.x * q.z + q.w * q.y) * this.x +
                    2 * (q.y * q.z - q.w * q.x) * this.y +
                    (1 - 2 * q.x * q.x - 2 * q.y * q.y) * this.z;


            this.x = x;
            this.y = y;
            this.z = z;
        }

        public void normalise()
        {
            float fact = (float)Math.sqrt(x*x + y*y + z*z);
            this.x /= fact;
            this.y /= fact;
            this.z /= fact;
        }

        public float[] getEuler()
        {
            double roll, pitch, yaw;

            roll = Math.atan2(this.y, this.x);
            pitch = Math.atan2(this.y, this.z);
            yaw = Math.atan2(this.x, this.z);

            return new float[] {(float)roll, (float)pitch, (float)yaw};
        }
    }
}
