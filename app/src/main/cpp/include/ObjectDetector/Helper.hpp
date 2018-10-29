#include <string>

#include <jni.h>

#include <opencv2/core.hpp>

#ifdef __cplusplus
extern "C" {
#endif

namespace ObjectDetector
{
    std::string jstr2str(JNIEnv*, jobject, jstring);
    jstring str2jstr(JNIEnv*, jobject, std::string);
    cv::String jstr2cvstr(JNIEnv*, jobject, jstring);
}

#ifdef __cplusplus
}
#endif
