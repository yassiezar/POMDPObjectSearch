#ifndef SOUNDGENERATOR_SOUNDGENERATOR_HPP
#define SOUNDGENERATOR_SOUNDGENERATOR_HPP

#include <jni.h>
#include <android/log.h>

#include <cmath>
#include <malloc.h>
#include <cstring>

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

        bool alInit();
        bool alKill();

        bool startSound();
        bool endSound();

        void play(JNIEnv *env, jfloat srcX, jfloatArray list, jfloat gain, jfloat pitch);
        void play(JNIEnv *env, jfloat gain, jfloat pitch);
        void initBuffer(ALuint src, ALuint* buf, jfloat pitch);
        float convertToneToSemitone(float pitch);
        short* generateSoundWave(size_t bufferSize, jfloat pitch, short lastVal, bool onUpSwing);
        bool isSoundPlaying(ALuint src);

    private:
        ALuint targetSrcSound;
        ALuint targetSrcBuf[NUM_BUFFERS];

        ALuint onTargetSound;
        ALuint onTargetBuf[NUM_BUFFERS];

        float notes[NUM_SEMITONES];
    };
}

#endif
