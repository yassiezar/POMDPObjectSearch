#ifndef POMDPOBJECTSEARCH_JNIWRAPPER_HPP
#define POMDPOBJECTSEARCH_JNIWRAPPER_HPP

#include <jni.h>
#include <android/log.h>
#include <MDP/mdp.hpp>
#include <SoundGenerator/SoundGenerator.hpp>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT bool JNICALL Java_com_example_jaycee_pomdpobjectsearch_JNIBridge_initSearch(JNIEnv*, jobject, jlong, jlong);
JNIEXPORT jlong JNICALL Java_com_example_jaycee_pomdpobjectsearch_JNIBridge_getAction(JNIEnv*, jobject, jlong);

JNIEXPORT bool JNICALL Java_com_example_jaycee_pomdpobjectsearch_JNIBridge_initSound(JNIEnv*, jobject);
JNIEXPORT bool JNICALL Java_com_example_jaycee_pomdpobjectsearch_JNIBridge_killSound(JNIEnv*, jobject);
JNIEXPORT void JNICALL_Java_com_example_jaycee_pomdpobjectsearch_JNIBridge_playSound(JNIEnv*, jobject, jfloatArray, jfloatArray, jfloat, jfloat);

#ifdef __cplusplus
}
#endif

static MDPNameSpace::MDP mdp;
static SoundGenerator::SoundGenerator soundGenerator;

#endif
