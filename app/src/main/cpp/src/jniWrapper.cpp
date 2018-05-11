#include <jniWrapper.hpp>

JNIEXPORT bool JNICALL
Java_com_example_jaycee_pomdpobjectsearch_JNIBridge_init(JNIEnv *env, jobject obj)
{
    MDPNameSpace::MDP mdp;

    return true;
}
