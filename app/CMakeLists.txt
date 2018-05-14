cmake_minimum_required(VERSION 3.11.0)

set(CMAKE_CXX_STANDARD 14)
set(ANDROID_LIB_DIR /home/jaycee/workspace/cpp/AndroidMDP)
set(CMAKE_VERBOSE_MAKEFILE on)

include_directories(/usr/local/include/)
include_directories(/usr/include/eigen3/)
include_directories(src/main/cpp/include/)

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
                    )

add_library(JNI
            SHARED
            src/main/cpp/src/jniWrapper.cpp
            )

target_link_libraries(JNI
                    log
                    MDP
                    )