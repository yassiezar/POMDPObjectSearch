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
            //std::cout << "Model cfg: " << cfg_file << std::endl;
            //std::cout << "Model weigth: " << weights_file << std::endl;
            //import the model of the network
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
        int result_counter = 0;
        cv::Mat frame(input_frame.clone());

        if (frame.channels() == 4)
            cvtColor(input_frame, frame, cv::COLOR_BGRA2BGR);

        //create the blob for the network
        cv::Mat blob = cv::dnn::blobFromImage(frame, 1/255.F, cv::Size(416,416), cv::Scalar(), true, false);
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
                //we save the detected objects in a Mat
//                cv::Mat tmp = cv::Mat(1, 6, CV_32F);
//
//                tmp.at<float>(0, 0) = output.at<float>(i, 0) * frame.cols;
//                tmp.at<float>(0, 1) = output.at<float>(i, 1) * frame.rows;
//                tmp.at<float>(0, 2) = output.at<float>(i, 2) * frame.cols;
//                tmp.at<float>(0, 3) = output.at<float>(i, 3) * frame.rows;
//                tmp.at<float>(0, 4) = idx;
//                tmp.at<float>(0, 5) = (float) maxVal;

                results.push_back(output.at<float>(i, 0) * frame.cols);
                results.push_back(output.at<float>(i, 1) * frame.cols);
                results.push_back(output.at<float>(i, 2) * frame.cols);
                results.push_back(output.at<float>(i, 3) * frame.cols);
                results.push_back(idx);
                results.push_back((float) maxVal);



//            int x = (int) (output.at<float>(i, 0) * frame.cols);
//            int y = (int) (output.at<float>(i, 1) * frame.rows);
//            int w = (int) (output.at<float>(i, 2) * frame.cols);
//            int h = (int) (output.at<float>(i, 3) * frame.rows);

//            Point p1(cvRound(x - w / 2), cvRound(y - h / 2));
//            Point p2(cvRound(x + w / 2), cvRound(y + h / 2));
//
//            Rect object(p1, p2);
//
//            Scalar object_roi_color(0, 255, 0);
//
//            rectangle(result, object, object_roi_color);
//            putText(result, class_names[idx], p1, FONT_HERSHEY_SIMPLEX, 0.75, Scalar(0,255,0), 2);

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


