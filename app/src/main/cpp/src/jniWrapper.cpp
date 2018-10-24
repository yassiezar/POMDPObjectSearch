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
                      jfloat conf_t,
                      jstring classNames ){


    const cv::String& cfg_file = ObjectDetector::jstr2ostr(env, obj, cfg);
    const cv::String& weights_file = ObjectDetector::jstr2ostr(env, obj, weights);
    const float conf_thr = conf_t;
    const cv::String classNames_file = ObjectDetector::jstr2ostr(env, obj, classNames);

    objectDetector = new ObjectDetector::Yolo(cfg_file, weights_file, conf_thr, classNames_file);

}

JULAYOM(jdouble, classify)(JNIEnv * env, jobject obj,
                        jlong input_frame, jfloatArray results){

    cv::Mat& in_frame = *(cv::Mat*) input_frame;

    int e1 = cv::getTickCount();

    std::vector<float> finded_object = objectDetector->classify(in_frame);

    int e2 = cv::getTickCount();
    jdouble time = (e2 - e1)/ cv::getTickFrequency();

    float* array = &finded_object[0];

    int array_size = (int) finded_object.size();
    results = env->NewFloatArray(array_size);
    env->SetFloatArrayRegion(results, 0, array_size, array);

    return time;
}

#ifdef __cplusplus
}
#endif
