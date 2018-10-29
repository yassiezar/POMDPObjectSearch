#include <ObjectDetector/Helper.hpp>

/**
 * The Helper class provide functions to transform std::string, jString and cv::String in each other.
 *
 * @author  Andrea Gaetano Tramontano
 * @version 1.0
 * @since   2018-10-29
*/
namespace ObjectDetector
{
    /**
     * The jstr2str method transform a jString in a std::string.
     *
     * @param jstr This is the jString to transform.
     *
     * @return std::string This is the string in c++ format.
    */
    std::string jstr2str(JNIEnv * env, jobject obj, jstring jstr)
    {
        const char *str = env->GetStringUTFChars(jstr, 0);
        return str;
    }

    /**
     * The str2jstr method transform a std::string in a jString.
     *
     * @param std::string This is the string in c++ format.
     *
     * @return jstr This is the jString to transform.
    */
    jstring str2jstr(JNIEnv * env, jobject obj, std::string str)
    {
        return env->NewStringUTF(str.c_str());
    }

    /**
     * The jstr2cvstr method transform a jString in a cv::String.
     *
     * @param jstr This is the jString to transform.
     *
     * @return cv::String This is the string in OpenCV format.
    */
    cv::String jstr2cvstr(JNIEnv * env, jobject obj, jstring jstr)
    {
        return cv::String(jstr2str(env, obj, jstr));

    }
}