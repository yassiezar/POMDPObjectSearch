package com.example.jaycee.pomdpobjectsearch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;

import com.example.jaycee.pomdpobjectsearch.rendering.SurfaceRenderer;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.nio.IntBuffer;

public class BarcodeScanner implements Runnable
{
    private static final String TAG = BarcodeScanner.class.getSimpleName();

    private static final int O_NOTHING = 0;

    private IntBuffer bitmapBuffer;

    private int scannerWidth, scannerHeight;

    private Handler handler = new Handler();

    private BarcodeDetector detector;

    private SurfaceRenderer renderer;

    private boolean stop = false;

    private int code = O_NOTHING;

    public BarcodeScanner(Context context, int scannerWidth, int scannerHeight, SurfaceRenderer renderer)
    {
        this.scannerWidth = scannerWidth;
        this.scannerHeight = scannerHeight;
        this.renderer = renderer;

        this.bitmapBuffer = IntBuffer.allocate(scannerHeight*scannerWidth);

        this.detector = new BarcodeDetector.Builder(context).setBarcodeFormats(Barcode.ALL_FORMATS).build();
    }

    @Override
    public void run()
    {
        Log.v(TAG, "Running barcode scanner");
/*        if(bitmapBuffer != null)
        {
            bitmapBuffer.position(0);
        }*/

/*        bitmapBuffer.position(0);
        // GLES20.glReadPixels(scannerX, scannerY, scannerX+scannerWidth, scannerY+scannerHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bitmapBuffer);
        GLES20.glReadPixels(0, 0, scannerWidth, scannerHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bitmapBuffer);

        Bitmap bitmap = Bitmap.createBitmap(scannerWidth, scannerHeight, Bitmap.Config.ARGB_8888);

        bitmap.copyPixelsFromBuffer(bitmapBuffer);

        Frame bitmapFrame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<Barcode> barcodes = detector.detect(bitmapFrame);

        if(barcodes.size() > 0)
        {
            Log.i(TAG, "Object found: " + code);
            for(int i = 0; i < barcodes.size(); i ++)
            {
                int key = barcodes.keyAt(i);

                // Rect scannerArea = new Rect(scannerView.getLeft(), scannerView.getTop(), scannerView.getRight(), scannerView.getBottom());
*//*                Rect scannerArea = new Rect(scannerX, scannerY, scannerX+scannerWidth, scannerHeight+scannerY);
                if(scannerArea.contains(barcodes.get(key).getBoundingBox()))
                {*//*
                    code = Integer.parseInt(barcodes.get(key).displayValue);
                    Log.i(TAG, "Object found: " + code);
//                }
            }
        }*/
        code = O_NOTHING;
        // bitmap.recycle();

        Bitmap bitmap = Bitmap.createBitmap(scannerWidth, scannerHeight, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(renderer.getCurrentFrameBuffer());

        Frame bitmapFrame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<Barcode> barcodes = detector.detect(bitmapFrame);
        if(barcodes.size() > 0)
        {
            Log.i(TAG, "Object found: " + code);
        }

        if(!stop) handler.postDelayed(this, 40);
    }

    public void stop()
    {
        this.stop = true;
        handler = null;
        // bitmapBuffer.clear();
    }

    public int getCode() { return this.code; }
}
