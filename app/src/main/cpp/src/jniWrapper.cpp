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

JULAYOM(void, createObjectDetector)(JNIEnv * env, jobject obj,
                      jstring cfg,
                      jstring weights,
                      jfloat confThreshold)
{
    const cv::String& cfgFile = ObjectDetector::jstr2cvstr(env, obj, cfg);
    const cv::String& weightsFile = ObjectDetector::jstr2cvstr(env, obj, weights);
    const float confThreshold_ = confThreshold;

    objectDetector = new ObjectDetector::Yolo(cfgFile, weightsFile, confThreshold_);

}

// JULAYOM(jfloatArray, classify)(JNIEnv * env, jobject obj, jlong inputFrame)
JULAYOM(jfloatArray, classify)(JNIEnv * env, jobject obj, jlong inputFrame)
{
    cv::Mat& inputFrame_ = *(cv::Mat*) inputFrame;

    int64 e1 = cv::getTickCount();

    std::vector<float> foundObjects;// = objectDetector->classify(inputFrame_);

    int64 e2 = cv::getTickCount();
    jdouble time = (e2 - e1)/cv::getTickFrequency();

    float* objectArray = &foundObjects[0];

    //method to transform a c++ array in a Java array
    int arraySize = (int) foundObjects.size();
    jfloatArray results = env->NewFloatArray(arraySize);
    env->SetFloatArrayRegion(results, 0, arraySize, objectArray);

    return results;
}

JULAYOM(jobjectArray, classifyNew)(JNIEnv * env, jobject obj, jbyteArray inputFrameBuffer, jint width, jint height)
{
    jbyte* framePtr = env->GetByteArrayElements(inputFrameBuffer, 0);
    // jsize arrayLen = env->GetArrayLength(inputFrameBuffer);

    cv::Mat inputFrame(height+height/2, width, CV_8UC1, (uint8_t*)framePtr);
    env->ReleaseByteArrayElements(inputFrameBuffer, framePtr, 0);
    cv::cvtColor(inputFrame, inputFrame, CV_YUV2RGBA_NV21);

    // cv::Mat& inputFrame_ = *(cv::Mat*) inputFrame;

    int64 e1 = cv::getTickCount();

    std::vector<ObjectDetector::Recognition> foundObjects = objectDetector->classify(inputFrame);

    int64 e2 = cv::getTickCount();
    jdouble time = (e2 - e1)/cv::getTickFrequency();

    // ObjectDetector::Recognition* objectArray = &foundObjects[0];

    //method to transform a c++ array in a Java array
    jsize arraySize = foundObjects.size();
    LOGI("Found %d objects", arraySize);
    jclass cls = env->FindClass("com/example/jaycee/pomdpobjectsearch/Recognition");
    if(cls == NULL)
    {
        LOGE("Could not find Recognition class");
        return NULL;
    }

    jmethodID constructor = env->GetMethodID(cls, "<init>", "(ILjava/lang/String;Ljava/lang/Float;IIII)V");
    if(constructor == NULL)
    {
        LOGE("Could not find class constructor");
        return NULL;
    }

    jobjectArray results = env->NewObjectArray(arraySize, cls, NULL);

    for(jsize i = 0; i < arraySize; i ++)
    {
        jobject object = env->NewObject(cls, constructor, obj, foundObjects[i].id, foundObjects[i].title.c_str(),
                foundObjects[i].conf, foundObjects[i].x, foundObjects[i].y, foundObjects[i].w, foundObjects[i].h);
        env->SetObjectArrayElement(results, i, object);
    }


    return results;
}

#ifdef __cplusplus
}
#endif
