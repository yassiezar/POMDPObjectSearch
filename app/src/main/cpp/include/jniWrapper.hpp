#ifndef POMDPOBJECTSEARCH_JNIWRAPPER_HPP
#define POMDPOBJECTSEARCH_JNIWRAPPER_HPP

#include <jni.h>
#include <android/log.h>
#include <SoundGenerator/SoundGenerator.hpp>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT bool JNICALL Java_com_example_jaycee_pomdpobjectsearch_JNIBridge_initSound(JNIEnv*, jobject);
JNIEXPORT bool JNICALL Java_com_example_jaycee_pomdpobjectsearch_JNIBridge_killSound(JNIEnv*, jobject);
JNIEXPORT void JNICALL_Java_com_example_jaycee_pomdpobjectsearch_JNIBridge_playSoundFFFF(JNIEnv*, jobject, jfloat, jfloatArray, jfloat, jfloat);
JNIEXPORT void JNICALL_Java_com_example_jaycee_pomdpobjectsearch_JNIBridge_playSoundFF(JNIEnv*, jobject, jfloat, jfloat);

#ifdef __cplusplus
}
#endif

static SoundGenerator::SoundGenerator soundGenerator;

#endif
