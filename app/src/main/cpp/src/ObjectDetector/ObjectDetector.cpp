#include <ObjectDetector/ObjectDetector.hpp>
#include <GLRenderer/GLUtils.hpp>

/**
 * The ObjectDetector class provide the functions to create and use a Deep Neural Network based on
 * YOLOv3 algorithm (an object detector). It is implemented using OpenCV 3.4.3 library.
 *
 * @author  Andrea Gaetano Tramontano
 * @version 1.0
 * @since   2018-10-29
*/
namespace ObjectDetector
{
    /**
     * Constructor: initialize the YOLOv3 network with the configuration file (.cfg), the weights of
     * the model (.weights) and with the minimum confidence threshold
     *
     * @param cfg_file This is the path of the configuration file for the YOLO DNN.
     * @param weights_file This is the path of the weights file for the YOLO DNN.
     * @param confidence_threshold This is the minimum confidence threshold.
     *
    */
    Yolo::Yolo(const cv::String& cfgFile,
               const cv::String& weightsFile,
               const float confidenceThreshold):
            confidenceThreshold(confidenceThreshold) //initialize confidenceThreshold
    {
        //initialize the DNN with .cfg and .weights files
        try
        {
            net = cv::dnn::readNetFromDarknet(cfgFile, weightsFile);
            net.setPreferableBackend(cv::dnn::DNN_BACKEND_OPENCV);
            net.setPreferableTarget(cv::dnn::DNN_TARGET_CPU);
        }
        catch (cv::Exception& e)
        {
            __android_log_write(ANDROID_LOG_WARN, OBJECTLOG, "WARNING: YOLOClassifier not initialized correctly!");
            std::cout << e.what() << std::endl;
        }
    }

    /**
     * The Classify method provide the object detection of a single frame and return the object
     * detection results. It is possible to specify the object (or the group of object) you are
     * looking for.
     *
     * @param &input_frame This is the frame we want to analyze and where we want to find object.
     *
     * @return vector<float> This the results of the object detection containing the coordinates of
     * the objects bounding box, the index of the label name and the confidence threshold. For every
     * object there are six parameters, so the vector length is a multiple of six
     * (length=6 -> 1 object found, length=12 -> 2 object found).
    */
    std::vector<Recognition> Yolo::classify(const cv::Mat &inputFrame)
    {
        std::vector<Recognition> objectResults;
        cv::Mat frame(inputFrame.clone());

        //the DNN work with BGR images
        if (frame.channels() == 4)
            cvtColor(inputFrame, frame, cv::COLOR_BGRA2BGR);

        //create and set the blob for the DNN. We can change forth parameter if we want to rescale
        // the image
        cv::Mat blob = cv::dnn::blobFromImage(frame, 1/255.F, cv::Size(416,416), cv::Scalar(), true,
                false);
        LOGI("Here");
        net.setInput(blob);

        //compute the object detection
        cv::Mat forwardOutput = net.forward(getOutputsLayerNames());

        //look if there are detected objects
        for (int i = 0; i < forwardOutput.rows; i ++)
        {
            cv::Mat scores = forwardOutput.row(i).colRange(5, forwardOutput.cols);

            cv::Point maxLoc;
            double maxVal;
            //find the max value in the scores Mat
            minMaxLoc(scores, 0, &maxVal, 0, &maxLoc);

            //we look for only this object indexes: 1, 25, 57, 63, 64, 65, 67
            // (person, backpack, chair, tv-monitor, laptop, mouse, keyboard)
            std::vector<size_t> labelsIdx = {1, 25, 57, 63, 64, 65, 67};

            size_t idx = maxLoc.x;

            // condition = TRUE, if an found object belongs to labelsIdx, otherwise condition = FALSE
            // Will be slow for large label vectors
            bool condition = false;
            for(int k = 0; k < labelsIdx.size(); k ++)
                condition = condition || (idx+1 == labelsIdx[k]);

            //we take object with a confidence higher than the minimum threshold
            if (maxVal > confidenceThreshold && condition)
            {
                Recognition recognition;
                recognition.x = forwardOutput.at<size_t>(i, 0) * frame.cols; //bounding box X coordinates
                recognition.y = forwardOutput.at<size_t>(i, 1) * frame.cols; //bounding box Y coordinates
                recognition.w = forwardOutput.at<size_t>(i, 2) * frame.cols; //bounding box width
                recognition.h = forwardOutput.at<size_t>(i, 3) * frame.cols; //bounding box high
                recognition.id = idx; //label index
                recognition.conf = (float) maxVal; //confidence value
                objectResults.push_back(recognition);
            }
        }

        return objectResults;
    }

    /**
     * The getOutputsLayerNames method look for the name of the output layers of the DNN.
     *
     * @return String This is the name of the first output layer. We need only the first one and not
     * the other for our purpose.
    */
    cv::String Yolo::getOutputsLayerNames()
    {
        //the unconnected layers are the output ones
        std::vector<int> outLayers = net.getUnconnectedOutLayers();
        std::vector<cv::String> layersName = net.getLayerNames();

        //we can switch between outLayers[0] or outLayers[1]
        return layersName[outLayers[0] - 1];
    }
}


