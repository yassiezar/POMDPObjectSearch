package com.example.jaycee.pomdpobjectsearch;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class HandlerMDPIntentService extends Handler
{
    private ActivityCamera activityCamera;

    private boolean mdpLearning = false;
    private boolean mdpLearned = false;

    HandlerMDPIntentService(ActivityCamera activityCamera)
    {
        this.activityCamera = activityCamera;
    }

    @Override
    public void handleMessage(Message message)
    {
        Bundle intentServiceReply = message.getData();

        mdpLearning = intentServiceReply.getBoolean("BOOLEAN_MDP_LEARNING", false);
        mdpLearned = intentServiceReply.getBoolean("BOOLEAN_MDP_LEARNED", false);
    }

    public boolean getMdpLearning() { return this.mdpLearning; }
    public boolean getMdpLearned() { return this.mdpLearned; }
}
