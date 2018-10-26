/*  This file has been created by Nataniel Ruiz affiliated with Wall Lab
 *  at the Georgia Institute of Technology School of Interactive Computing
 */

package com.example.jaycee.pomdpobjectsearch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class BoundingBoxView extends View {
    private float[] coordinates;
    private final Paint fgPaint, bgPaint, textPaint, trPaint;

    public BoundingBoxView(final Context context, final AttributeSet set) {
        super(context, set);

        fgPaint = new Paint();
        fgPaint.setColor(0xff00ff01);
        fgPaint.setStyle(Paint.Style.STROKE);
        fgPaint.setStrokeWidth(4);

        bgPaint = new Paint();
        bgPaint.setARGB(0, 0, 0, 0);
        bgPaint.setAlpha(0);
        bgPaint.setStyle(Paint.Style.STROKE);

        trPaint = new Paint();
        trPaint.setColor(0xff00ff02);
        trPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setStyle(Paint.Style.STROKE);
        textPaint.setTextSize(50);  //set text size

    }

    public void setResults(float[] results) {
        this.coordinates = results;
        postInvalidate();
    }

    @Override
    public void onDraw(final Canvas canvas) {

        // Get view size.
        float view_height_temp = (float) this.getHeight();
        float view_width_temp = (float) this.getWidth();
        float view_height = Math.max(view_height_temp, view_width_temp);
        float view_width = Math.min(view_height_temp, view_width_temp);

        String prediction_string = "width: " + Float.toString(view_width) +
                " height: " + Float.toString(view_height);
        Log.v("BoundingBox", prediction_string);

        if (coordinates != null) {
            for(int i=0; i<coordinates.length/6; i++) {
                // Get x, y, width and height before pre processing of
                // bounding boxes. Then pre-process the bounding boxes
                // by using the multipliers and offsets to map a 448x448 image
                // coordinates to a device_width x device_height surface

                float x = coordinates[(i*6)] * 1440;
                float y = coordinates[(i*6)+1] * 2280;
                float w = coordinates[(i*6)+2] * 1440;
                float h = coordinates[(i*6)+3] * 2280;

                float p1_x = x - w / 2;
                float p1_y = y - h / 2;
                float p2_x = x + w / 2;
                float p2_y = y + h / 2;

                // Create new bounding box and draw it.
                RectF boundingBox = new RectF(p1_x, p1_y, p2_x, p2_y);

                String t = "Boundibox: (" + p1_x + ", " + p1_y + ")( " + p1_x + ", " + p1_x + "), color: ";
                Log.d("BOUNDIGBOX", t);

                Paint p = new Paint();
                p.setColor(0xff00ff01);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(10);

                RectF prova = new RectF(500, 500, 600, 600);

                canvas.drawRect(prova, p);
                canvas.drawRect(boundingBox, fgPaint);
                canvas.drawRect(boundingBox, bgPaint);

                // Create class name text on bounding box.
                String class_name = Float.toString(coordinates[(i*6)+4]);
                float text_width = textPaint.measureText(class_name)/2;
                float text_size = textPaint.getTextSize();
                float text_center_x = p1_x - 2;
                float text_center_y = p1_y - text_size;
                textPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawRect(text_center_x, text_center_y, text_center_x + 2 * text_width, text_center_y + text_size, trPaint);
                canvas.drawText(class_name, text_center_x + text_width, text_center_y + text_size, textPaint);

            }
        }
    }
}
