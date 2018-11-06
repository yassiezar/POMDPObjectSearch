#ifndef POMDPOBJECTSEARCH_RENDERWRAPPER_HPP
#define POMDPOBJECTSEARCH_RENDERWRAPPER_HPP

#include <jni.h>
#include <android/log.h>

#include <GLRenderer/RendererContext.hpp>

#ifdef __cplusplus
extern "C" {
#endif

#define JULAYOM(rettype, name)                                             \
rettype JNIEXPORT JNICALL Java_com_example_jaycee_pomdpobjectsearch_rendering_CameraRenderer_##name

JULAYOM(void, nativeCreateRenderer)(JNIEnv*, jobject);
JULAYOM(void, nativeDestroyRenderer)(JNIEnv*, jobject);
JULAYOM(void, nativeInitRenderer)(JNIEnv*, jobject, jint, jint);
JULAYOM(void, nativeRenderFrame)(JNIEnv*, jobject);
JULAYOM(void, nativeDrawFrame)(JNIEnv*, jobject, jbyteArray, jint, jint, jint);

#ifdef __cplusplus
}
#endif

#endif
