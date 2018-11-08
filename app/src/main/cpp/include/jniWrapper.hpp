#ifndef POMDPOBJECTSEARCH_JNIWRAPPER_HPP
#define POMDPOBJECTSEARCH_JNIWRAPPER_HPP

#include <jni.h>
#include <android/log.h>

#include <SoundGenerator/SoundGenerator.hpp>

#ifdef __cplusplus
extern "C" {
#endif

#define JULAYOM(rettype, name)                                             \
  rettype JNIEXPORT JNICALL Java_com_example_jaycee_pomdpobjectsearch_JNIBridge_##name

SoundGenerator::SoundGenerator* soundGenerator;

JULAYOM(bool, initSound)(JNIEnv*, jobject);
JULAYOM(bool, killSound)(JNIEnv*, jobject);
JULAYOM(void, playSound)(JNIEnv*, jobject, jfloatArray, jfloatArray, jfloat, jfloat);
JULAYOM(bool, stopSound)(JNIEnv*, jobject);

#ifdef __cplusplus
}
#endif

#endif
