#include <GLRenderer/RendererContext.hpp>

RendererContext::jni_fields RendererContext::jniFields = { 0L };

RendererContext::RendererContext()
{
    pVideoRenderer = Renderer::createRenderer(YUV420);
}

RendererContext::~RendererContext(){ }

void RendererContext::initRenderer(size_t width, size_t height)
{
    pVideoRenderer->initRenderer(width, height);
}

void RendererContext::renderFrame()
{
    pVideoRenderer->renderFrame();
}

void RendererContext::drawFrame(uint8_t* buffer, size_t length, size_t width, size_t height, int rotation)
{
    pVideoRenderer->drawFrame(buffer, length, width, height, rotation);
}

void RendererContext::createContext(JNIEnv* env, jobject obj)
{
    RendererContext* context = new RendererContext();
    storeContext(env, obj, context);
}

void RendererContext::storeContext(JNIEnv* env, jobject obj, RendererContext* context)
{
    jclass cls = env->GetObjectClass(obj);

    if(cls == NULL)
    {
        LOGE("Could not find com/media/camera2glpreview/render/VideoRenderer.");
        return;
    }

    jniFields.context = env->GetFieldID(cls, "glContext", "J");
    if(jniFields.context == NULL)
    {
        LOGE("Could not find glContext.");
        return;
    }

    env->SetLongField(obj, jniFields.context, (jlong)context);
}

void RendererContext::deleteContext(JNIEnv* env, jobject obj)
{
    if(jniFields.context == NULL)
    {
        LOGE("Could not find mNativeContext.");
        return;
    }

    RendererContext* context = reinterpret_cast<RendererContext*>(env->GetLongField(obj, jniFields.context));

    if(context) delete context;

    env->SetLongField(obj, jniFields.context, 0L);
}

RendererContext* RendererContext::getContext(JNIEnv* env, jobject obj)
{
    if(jniFields.context == NULL)
    {
        LOGE("Could not find mNativeContext.");
        return NULL;
    }

    RendererContext* context = reinterpret_cast<RendererContext*>(env->GetLongField(obj, jniFields.context));

    return context;
}
