#include <ObjectDetector/Helper.hpp>

namespace ObjectDetector
{
    std::string jstr2str(JNIEnv * env, jobject obj, jstring jstr)
    {
        const char *str = env->GetStringUTFChars(jstr, 0);
        return str;
    }

    jstring str2jstr(JNIEnv * env, jobject obj, std::string str)
    {

        return env->NewStringUTF(str.c_str());
    }

    cv::String jstr2ostr(JNIEnv * env, jobject obj, jstring jstr)
    {
        return cv::String(jstr2str(env, obj, jstr));

    }
}