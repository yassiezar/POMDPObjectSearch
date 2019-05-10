package com.example.jaycee.pomdpobjectsearch.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.example.jaycee.pomdpobjectsearch.R;

public class CentreView extends LinearLayout
{
    private Arrow leftArrow, rightArrow, upArrow, downArrow;

    public CentreView(Context context, AttributeSet attr)
    {
        super(context, attr);
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.centre_layout, this);

        leftArrow = findViewById(R.id.arrow_left);
        rightArrow = findViewById(R.id.arrow_right);
        upArrow = findViewById(R.id.arrow_up);
        downArrow = findViewById(R.id.arrow_down);
    }

    public void setArrowAlpha(Arrow.Direction direction, int alpha)
    {
        switch(direction)
        {
            case UP: upArrow.setAlpha(alpha); break;
            case DOWN: downArrow.setAlpha(alpha); break;
            case LEFT: leftArrow.setAlpha(alpha); break;
            case RIGHT: rightArrow.setAlpha(alpha); break;
        }
    }

    public void resetArrows()
    {
        upArrow.setAlpha(0);
        downArrow.setAlpha(0);
        leftArrow.setAlpha(0);
        rightArrow.setAlpha(0);
    }
}
