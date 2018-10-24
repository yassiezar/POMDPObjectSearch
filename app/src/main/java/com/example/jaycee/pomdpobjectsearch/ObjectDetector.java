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
import org.opencv.android.Utils;
import org.opencv.imgproc.Imgproc;

import static org.opencv.core.CvType.CV_32F;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2BGRA;

public class ObjectDetector implements Runnable
{

    private static final String TAG = "OBJECT_DETECTOR";//ObjectDetector.class.getSimpleName();

    private Handler handler = new Handler();

    private Bitmap bitmap;

    private SurfaceRenderer renderer;

    private boolean stop = false;

    private String cfg_file;
    private String weigth_file;
    private float confidence_threshold;
    private String classNames_file;

    private static final int O_NOTHING = 0;
    private int code = O_NOTHING;

    private int counter;


    public ObjectDetector(Context context, int scannerWidth, int scannerHeight, SurfaceRenderer renderer)
    {
        this.renderer = renderer;

        //this.detector = new BarcodeDetector.Builder(context).setBarcodeFormats(Barcode.QR_CODE).build();
        this.bitmap = Bitmap.createBitmap(scannerWidth, scannerHeight, Bitmap.Config.ARGB_8888);

        cfg_file = getPath(".cfg", context);
        weigth_file = getPath(".weights", context);
        confidence_threshold = 0.5f;
        classNames_file = getPath(".names", context);

        JNIBridge.create(cfg_file, weigth_file, confidence_threshold, classNames_file);

        counter = 0;
    }

    @Override
    public void run()
    {

        //Log.v(TAG, "Running Object Detector");

        code = O_NOTHING;

        bitmap.copyPixelsFromBuffer(renderer.getCurrentFrameBuffer());


        Mat inputFrame = new Mat();
        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, inputFrame);

        float[] results = null;

        double time = -1;
        if(counter%15 == 0) {
            time = JNIBridge.classify(inputFrame.getNativeObjAddr(), results);
            int obj = 0;
            if(results != null)
                obj = results.length;
            String t = "Time: " + time + " s - Found object: " + obj;
            Log.v(TAG, t);
        }
        counter++;



//        int[] idx = new int[results.rows()];
//
//        double[] a;
//        if(results != null) {
//            a = results.get(0, 0);
//        }




//        if(results.rows() > 0 )
//        {
//            for(int i = 0; i < results.rows(); i++)
//            {
////                int key = barcodes.keyAt(i);
////                Log.d(TAG, String.format("Object found, coords %d %d", barcodes.get(key).getBoundingBox().right, barcodes.get(key).getBoundingBox().bottom));
////                Log.i(TAG, String.format("Barcode content: %s", barcodes.get(key).rawValue));
////                this.code = Integer.parseInt(barcodes.get(key).rawValue);
//
//
//                int buff[] = new int[(int)results.total() * results.channels()];
//                results.get(0, 0);
//
//                idx[i] =
//
//                switch (result.getItemId())
//                {
//                    case R.id.item_object_mug:
//                        target = T_MUG;
//                        break;
//                    case R.id.item_object_desk:
//                        target = T_DESK;
//                        break;
//                    case R.id.item_object_office_supplies:
//                        target = T_OFFICE_SUPPLIES;
//                        break;
//                    case R.id.item_object_keyboard:
//                        target = T_COMPUTER_KEYBOARD;
//                        break;
//                    case R.id.item_object_monitor:
//                        target = T_COMPUTER_MONITOR;
//                        break;
//                    case R.id.item_object_mouse:
//                        target = T_COMPUTER_MOUSE;
//                        break;
//                    case R.id.item_object_window:
//                        target = T_WINDOW;
//                        break;
//                }
//
//                this.code =
//            }
//        }



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
}

