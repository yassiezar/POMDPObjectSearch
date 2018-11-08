#ifndef OBJECTDETECTOR_OBJECTDETECTOR_HPP
#define OBJECTDETECTOR_OBJECTDETECTOR_HPP

#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/video.hpp>
#include <opencv2/objdetect.hpp>
#include <opencv2/dnn.hpp>
#include <opencv2/dnn/shape_utils.hpp>
#include <opencv2/highgui.hpp>

#include <fstream>
#include <string>

#include <jni.h>

#include <android/log.h>

#define OBJECTLOG "ObjectDetector"

#ifdef __cplusplus
extern "C" {
#endif

namespace ObjectDetector
{
    typedef struct Recognition
    {

    };

    class Yolo
    {
    private:
        cv::dnn::Net net;
        const double confidenceThreshold;

        cv::String getOutputsLayerNames();

    public:
        Yolo(const cv::String&, const cv::String&, const float);
        std::vector<float> classify(const cv::Mat&);
    };
}

#ifdef __cplusplus
}
#endif

#endif
