#include <jniWrapper.hpp>

JNIEXPORT bool JNICALL
Java_com_example_jaycee_pomdpobjectsearch_JNIBridge_initSearch(JNIEnv *env, jobject obj, jlong target, jlong horizon)
{
    mdp.setTarget(target);
    mdp.solve(horizon);

    return true;
}

JNIEXPORT jlong JNICALL
Java_com_example_jaycee_pomdpobjectsearch_JNIBridge_getAction(JNIEnv *env, jobject obj, jlong state)
{
    mdp.getAction(state);
}
