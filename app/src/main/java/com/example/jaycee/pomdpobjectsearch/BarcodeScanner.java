package com.example.jaycee.pomdpobjectsearch;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;

import com.example.jaycee.pomdpobjectsearch.rendering.SurfaceRenderer;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

public class BarcodeScanner implements Runnable
{
    private static final String TAG = BarcodeScanner.class.getSimpleName();

    private static final int O_NOTHING = 0;

    private Handler handler = new Handler();

    private BarcodeDetector detector;
    private Bitmap bitmap;

    private SurfaceRenderer renderer;

    private boolean stop = false;

    private int code = O_NOTHING;

    public BarcodeScanner(Context context, int scannerWidth, int scannerHeight, SurfaceRenderer renderer)
    {
        this.renderer = renderer;

        this.detector = new BarcodeDetector.Builder(context).setBarcodeFormats(Barcode.QR_CODE).build();
        this.bitmap = Bitmap.createBitmap(scannerWidth, scannerHeight, Bitmap.Config.ARGB_8888);
    }

    @Override
    public void run()
    {
        Log.v(TAG, "Running barcode scanner");
        code = O_NOTHING;

        bitmap.copyPixelsFromBuffer(renderer.getCurrentFrameBuffer());

        Frame bitmapFrame = new Frame.Builder().setRotation(180).setBitmap(bitmap).build();
        SparseArray<Barcode> barcodes = detector.detect(bitmapFrame);
        if(barcodes.size() > 0)
        {
            for(int i = 0; i < barcodes.size(); i++)
            {
                int key = barcodes.keyAt(i);
                Log.i(TAG, String.format("Object found, coords %d %d", barcodes.get(key).getBoundingBox().right, barcodes.get(key).getBoundingBox().bottom));
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
}
