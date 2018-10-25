#include <ObjectDetector/ObjectDetector.hpp>
#include <opencv2/core/cvstd.hpp>

#include <android/log.h>
#define APPNAME "DETECTED_OBJECT"
#define LOGD(TAG) __android_log_print(ANDROID_LOG_DEBUG , APPNAME, TAG);

namespace ObjectDetector
{
    Yolo::Yolo(const cv::String& cfg_file,
               const cv::String& weights_file,
               const float conf_thr,
               const cv::String classNames_file):
            confidenceThreshold(conf_thr)
    {
        readClassNames(classNames_file);

        try
        {
            net = cv::dnn::readNetFromDarknet(cfg_file, weights_file);
            net .setPreferableBackend(cv::dnn::DNN_BACKEND_OPENCV);
            net.setPreferableTarget(cv::dnn::DNN_TARGET_CPU);
        }
        catch (cv::Exception& e)
        {
            std::cout << "WARNING: YOLOClassifier not initialized correctly!"
                      << std::endl;
            std::cout << e.what() << std::endl;
        }
    }


    std::vector<float> Yolo::classify(const cv::Mat &input_frame)
    {

        std::vector<float> results;
        cv::Mat frame(input_frame.clone());

        if (frame.channels() == 4)
            cvtColor(input_frame, frame, cv::COLOR_BGRA2BGR);

        //create the blob for the network
        cv::Mat blob = cv::dnn::blobFromImage(frame, 1/255.F, cv::Size(416,416), cv::Scalar(0.5,0.5), true, false);
        net.setInput(blob, "data");
        cv::Mat output = net.forward(getOutputsNames());

        for (int i=0; i<output.rows; i++)
        {
            cv::Mat scores = output.row(i).colRange(5, output.cols);

            cv::Point maxLoc;
            double maxVal;
            minMaxLoc(scores, 0, &maxVal, 0, &maxLoc);

            //we look for this idx: 63, 1, 25, 57, 64, 65, 67
            std::vector<int> labelsIdx = {1, 25, 57, 63, 64, 65, 67};

            int idx = maxLoc.x;

            bool condition;
            for(int i=0; i<labelsIdx.size(); i++)
                condition = condition || (idx+1 == labelsIdx[i]);

            if (maxVal > 0)// && condition)
            {

                results.push_back(output.at<float>(i, 0) * frame.cols);
                results.push_back(output.at<float>(i, 1) * frame.cols);
                results.push_back(output.at<float>(i, 2) * frame.cols);
                results.push_back(output.at<float>(i, 3) * frame.cols);
                results.push_back(idx);
                results.push_back((float) maxVal);

            }

        }

        return results;
    }


    cv::String Yolo::getOutputsNames()
    {
        std::vector<int> outLayers = net.getUnconnectedOutLayers();
        std::vector<cv::String> layers = net.getLayerNames();
        std::vector<cv::String> names = std::vector<cv::String>(outLayers.size());
        for (int i=0; i<outLayers.size(); i++)
            names[i] = layers[outLayers[i]-1];

        return names[0];
    }


    void Yolo::readClassNames(cv::String path)
    {
        std::ifstream infile(path);
        std::string line;
        while( getline(infile, line) )
            classNames.push_back(line);

    }
}


