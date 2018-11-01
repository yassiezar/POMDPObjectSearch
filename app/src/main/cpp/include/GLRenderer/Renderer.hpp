#ifndef POMDPOBJECTSEARCH_RENDERER_HPP
#define POMDPOBJECTSEARCH_RENDERER_HPP

#include <GLRenderer/GLUtils.hpp>

#include <stdint.h>

#include <memory>

enum { YUV420, YUV420_FILTER };

struct camera_frame
{
    size_t width;
    size_t height;
    size_t stride_y;
    size_t stride_uv;
    uint8_t* y;
    uint8_t* u;
    uint8_t* v;
};

class Renderer
{
public:
    Renderer();
    virtual ~Renderer();

    static std::unique_ptr<Renderer> createRenderer(int type);

    virtual void initRenderer(size_t width, size_t height) = 0;
    virtual void renderFrame() = 0;
    virtual void updateFrame(const camera_frame& frame) = 0;
    virtual void drawFrame(uint8_t* buffer, size_t length, size_t width, size_t height, int rotation) = 0;
    virtual bool createTextures() = 0;
    virtual bool updateTextures() = 0;
    virtual void deleteTextures() = 0;
    virtual GLuint createProgram(const char *pVertexSource, const char *pFragmentSource) = 0;
    virtual GLuint useProgram() = 0;

protected:
    GLuint program;
    GLuint vertexShader;
    GLuint pixelShader;

    size_t width;
    size_t height;
    size_t backingWidth;
    size_t backingHeight;

    bool isDirty;
    bool isProgramChanged;
};

#endif
