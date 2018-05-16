#ifndef POMDPOBJECTSEARCH_JNIWRAPPER_HPP
#define POMDPOBJECTSEARCH_JNIWRAPPER_HPP

#include <jni.h>
#include <android/log.h>
#include <MDP/mdp.hpp>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT bool JNICALL Java_com_example_jaycee_pomdpobjectsearch_JNIBridge_initSearch(JNIEnv*, jobject, jlong, jlong);
JNIEXPORT jlong JNICALL Java_com_example_jaycee_pomdpobjectsearch_JNIBridge_getAction(JNIEnv*  jobject, jlong);

static MDPNameSpace::MDP mdp;

#ifdef __cplusplus
}
#endif

#endif
