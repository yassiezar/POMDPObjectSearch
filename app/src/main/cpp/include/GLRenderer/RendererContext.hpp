#ifndef POMDPOBJECTSEARCH_RENDERERCONTEXT_HPP
#define POMDPOBJECTSEARCH_RENDERERCONTEXT_HPP

#include <GLRenderer/Renderer.hpp>

#include <jni.h>

#include <memory>

class RendererContext
{
public:
    struct jni_fields
    {
        jfieldID context;
    };

    RendererContext();
    ~RendererContext();

    void initRenderer(size_t, size_t);
    void renderFrame();
    void drawFrame(uint8_t*, size_t, size_t, size_t, int);

    static void createContext(JNIEnv*, jobject);
    static void storeContext(JNIEnv*, jobject, RendererContext*);
    static void deleteContext(JNIEnv*, jobject);
    static RendererContext* getContext(JNIEnv*, jobject);

private:
    std::unique_ptr<Renderer> pVideoRenderer;

    static jni_fields jniFields;
};

#endif
