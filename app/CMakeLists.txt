cmake_minimum_required(VERSION 3.11.2)

set(CMAKE_CXX_STANDARD 14)
set(ANDROID_LIB_DIR /home/jaycee/workspace/cpp/AndroidMDP)
set(CMAKE_VERBOSE_MAKEFILE on)

include_directories(/usr/local/include/)
include_directories(/usr/include/eigen3/)
include_directories(${PROJECT_SOURCE_DIR}/src/main/cpp/include/)
include_directories(/home/jaycee/src/github.com/openal-soft/include/)

add_library(openal SHARED IMPORTED )
set_target_properties(openal PROPERTIES IMPORTED_LOCATION ${PROJECT_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libopenal.so )

add_library(SoundGenerator
            SHARED
            src/main/cpp/src/SoundGenerator/SoundGenerator.cpp
            )

target_link_libraries(SoundGenerator
                      openal
                      log
                      )

add_library(AndroidMDP
            SHARED
            src/main/cpp/src/AndroidMDP/Util/Seed.cpp
            src/main/cpp/src/AndroidMDP/MDP/SparseModel.cpp
            src/main/cpp/src/AndroidMDP/MDP/Algorithms/ValueIteration.cpp
            src/main/cpp/src/AndroidMDP/MDP/Util.cpp
            src/main/cpp/src/AndroidMDP/MDP/Policies/Policy.cpp
            )

add_library(MDP
            SHARED
            src/main/cpp/src/MDP/model.cpp
            src/main/cpp/src/MDP/mdp.cpp
            )

target_link_libraries(MDP
                    AndroidMDP
                    log
                    android
                    )

add_library(JNI
            SHARED
            src/main/cpp/src/jniWrapper.cpp
            )

target_link_libraries(JNI
                    MDP
                    SoundGenerator
                    )