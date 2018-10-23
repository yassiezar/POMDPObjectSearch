cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_VERBOSE_MAKEFILE on)

set(libs "${CMAKE_SOURCE_DIR}/src/main/jniLibs") #for OpenCV

include_directories(${PROJECT_SOURCE_DIR}/src/main/cpp/include/)
include_directories(/home/user/openal-soft/include/)

add_library( libopencv_java3 SHARED IMPORTED )  #for OpenCV
set_target_properties(libopencv_java3 PROPERTIES  #for OpenCV
        IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_java3.so"
        )

add_library(openal SHARED IMPORTED )
set_target_properties(openal PROPERTIES IMPORTED_LOCATION ${PROJECT_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libopenal.so )

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11 -fexceptions -frtti") #for OpenCV

add_library(SoundGenerator
            SHARED
            src/main/cpp/src/SoundGenerator/SoundGenerator.cpp
            )

target_link_libraries(SoundGenerator
                      openal
                      log
                      )

add_library(JNI
            SHARED
            src/main/cpp/src/jniWrapper.cpp
            )

target_link_libraries(JNI
                    SoundGenerator
                    )


add_library( #for OpenCV
        yolo

        SHARED

        src/main/cpp/src/ObjectDetector/Yolo.cpp
        src/main/cpp/src/ObjectDetector/YoloJNI.cpp )

find_library( #for OpenCV
        log-lib

        log)

target_link_libraries( #for OpenCV
        yolo android log

        libopencv_java3
        ${log-lib} )