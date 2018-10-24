#include <string>

#include <jni.h>

#include <opencv2/core.hpp>

namespace ObjectDetector
{
    std::string jstr2str(JNIEnv * env, jobject obj, jstring jstr);
    jstring str2jstr(JNIEnv * env, jobject obj, std::string str);
    cv::String jstr2ostr(JNIEnv * env, jobject obj, jstring jstr);
}