#include <ObjectDetector/ObjectDetector.hpp>
#include <opencv2/core/cvstd.hpp>

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


    cv::Mat Yolo::classify(const cv::Mat &frame)
    {
        cv::Mat result;

        if (result.channels() == 4)
            cvtColor(result, result, cv::COLOR_BGRA2BGR);

        //create the blob for the network
        cv::Mat blob = cv::dnn::blobFromImage(result, 1/255.F, cv::Size(416,416), cv::Scalar(), true, false);
        net.setInput(blob, "data");
        cv::Mat output = net.forward(getOutputsNames());

        for (int i=0; i<output.rows; i++)
        {
            cv::Mat scores = output.row(i).colRange(5, output.cols);

            cv::Point maxLoc;
            double maxVal;
            minMaxLoc(scores, 0, &maxVal, 0, &maxLoc);

            //we look for this idx: 63, 1, 25, 57, 64, 65, 67
            std::vector<int> labels = {1, 25, 57, 63, 64, 65, 67};

            int idx = maxLoc.x;

            if (maxVal > confidenceThreshold && (std::find(labels.begin(), labels.end(), idx+1) != labels.end()))
            {
                //we save the detected objects in a Mat

                cv::Mat tmp = cv::Mat(1, 6, CV_32F);

                tmp.at<float>(0, 4) = idx;
                tmp.at<float>(0, 0) = output.at<float>(i, 0) * frame.cols;
                tmp.at<float>(0, 1) = output.at<float>(i, 1) * frame.rows;
                tmp.at<float>(0, 2) = output.at<float>(i, 2) * frame.cols;
                tmp.at<float>(0, 3) = output.at<float>(i, 3) * frame.rows;
                tmp.at<float>(0, 5) = (float) maxVal;

                result.push_back(tmp);

//            int idx = maxLoc.x;
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
        return result;
    }


    cv::String Yolo::getOutputsNames()
    {
        std::vector<int> outLayers = net.getUnconnectedOutLayers();
        std::vector<cv::String> layers = net.getLayerNames();
        std::vector<cv::String> names = std::vector<cv::String>(outLayers.size());
        for (int i=0; i<outLayers.size(); i++)
            names[i] = layers[outLayers[i]-1];

        return names[1];
    }


    void Yolo::readClassNames(cv::String path)
    {
        std::ifstream infile(path);
        std::string line;
        while( getline(infile, line) )
            classNames.push_back(line);

    }
}


