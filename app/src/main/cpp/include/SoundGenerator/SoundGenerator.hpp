#ifndef SOUNDGENERATOR_SOUNDGENERATOR_HPP
#define SOUNDGENERATOR_SOUNDGENERATOR_HPP

#include <jni.h>
#include <android/log.h>

#include <math.h>
#include <malloc.h>
#include <string.h>

#include <AL/al.h>
#include <AL/alc.h>
#include <AL/alext.h>

#define SOUNDLOG "SoundGenerator"

#define NUM_BUFFERS 1
#define SOUND_LEN 8
#define SAMPLE_RATE 44100
#define NUM_SEMITONES 120

namespace SoundGenerator
{
    class SoundGenerator
    {
    public:
        SoundGenerator();

        bool init();
        bool kill();

        bool startSound();
        bool endSound();
        bool killSound();

        void startPlay(jfloat pitch);
        void play(JNIEnv *env, jfloatArray src, jfloatArray list, jfloat gain, jfloat pitch);
        float convertToneToSemitone(float pitch);
        short* generateSoundWave(size_t bufferSize, jfloat pitch, short lastVal, bool onUpSwing);
        bool sourcePlaying();

    private:
        ALuint soundSrc;
        ALuint soundBuf[NUM_BUFFERS];
        bool playing = false;
        float notes[NUM_SEMITONES];
    };
}

#endif
