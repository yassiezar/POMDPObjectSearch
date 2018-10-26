package com.example.jaycee.pomdpobjectsearch;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;

import com.example.jaycee.pomdpobjectsearch.rendering.SurfaceRenderer;
import com.google.android.gms.vision.Frame;

import org.opencv.core.Mat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.IntBuffer;

import org.opencv.android.Utils;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import static org.opencv.core.CvType.CV_32F;
import static org.opencv.core.CvType.CV_32SC1;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2BGRA;

public class ObjectDetector implements Runnable
{

    private static final String TAG = "OBJECT_DETECTOR";//ObjectDetector.class.getSimpleName();

    private Handler handler = new Handler();

    private Bitmap bitmap;

    private SurfaceRenderer renderer;

    private boolean stop = false;

    private static final int O_NOTHING = 0;
    private int code = O_NOTHING;

    private int counter;


    public ObjectDetector(Context context, int scannerWidth, int scannerHeight, SurfaceRenderer renderer)
    {
        this.renderer = renderer;

        this.bitmap = Bitmap.createBitmap(scannerWidth, scannerHeight, Bitmap.Config.ARGB_8888);

        String cfg_file = getPath(".cfg", context);
        String weigth_file = getPath(".weights", context);
        float confidence_threshold = 0.5f;
        String classNames_file = getPath(".names", context);
        String prova = getPath(".jpg", context);

        JNIBridge.create(cfg_file, weigth_file, confidence_threshold, classNames_file);

        counter = 0;
    }

    @Override
    public void run()
    {

        //Log.v(TAG, "Running Object Detector");

        code = O_NOTHING;

        IntBuffer a = renderer.getCurrentFrameBuffer();

        bitmap.copyPixelsFromBuffer(renderer.getCurrentFrameBuffer());

        String v = "Buffer: " + a.capacity() + " Bitmap: " + bitmap.getByteCount();
        Log.v("BUFFER", v);

        Mat inputFrame = new Mat();

        Utils.bitmapToMat(bitmap, inputFrame);

        float[] results = new float[1];

        double time = -1;
        if(counter%15 == 0)
            results = JNIBridge.classify(inputFrame.getNativeObjAddr());
        counter++;

        int result_length = results.length;

        if(result_length > 5 && result_length%6 == 0)
        {

            String t = "Time: " + time + " s - Number of found objects: " + result_length/6;
            Log.v(TAG, t);

            int num_foundObject = result_length/6;

            for(int i = 0; i < num_foundObject; i++)
            {

                int idx = (int) results[(i*6)+4]+1;

                t = "Index: " + idx;
                Log.v(TAG, t);

                code = (int) results[(i*6)+4];

                switch (idx)
                {
                    case 1: //person
                        code = 1;
                        break;
                    case 25: //backpack
                        code = 2;
                        break;
                    case 57: //chair
                        code = 3;
                        break;
                    case 63: //tvmonitor
                        code = 4;
                        break;
                    case 64: //laptop
                        code = 5;
                        break;
                    case 65: //mouse
                        code = 6;
                        break;
                    case 67: //keyboar
                        code = 7;
                        break;
                }

            }
        }

        if(!stop) handler.postDelayed(this, 40);

    }

    public void stop()
    {
        this.stop = true;
        handler = null;
    }

    public int getCode() { return this.code; }

    @SuppressLint("LongLogTag")
    private static String getPath(String fileType, Context context) {
        AssetManager assetManager = context.getAssets();
        String[] pathNames = {};
        String fileName = "";
        try {
            pathNames = assetManager.list("yolo");
        } catch (IOException e) {
            e.printStackTrace();
        }
        for ( String filePath : pathNames ) {
            if ( filePath.endsWith(fileType)) {
                fileName = filePath;
                break;
            }
        }
        BufferedInputStream inputStream;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open("yolo/" + fileName));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();

            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), fileName);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            // Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            //Log.i(TAG, "Failed to upload a file");
        }
        return "";
    }

    public void drawRectangle(float[] coordinates){

        int x = (int) coordinates[0]*1440;
        int y = (int) coordinates[1]*2280;
        int w = (int) coordinates[2]*1440;
        int h = (int) coordinates[3]*2280;

//            Point p1(cvRound(x - w / 2), cvRound(y - h / 2));
//            Point p2(cvRound(x + w / 2), cvRound(y + h / 2));
//
//            Rect object(p1, p2);
//
//            Scalar object_roi_color(0, 255, 0);
//
//            rectangle(result, object, object_roi_color);
//            putText(result, class_names[idx], p1, FONT_HERSHEY_SIMPLEX, 0.75, Scalar(0,255,0), 2);

    }


}

