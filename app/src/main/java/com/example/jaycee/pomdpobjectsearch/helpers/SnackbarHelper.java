package com.example.jaycee.pomdpobjectsearch.helpers;

import android.app.Activity;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.view.View;


public final class SnackbarHelper
{
    private static final int BACKGROUND_COLOUR = 0xbf323232;

    private enum DismissBehaviour {HIDE, SHOW, FINISH};

    private Snackbar snackbar;

    private Activity activity;

    public SnackbarHelper(Activity activity)
    {
        this.activity = activity;
    }

    private boolean isShowing() { return snackbar != null; }

    public void showMessage(String message)
    {
        show(message, DismissBehaviour.HIDE);
    }

    public void showError(String errorMessage)
    {
        show(errorMessage, DismissBehaviour.FINISH);
    }

    public void hide(Activity activity)
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if(snackbar != null)
                {
                    snackbar.dismiss();
                }
                snackbar = null;
            }
        });
    }

    private void show(final String message, final DismissBehaviour dismissBehaviour)
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                snackbar = Snackbar.make(activity.findViewById(android.R.id.content), message, Snackbar.LENGTH_INDEFINITE);
                snackbar.getView().setBackgroundColor(BACKGROUND_COLOUR);

                if(dismissBehaviour != DismissBehaviour.HIDE)
                {
                    snackbar.setAction("Dismiss", new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            snackbar.dismiss();
                        }
                    });

                    if(dismissBehaviour == DismissBehaviour.FINISH)
                    {
                        snackbar.addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>()
                        {
                            @Override
                            public void onDismissed(Snackbar transientBottomBar, int event)
                            {
                                super.onDismissed(transientBottomBar, event);
                                activity.finish();
                            }
                        });
                    }
                }
                snackbar.show();
            }
        });
    }
}
