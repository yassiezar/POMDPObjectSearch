# POMDP Object Search

This repo contains the source code for apps that help people find objects using audio cues to guide the user.
This work was done to complete my PhD and contains the source code that were used to run 2 different experiments.
You are welcome to use the code to further your own work or just to try it out yourself.
The apps performed to expectation.
However, please be aware that the code here was used for purely academic reasons and may therefore contain bad documentation, error handling and optimisation.

## System description

The goal of the app is to guide the user to pointing the phone's camera at a target object.
Fundamentally, the system contains the following elements:

- An object detector
- An audio generation interface
- A control module that generates the guidance location

After a user selects a target object from the system, it tries to find the object as follows. 
The object detector taps into the camera's input and tries to classify any objects it comes across. 
These objects are then fed into a PO/MDP-based control module that generates the location it thinks the object will most likely be located in.
The PO/MDP is trained on the [OpenImages](https://github.com/openimages/dataset) dataset, where it extracts objects' positions relative to each other, which allows it to infer an object's most likely location based on the camera's current observation.
The location, or waypoint, the controller outputs is sent to the audio generator, which transmits a spatialised audio signal to the user.
This signal is spatialised in the horizontal plane, indicating whether the waypoint is to the user's left or right, and indicates the waypoint's elevation angle by adjusting the tone's pitch: a high tone indicates a high elevation, while a low tone indicates a low elevation.
As the user follows the guidance, the signal is adjusted and the controller is updated with new observations.
This process is repeated until the target object is found. 

As mentioned earlier, there are 2 different implementations of the guidance system; one that uses a Markov Decision Process (MDP), and another that uses a Partially Observable MDP (POMDP).
The more basic MDP model was implemented initially to test the concept and uses a set of barcodes that mimics real objects.
After it was successfully implemented and evaluated, it was expanded to a POMDP that takes sensor noise into account. 
The sensor noise comes from the imperfect object detector (based on MobileNet v2.0 and an SSD-lite backend).
The policies were generated using SARSA and PBVI using the implementation from [this](https://github.com/Svalorzen/AI-Toolbox) repository.
The audio interface uses the [OpenAL library](https://github.com/kcat/openal-soft) to spatialise the signal with its default HRTF library.
The signal's other parameters are manually adjusted.

For a more detailed description of the entire system, please see my PhD thesis here [TODO: PUT IN THESIS LINK], or the conference papers I published with the [MDP-based system](http://eprints.lincoln.ac.uk/id/eprint/34596/1/Lock2019.pdf) and the [POMDP-based system](https://link.springer.com/chapter/10.1007/978-3-030-30645-8_59).

## Hardware and software requirements

To be able to run and use the object search apps, you need the following: 

- A phone running Android 7.0 or later that has 
  - ARCore enabled
  - a camera
- A pair of binaural headphones
- Properly encoded QR codes if using the MDP search app

## Installation

### From .apk

** NOTE: ** Only the MDP search app can be installed from the provided APK, since the policy files for the POMDP are beyond Github's allowed limit of 10MB. 

To install the MDP search app, just head to the release tab and download the provided .apk file to your phone. 
On your phone, head to the download location and install the app as normal.
Be sure to enable the capability to install apps from unknown locations. 

### From source

#### MDP
 
To install the MDP from source, clone this repo and check out the `qr-code-experiment` or `qrcode-experiment-dev` branch.
Then, open the project in AndroidStudio, hit compile and run it on your device. 

#### POMDP

To compile and install the POMDP search is a bit more complex.

First, clone this repo and check out the `object-search-experiment` or `object-search-experiment-dev` branch.
Then, download [this](https://drive.google.com/file/d/1FgSHg3DeHHmvaml89lBDeMFe1PEt-1Hp/view?usp=sharing) file and unzip its contents into `/path/to/repo/POMDPObjectSearch/app/src/main/assets/`.
Open the project in AndroidStudio, compile the code and then run it on your device. 

Note that the policy files take long to load and may make it look like your phone is hanging when you select a target object.
Just wait ~20 seconds and it should hopefully load up.

## Expand for your own needs

There are a number of things we'd like to implement:

- Improve the object detector
- Add the ability to tweak audio parameters on the fly in order to improve guidance performance

You might be interested in expanding on these components yourself:

- Implement your own object detector: replace the .tflite model in the /assets/ with your own
- Add other objects: you'll have to train additional policies for these objects and place them in the /assets/ directory.


This project is licensed under the terms of the GPL v3.0 license. 

## Reference

If you found this code useful, please consider adding a reference to this repository in your work:

```
@phdthesis {lock2020,
  author  = {"Lock, Jacobus C. and Bellotto, Nicola and Cielniak, Grzegorz"},
  title   = {"Active Vision-Based Guidance with a Mobile Device for People with Visual Impairments"},
  school  = {"University of Lincoln"},
  address = {"Lincoln, UK"},
  year    = {"2020"},
  month   = {"April"}
}
```
