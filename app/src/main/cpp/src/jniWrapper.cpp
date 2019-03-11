#include <jniWrapper.hpp>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT bool JNICALL
Java_com_example_jaycee_pomdpobjectsearch_JNIBridge_initSound(JNIEnv* env, jobject obj)
{
    soundGenerator.alInit();
    soundGenerator.startSound();

    return true;
}

JNIEXPORT bool JNICALL
Java_com_example_jaycee_pomdpobjectsearch_JNIBridge_killSound(JNIEnv* env, jobject obj)
{
    soundGenerator.endSound();
    soundGenerator.alKill();

    return true;
}

JNIEXPORT void JNICALL
Java_com_example_jaycee_pomdpobjectsearch_JNIBridge_playSound_FFFF(JNIEnv* env, jobject obj, jfloatArray src, jfloatArray list, jfloat gain, jfloat pitch)
{
    soundGenerator.play(env, src, list, gain, pitch);
}

JNIEXPORT void JNICALL
Java_com_example_jaycee_pomdpobjectsearch_JNIBridge_playSound_FF(JNIEnv* env, jobject obj, jfloat gain, jfloat pitch)
{
    soundGenerator.play(env, gain, pitch);
}

/*JNIEXPORT bool JNICALL
Java_com_example_jaycee_pomdpobjectsearch_JNIBridge_stopSound(JNIEnv*, jobject)
{
    soundGenerator.endSound();
}*/

#ifdef __cplusplus
}
#endif
