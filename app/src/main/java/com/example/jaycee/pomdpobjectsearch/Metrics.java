package com.example.jaycee.pomdpobjectsearch;

import android.os.AsyncTask;
import android.util.Log;

import com.google.ar.core.Pose;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.Socket;

public class Metrics
{
    private static final String TAG = Metrics.class.getSimpleName();
    private static final String DELIMITER = ",";

    private WifiDataSend dataStreamer = null;

    private double timestamp;
    private double targetX, targetY, targetZ;
    private double phoneX, phoneY, phoneZ;
    private double phoneQx, phoneQy, phoneQz, phoneQw;
    private long observation, target, state;

    public void writeWifi()
    {
        String wifiString = String.valueOf(timestamp) + DELIMITER +
                String.valueOf(observation) + DELIMITER +
                String.valueOf(state) + DELIMITER +
                String.valueOf(target) + DELIMITER +
                String.valueOf(targetX) + DELIMITER +
                String.valueOf(targetY) + DELIMITER +
                String.valueOf(targetZ) + DELIMITER +
                String.valueOf(phoneX) + DELIMITER +
                String.valueOf(phoneY) + DELIMITER +
                String.valueOf(phoneZ) + DELIMITER +
                String.valueOf(phoneQx) + DELIMITER +
                String.valueOf(phoneQy) + DELIMITER +
                String.valueOf(phoneQz) + DELIMITER +
                String.valueOf(phoneQw) + DELIMITER;

        if(dataStreamer == null ||
                dataStreamer.getStatus() != AsyncTask.Status.RUNNING)
        {
            dataStreamer = new WifiDataSend();
            // dataStreamer.execute(wifiString);
        }
    }

    public void updateTimestamp(double timestamp) { this.timestamp = timestamp; }
    public void updateState(long state) { this.state = state; }

    public void updateTargetPosition(Pose pose)
    {
        float[] pos = pose.getTranslation();

        targetX = pos[0];
        targetY = pos[1];
        targetZ = pos[2];
    }

    public void updatePhonePosition(Pose pose)
    {
        float[] pos = pose.getTranslation();

        phoneX = pos[0];
        phoneY = pos[1];
        phoneZ = pos[2];
    }

    public void updatePhoneOrientation(Pose pose)
    {
        float[] q = pose.getRotationQuaternion();

        phoneQx = q[0];
        phoneQy = q[1];
        phoneQz = q[2];
        phoneQw = q[3];
    }

    public void updateObservation(long observation) { this.observation = observation; }
    public void updateTarget (long target) { this.target = target; }

    private static class WifiDataSend extends AsyncTask<String, Void, Void>
    {
        private String serverIdAddress = "10.42.0.1";
        private int connectionPort = 6666;

        WifiDataSend() { }

        @Override
        protected Void doInBackground(String... strings)
        {
            try
            {
                Socket socket = new Socket(serverIdAddress, connectionPort);
                OutputStream stream = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(stream);

                int charsRead;
                int bufferLen = 1024;
                char[] tempBuffer = new char[bufferLen];

                BufferedReader bufferedReader = new BufferedReader(new StringReader(strings[0]));

                Log.d(TAG, "Writing to WiFi");
                while((charsRead = bufferedReader.read(tempBuffer, 0, bufferLen)) != -1)
                {
                    writer.print(tempBuffer);
                }
                writer.write("\n");

                writer.flush();
                writer.close();

                socket.close();
            }
            catch(ConnectException e)
            {
                Log.w(TAG, "WiFi connect error. Likely not connected to wifi network");
            }
            catch(IOException e)
            {
                Log.e(TAG, "Wifi write error: ", e);
            }

            return null;
        }
    }
}
