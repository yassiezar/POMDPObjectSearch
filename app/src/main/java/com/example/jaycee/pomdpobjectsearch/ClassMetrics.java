package com.example.jaycee.pomdpobjectsearch;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.Socket;

public class ClassMetrics
{
    private static final String TAG = ClassMetrics.class.getSimpleName();
    private static final String DELIMITER = ",";

    private WifiDataSend dataStreamer = null;

    private double timetamp;
    private double targetX, targetY, targetZ;
    private double phoneX, phoneY, phoneZ;
    private double phoneQx, phoneQy, phoneQz, phoneQw;
    private long observation, targetObservation, target;

    public void writeWifi()
    {
        String wifiString = String.valueOf(timetamp) + DELIMITER +
                String.valueOf(observation) + DELIMITER +
                String.valueOf(targetObservation) + DELIMITER +
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
            dataStreamer.execute(wifiString);
        }
    }

    public void updateTimestamp(double timetamp) { this.timetamp = timetamp; }

    public void updateTargetPosition(double x, double y, double z)
    {
        targetX = x;
        targetY = y;
        targetZ = z;
    }

    public void updatePhonePosition(double x, double y, double z)
    {
        phoneX = x;
        phoneY = y;
        phoneZ = z;
    }

    public void updatePhoneOrientation(double x, double y, double z, double w)
    {
        phoneQx = x;
        phoneQy = y;
        phoneQz = z;
        phoneQw = w;
    }

    public void updateObservation(long observation) { this.observation = observation; }
    public void updateTargetObservation(long targetObs) { this.targetObservation = targetObs; }
    public void updateTarget (long target) { this.target = target; }

    private static class WifiDataSend extends AsyncTask<String, Void, Void>
    {
        private String serverIdAddress = "10.5.42.29";
        private int connectionPort = 6666;

        public WifiDataSend() { }

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
            catch(IOException e)
            {
                Log.e(TAG, "Wifi write error: ", e);
            }

            return null;
        }
    }
}
