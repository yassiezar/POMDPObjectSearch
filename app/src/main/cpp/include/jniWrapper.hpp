#ifndef POMDPOBJECTSEARCH_JNIWRAPPER_HPP
#define POMDPOBJECTSEARCH_JNIWRAPPER_HPP

#include <jni.h>
#include <android/log.h>
#include <MDP/mdp.hpp>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT bool JNICALL Java_com_example_jaycee_pomdpobjectsearch_JNIBridge_init(JNIEnv *env, jobject obj);

#ifdef __cplusplus
}
#endif

#endif
