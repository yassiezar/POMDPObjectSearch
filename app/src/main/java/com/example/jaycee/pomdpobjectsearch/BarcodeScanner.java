package com.example.jaycee.pomdpobjectsearch;

import android.graphics.Bitmap;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.nio.IntBuffer;

public class BarcodeScanner implements Runnable
{
    private static final String TAG = BarcodeScanner.class.getSimpleName();

    private IntBuffer bitmapBuffer;

    private int scannerWidth, scannerHeight;
    private int scannerX, scannerY;

    private Handler handler = new Handler();

    private boolean stop = false;

    public BarcodeScanner(String name, int scannerWidth, int scannerHeight, int scannerX, int scannerY)
    {
        this.scannerWidth = scannerWidth;
        this.scannerHeight = scannerHeight;
        this.scannerX = scannerX;
        this.scannerY = scannerY;

        this.bitmapBuffer = IntBuffer.allocate(scannerWidth*scannerHeight);
    }

    @Override
    public void run()
    {
        Log.i(TAG, "Running barcode scanner");
        bitmapBuffer.position(0);

        GLES20.glReadPixels(scannerX, scannerY, scannerWidth, scannerHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bitmapBuffer);

        Bitmap bitmap = Bitmap.createBitmap(scannerWidth, scannerHeight, Bitmap.Config.ARGB_8888);

        bitmap.copyPixelsFromBuffer(bitmapBuffer);

        if(!stop) handler.postDelayed(this, 40);
        // Log.w(TAG, "Barcode scanner thread interrupted");
    }

    public void stop() { this.stop = true; }
}
