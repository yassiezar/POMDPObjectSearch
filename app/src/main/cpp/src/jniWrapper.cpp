#include <jniWrapper.hpp>

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

JULAYOM(void, create)(JNIEnv * env, jobject obj,
                      jstring cfg,
                      jstring weights,
                      jfloat confThreshold)
{
    const cv::String& cfgFile = ObjectDetector::jstr2cvstr(env, obj, cfg);
    const cv::String& weightsFile = ObjectDetector::jstr2cvstr(env, obj, weights);
    const float confThreshold_ = confThreshold;

    objectDetector = new ObjectDetector::Yolo(cfgFile, weightsFile, confThreshold_);

}

JULAYOM(jfloatArray, classify)(JNIEnv * env, jobject obj, jlong inputFrame)
{
    cv::Mat& inputFrame_ = *(cv::Mat*) inputFrame;

    int64 e1 = cv::getTickCount();

    std::vector<float> foundObjects = objectDetector->classify(inputFrame_);

    int64 e2 = cv::getTickCount();
    jdouble time = (e2 - e1)/cv::getTickFrequency();

    float* objectArray = &foundObjects[0];

    //method to transform a c++ array in a Java array
    int arraySize = (int) foundObjects.size();
    jfloatArray results = env->NewFloatArray(arraySize);
    env->SetFloatArrayRegion(results, 0, arraySize, objectArray);

    return results;
}

#ifdef __cplusplus
}
#endif
