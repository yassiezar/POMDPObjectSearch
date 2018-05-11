# CMAKE generated file: DO NOT EDIT!
# Generated by "Unix Makefiles" Generator, CMake Version 3.11

# Delete rule output on recipe failure.
.DELETE_ON_ERROR:


#=============================================================================
# Special targets provided by cmake.

# Disable implicit rules so canonical targets will work.
.SUFFIXES:


# Remove some rules from gmake that .SUFFIXES does not remove.
SUFFIXES =

.SUFFIXES: .hpux_make_needs_suffix_list


# Produce verbose output by default.
VERBOSE = 1

# Suppress display of executed commands.
$(VERBOSE).SILENT:


# A target that is always out of date.
cmake_force:

.PHONY : cmake_force

#=============================================================================
# Set environment variables for the build.

# The shell in which to execute make rules.
SHELL = /bin/sh

# The CMake executable.
CMAKE_COMMAND = /usr/local/bin/cmake

# The command to remove a file.
RM = /usr/local/bin/cmake -E remove -f

# Escaping for special characters.
EQUALS = =

# The top-level source directory on which CMake was run.
CMAKE_SOURCE_DIR = /home/jaycee/AndroidStudioProjects/POMDPObjectSearch/app

# The top-level build directory on which CMake was run.
CMAKE_BINARY_DIR = /home/jaycee/AndroidStudioProjects/POMDPObjectSearch/app

# Include any dependencies generated for this target.
include CMakeFiles/JNI.dir/depend.make

# Include the progress variables for this target.
include CMakeFiles/JNI.dir/progress.make

# Include the compile flags for this target's objects.
include CMakeFiles/JNI.dir/flags.make

CMakeFiles/JNI.dir/src/main/cpp/src/jniWrapper.cpp.o: CMakeFiles/JNI.dir/flags.make
CMakeFiles/JNI.dir/src/main/cpp/src/jniWrapper.cpp.o: src/main/cpp/src/jniWrapper.cpp
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green --progress-dir=/home/jaycee/AndroidStudioProjects/POMDPObjectSearch/app/CMakeFiles --progress-num=$(CMAKE_PROGRESS_1) "Building CXX object CMakeFiles/JNI.dir/src/main/cpp/src/jniWrapper.cpp.o"
	/usr/bin/c++  $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -o CMakeFiles/JNI.dir/src/main/cpp/src/jniWrapper.cpp.o -c /home/jaycee/AndroidStudioProjects/POMDPObjectSearch/app/src/main/cpp/src/jniWrapper.cpp

CMakeFiles/JNI.dir/src/main/cpp/src/jniWrapper.cpp.i: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Preprocessing CXX source to CMakeFiles/JNI.dir/src/main/cpp/src/jniWrapper.cpp.i"
	/usr/bin/c++ $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -E /home/jaycee/AndroidStudioProjects/POMDPObjectSearch/app/src/main/cpp/src/jniWrapper.cpp > CMakeFiles/JNI.dir/src/main/cpp/src/jniWrapper.cpp.i

CMakeFiles/JNI.dir/src/main/cpp/src/jniWrapper.cpp.s: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Compiling CXX source to assembly CMakeFiles/JNI.dir/src/main/cpp/src/jniWrapper.cpp.s"
	/usr/bin/c++ $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -S /home/jaycee/AndroidStudioProjects/POMDPObjectSearch/app/src/main/cpp/src/jniWrapper.cpp -o CMakeFiles/JNI.dir/src/main/cpp/src/jniWrapper.cpp.s

# Object files for target JNI
JNI_OBJECTS = \
"CMakeFiles/JNI.dir/src/main/cpp/src/jniWrapper.cpp.o"

# External object files for target JNI
JNI_EXTERNAL_OBJECTS =

libJNI.so: CMakeFiles/JNI.dir/src/main/cpp/src/jniWrapper.cpp.o
libJNI.so: CMakeFiles/JNI.dir/build.make
libJNI.so: libMDP.a
libJNI.so: libAndroidMDP.a
libJNI.so: CMakeFiles/JNI.dir/link.txt
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green --bold --progress-dir=/home/jaycee/AndroidStudioProjects/POMDPObjectSearch/app/CMakeFiles --progress-num=$(CMAKE_PROGRESS_2) "Linking CXX shared library libJNI.so"
	$(CMAKE_COMMAND) -E cmake_link_script CMakeFiles/JNI.dir/link.txt --verbose=$(VERBOSE)

# Rule to build all files generated by this target.
CMakeFiles/JNI.dir/build: libJNI.so

.PHONY : CMakeFiles/JNI.dir/build

CMakeFiles/JNI.dir/clean:
	$(CMAKE_COMMAND) -P CMakeFiles/JNI.dir/cmake_clean.cmake
.PHONY : CMakeFiles/JNI.dir/clean

CMakeFiles/JNI.dir/depend:
	cd /home/jaycee/AndroidStudioProjects/POMDPObjectSearch/app && $(CMAKE_COMMAND) -E cmake_depends "Unix Makefiles" /home/jaycee/AndroidStudioProjects/POMDPObjectSearch/app /home/jaycee/AndroidStudioProjects/POMDPObjectSearch/app /home/jaycee/AndroidStudioProjects/POMDPObjectSearch/app /home/jaycee/AndroidStudioProjects/POMDPObjectSearch/app /home/jaycee/AndroidStudioProjects/POMDPObjectSearch/app/CMakeFiles/JNI.dir/DependInfo.cmake --color=$(COLOR)
.PHONY : CMakeFiles/JNI.dir/depend

