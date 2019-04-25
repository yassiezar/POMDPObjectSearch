package com.example.jaycee.pomdpobjectsearch.policy;

import com.example.jaycee.pomdpobjectsearch.Objects;

public interface Policy
{
    void setTarget(Objects.Observation target, int numStates);
    long getAction(State state);
}
