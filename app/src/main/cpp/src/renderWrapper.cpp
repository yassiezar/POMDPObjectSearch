//
// Created by jaycee on 11/6/18.
//

#include <renderWrapper.hpp>

#ifdef __cplusplus
extern "C" {
#endif

JULAYOM(void, nativeCreateRenderer)(JNIEnv* env, jobject obj)
{
    RendererContext::createContext(env, obj);
}

JULAYOM(void, nativeDestroyRenderer)(JNIEnv* env, jobject obj)
{
    RendererContext::deleteContext(env, obj);
}

JULAYOM(void, nativeInitRenderer)(JNIEnv* env, jobject obj, jint width, jint height)
{
    RendererContext* context = RendererContext::getContext(env, obj);
    if(context) context->initRenderer((size_t)width, (size_t)height);
    else LOGE("Could not find context");
}

JULAYOM(void, nativeRenderFrame)(JNIEnv* env, jobject obj)
{
    RendererContext* context = RendererContext::getContext(env, obj);
    if(context) context->renderFrame();
}

JULAYOM(void, nativeDrawFrame)(JNIEnv* env, jobject obj, jbyteArray frame, jint width, jint height, jint rotation)
{
    jbyte* framePtr = env->GetByteArrayElements(frame, 0);
    jsize  arrayLen = env->GetArrayLength(frame);

    RendererContext* context = RendererContext::getContext(env, obj);
    if(context) context->drawFrame((uint8_t*)framePtr, (size_t)arrayLen, (size_t)width, (size_t)height, rotation);

    env->ReleaseByteArrayElements(frame, framePtr, 0);
}

#ifdef __cplusplus
}
#endif
