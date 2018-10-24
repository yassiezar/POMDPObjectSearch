#ifndef POMDPOBJECTSEARCH_JNIWRAPPER_HPP
#define POMDPOBJECTSEARCH_JNIWRAPPER_HPP

#include <jni.h>
#include <android/log.h>

#include <SoundGenerator/SoundGenerator.hpp>
#include <ObjectDetector/ObjectDetector.hpp>
#include <ObjectDetector/Helper.hpp>

#ifdef __cplusplus
extern "C" {
#endif

#define JULAYOM(rettype, name)                                             \
  rettype JNIEXPORT JNICALL Java_com_example_jaycee_pomdpobjectsearch_JNIBridge_##name

SoundGenerator::SoundGenerator* soundGenerator;

JULAYOM(bool, initSound)(JNIEnv* env, jobject obj);
JULAYOM(bool, killSound)(JNIEnv* env, jobject obj);
JULAYOM(void, playSound)(JNIEnv* env, jobject obj, jfloatArray src, jfloatArray list, jfloat gain, jfloat pitch);
JULAYOM(bool, stopSound)(JNIEnv*, jobject);

ObjectDetector::Yolo* objectDetector;

JULAYOM(void, create)(JNIEnv * env, jobject obj,
                      jstring cfg_file,
                      jstring weights_file,
                      jfloat conf_thr,
                      jstring classNames_file);

JULAYOM(jdouble, classify)(JNIEnv * env, jobject obj,
                        jlong input_frame, jfloatArray results);

#ifdef __cplusplus
}
#endif

#endif
