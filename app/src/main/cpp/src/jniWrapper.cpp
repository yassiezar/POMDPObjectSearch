#include <jniWrapper.hpp>
#include <GLRenderer/GLUtils.hpp>

#ifdef __cplusplus
extern "C" {
#endif

JULAYOM(bool, initSound)(JNIEnv* env, jobject obj)
{
    soundGenerator = new SoundGenerator::SoundGenerator();
    soundGenerator->init();
    soundGenerator->startSound();

    return true;
}

JULAYOM(bool, killSound)(JNIEnv* env, jobject obj)
{
    soundGenerator->endSound();
    soundGenerator->kill();

    return true;
}

JULAYOM(void, playSound)(JNIEnv* env, jobject obj, jfloatArray src, jfloatArray list, jfloat gain, jfloat pitch)
{
    soundGenerator->play(env, src, list, gain, pitch);
}

JULAYOM(bool, stopSound)(JNIEnv*, jobject)
{
    return soundGenerator->endSound();
}

#ifdef __cplusplus
}
#endif
