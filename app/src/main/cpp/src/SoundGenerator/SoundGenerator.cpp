#include <SoundGenerator/SoundGenerator.hpp>

namespace SoundGenerator
{
    SoundGenerator::SoundGenerator()
    {
        float note = 16.4;
        float interval = pow(2, 1/12.f);

        for(int i = 0; i < 120; i ++)
        {
            notes[i] = note;
            note *= interval;
        }
    }

    bool SoundGenerator::alInit()
    {
        ALCdevice* device;
        ALCcontext* context;

        device = alcOpenDevice(NULL);
        if(!device)
        {
            __android_log_print(ANDROID_LOG_ERROR, SOUNDLOG, "Error opening device.");

            return -1;
        }

        context = alcCreateContext(device, NULL);
        if(context == NULL || alcMakeContextCurrent(context) == ALC_FALSE)
        {
            if(context != NULL)
            {
                alcDestroyContext(context);
            }
            alcCloseDevice(device);
            __android_log_print(ANDROID_LOG_ERROR, SOUNDLOG, "Error creating context.");

            return -1;
        }

        __android_log_print(ANDROID_LOG_INFO, SOUNDLOG, "OpenAL Sound initialised.");

        return 0;
    }

    bool SoundGenerator::alKill()
    {
        ALCdevice* device = NULL;
        ALCcontext* context = NULL;

        context = alcGetCurrentContext();
        device = alcGetContextsDevice(context);

        alcMakeContextCurrent(NULL);
        alcDestroyContext(context);
        alcCloseDevice(device);

        __android_log_print(ANDROID_LOG_INFO, SOUNDLOG, "OpenAL Sound destroyed.");

        return 0;
    }

    bool SoundGenerator::startSound()
    {
        alGenBuffers(NUM_BUFFERS, onTargetBuf);
        alGenSources(1, &onTargetSound);

        alGenBuffers(NUM_BUFFERS, targetSrcBuf);
        alGenSources(1, &targetSrcSound);

        __android_log_print(ANDROID_LOG_INFO, SOUNDLOG, "Started sound");

        return 0;
    }

    bool SoundGenerator::endSound()
    {
        alDeleteBuffers(NUM_BUFFERS, onTargetBuf);
        alDeleteSources(1, &onTargetSound);

        alDeleteBuffers(NUM_BUFFERS, targetSrcBuf);
        alDeleteSources(1, &targetSrcSound);

        alSourceStop(targetSrcSound);
        alSourceStop(onTargetSound);

        __android_log_print(ANDROID_LOG_INFO, SOUNDLOG, "Ended Sound.");

        return 0;
    }

/*    bool SoundGenerator::endSound()
    {
        alSourceStop(soundSrc);
        playing = false;

        __android_log_print(ANDROID_LOG_INFO, SOUNDLOG, "Ended Sound.");

        return 0;
    }*/

    void SoundGenerator::play(JNIEnv *env, jfloat srcX, jfloatArray list, jfloat gain, jfloat pitch)
    {
        // Get source and listener coords and write to JArray
        jsize listLen = env->GetArrayLength(list);

        jfloat* lList = new float[listLen];

        env->GetFloatArrayRegion(list, 0, listLen, lList);

        // Set source properties
        alSourcef(targetSrcSound, AL_GAIN, gain);

        float orient[6] = { /*fwd:*/ 0.f, 0.f, -1.f, /*up:*/ 0.f, 1.f, 0.f};
        alListenerfv(AL_ORIENTATION, orient);

        // Check to see if target is centre of screen: fix for OpenAL not handling centre sounds properly
        if(sqrt((lList[0] - srcX) * (lList[0] - srcX)) < 0.1)
        // if(abs(acos((lSrc[2]*lSrc[2] + lSrc[2]*lSrc[2] - (lSrc[0] - lList[0])*(lSrc[0] - lList[0]))/(2*lSrc[2]*lSrc[2]))) < 0.1)
        {
            // Set listener position and orientation
            alListener3f(AL_POSITION, 0.f, lList[1], lList[2]);

            // Set source position and orientation
            alSource3f(targetSrcSound, AL_POSITION, 0.f, lList[1], lList[2]);
        }
        else
        {
            // Set listener position and orientation
            alListener3f(AL_POSITION, lList[0], lList[1], lList[2]);

            // Set source position and orientation
            alSource3f(targetSrcSound, AL_POSITION, srcX, lList[1], lList[2]);
        }
        alListener3f(AL_VELOCITY, 0.f, 0.f, 0.f);
        alSource3f(targetSrcSound, AL_VELOCITY, 0.f, 0.f, 0.f);

        alSourcei(targetSrcSound, AL_LOOPING, AL_TRUE);

        pitch = convertToneToSemitone(pitch);
        // __android_log_print(ANDROID_LOG_DEBUG, SOUNDLOG, "pitch: %f", pitch);

        if(!isSoundPlaying(targetSrcSound))
        {
            initBuffer(targetSrcSound, targetSrcBuf, pitch);
        }

        else
        {
            alSourcef(targetSrcSound, AL_PITCH, pitch / 512.f);
        }
    }

    void SoundGenerator::play(JNIEnv *env, jfloat gain, jfloat pitch)
    {
        // Set source properties
        alSourcef(onTargetSound, AL_GAIN, gain);
        alSourcei(onTargetSound, AL_LOOPING, AL_TRUE);

        pitch = convertToneToSemitone(pitch);
        // __android_log_print(ANDROID_LOG_DEBUG, SOUNDLOG, "pitch: %f", pitch);

        if(!isSoundPlaying(onTargetSound))
        {
            initBuffer(onTargetSound, onTargetBuf, pitch);
        }
        else
        {
            alSourcef(onTargetSound, AL_PITCH, pitch / 512.f);
        }
    }

    void SoundGenerator::initBuffer(ALuint src, ALuint* buf, jfloat pitch)
    {
        /*
         * 1. Generate buffers
         * 2. Fill buffers
         * 3. Que buffers
         * 4 . Play source
         */

        size_t bufferSize = SOUND_LEN * SAMPLE_RATE / NUM_BUFFERS;
        short lastVal = 0;
        bool onUpSwing = true;

        for(int i = 0; i < NUM_BUFFERS; i ++)
        {
            short* samples = generateSoundWave(bufferSize, pitch, lastVal, onUpSwing);
            lastVal = samples[bufferSize - 1];
            if(lastVal - samples[bufferSize - 2] > 0)
            {
                onUpSwing = true;
            }
            else
            {
                onUpSwing = false;
            }

            alBufferData(buf[i], AL_FORMAT_MONO16, samples, bufferSize, SAMPLE_RATE);
            free(samples);
        }

        alSourceQueueBuffers(src, NUM_BUFFERS, buf);
        alSourcePlay(src);

        __android_log_print(ANDROID_LOG_INFO, SOUNDLOG, "Playing");
    }

    short* SoundGenerator::generateSoundWave(size_t bufferSize, jfloat pitch, short lastVal, bool onUpSwing)
    {
        // Construct sound buffer
        short *samples = (short*)malloc(bufferSize * sizeof(short));
        memset(samples, 0, bufferSize);

        float phi = (2.f * float(M_PI) * pitch) / SAMPLE_RATE;

        /* Calculate phase shift to make the sines of different buffers align */
        float phase = asin(lastVal / 32760.f);

        for(int i = 0; i < bufferSize; i ++)
        {
            if(onUpSwing)
            {
                samples[i] = 32760 * sin(phi * i + phase);
            }
            else
            {
                samples[i] = 32760 * sin(phi * i - phase + M_PI);
            }

            /*if(i > bufferSize - 5 || i < 5)
            {
                __android_log_print(ANDROID_LOG_INFO, SOUNDLOG, "%d %f", samples[i], phase);
            }*/
        }

        return samples;
    }

    float SoundGenerator::convertToneToSemitone(float pitch)
    {
        float nPitch = 0;

        for(int i = 0; i < NUM_SEMITONES; i ++)
        {
            if(pitch - notes[i] < 0)
            {
                nPitch = notes[i] + 0.5f;
                // __android_log_print(ANDROID_LOG_DEBUG, SOUNDLOG, "Old pitch: %d New pitch: %d", (int)pitch, (int)nPitch);
                break;
            }
        }

        return nPitch;
    }

    bool SoundGenerator::isSoundPlaying(ALuint src)
    {
        ALint state;

        alGetSourcei(src, AL_SOURCE_STATE, &state);

        if(state == AL_PLAYING)
        {
            return true;
        }
        return false;
    }
}