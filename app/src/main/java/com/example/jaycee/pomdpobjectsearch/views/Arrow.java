package com.example.jaycee.pomdpobjectsearch.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;

import com.example.jaycee.pomdpobjectsearch.R;

public class Arrow extends View
{
    private static final Direction DEFAULT_DIRECTION = Direction.LEFT;
    private static final int DEFAULT_COLOUR = 0xffffffff;
    private static final int DEFAULT_ALPHA = 255;

    private Paint paint;
    private Path arrowPath;
    private Direction direction;
    private int colour;
    private int alpha;

    public Arrow(Context context)
    {
        super(context);
    }

    public Arrow(Context context, AttributeSet attr)
    {
        super(context, attr);

        init(attr);
    }

    public Arrow(Context context, AttributeSet attr, int style)
    {
        super(context, attr, style);
    }

    private void init(AttributeSet attr)
    {
        if(attr != null)
        {
            TypedArray attrArray = getContext().obtainStyledAttributes(attr, R.styleable.Arrow);
            switch (attrArray.getInt(R.styleable.Arrow_arrow_direction, 0))
            {
                case 0: direction = Direction.LEFT; break;
                case 1: direction = Direction.RIGHT; break;
                case 2: direction = Direction.UP; break;
                case 3: direction = Direction.DOWN; break;
                default: direction = Direction.LEFT; break;
            }

            colour = attrArray.getColor(R.styleable.Arrow_arrow_color, DEFAULT_COLOUR);
            alpha = attrArray.getInt(R.styleable.Arrow_arrow_alpha, DEFAULT_ALPHA);

            attrArray.recycle();
        }
        else
        {
            direction = DEFAULT_DIRECTION;
            colour = DEFAULT_COLOUR;
            alpha = DEFAULT_ALPHA;
        }

        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(colour);
        paint.setAlpha(alpha);
        paint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        canvas.drawPath(getArrowPath(), paint);
    }

    private Path getArrowPath()
    {
        if(arrowPath == null)
        {
            arrowPath = new Path();
            int width = getWidth();
            int height = getHeight();
            Point p1, p2, p3;

            switch (direction)
            {
                case LEFT:
                    p1 = new Point(width, 0);
                    p2 = new Point(width, height);
                    p3 = new Point(0, height/2);
                    break;
                case RIGHT:
                    p1 = new Point(0, 0);
                    p2 = new Point(0, height);
                    p3 = new Point(width, height/2);
                    break;
                case UP:
                    p1 = new Point(0, height);
                    p2 = new Point(width, height);
                    p3 = new Point(width/2, 0);
                    break;
                case DOWN:
                    p1 = new Point(0, 0);
                    p2 = new Point(width, 0);
                    p3 = new Point(width/2, height);
                    break;
                default:
                    p1 = new Point(0, 0);
                    p2 = new Point(width, 0);
                    p3 = new Point(width/2, height);
            }
            arrowPath.moveTo(p1.x, p1.y);
            arrowPath.lineTo(p2.x, p2.y);
            arrowPath.lineTo(p3.x, p3.y);
        }

        return arrowPath;
    }

    public void setColour(int colour)
    {
        if(this.colour != colour)
        {
            this.colour = colour;
            if(paint != null)
            {
                paint.setColor(colour);
            }
            arrowPath = null;
            invalidate();
        }
    }

    public void setDirection(Direction direction)
    {
        if(this.direction != direction)
        {
            this.direction = direction;
            arrowPath = null;
            invalidate();
        }
    }

    public void setAlpha(int alpha)
    {
        if(this.alpha != alpha)
        {
            this.alpha = alpha;
            if(paint != null)
            {

                paint.setAlpha(alpha);
            }
            arrowPath = null;
            invalidate();
        }
    }

    public enum Direction
    {
        LEFT, UP, RIGHT, DOWN
    }
}
