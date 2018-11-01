#ifndef POMDPOBJECTSEARCH_RENDERERYUV420_HPP
#define POMDPOBJECTSEARCH_RENDERERYUV420_HPP

#include <GLRenderer/Renderer.hpp>

class RendererYUV420 : public Renderer
{
public:
    RendererYUV420();
    virtual ~RendererYUV420();

    virtual void initRenderer(size_t, size_t) override;
    virtual void renderFrame() override;
    virtual void updateFrame(const camera_frame&) override;
    virtual void drawFrame(uint8_t*, size_t, size_t, size_t, int) override;
    virtual bool createTextures() override;
    virtual bool updateTextures() override;
    virtual void deleteTextures() override;
    virtual GLuint createProgram(const char*, const char*) override;
    virtual GLuint useProgram() override;

private:
    std::unique_ptr<uint8_t[]> pDataY;

    uint8_t* pDataU;
    uint8_t* pDataV;

    size_t length;
    size_t sizeY;
    size_t sizeU;
    size_t sizeV;

    GLuint textureIdY;
    GLuint textureIdU;
    GLuint textureIdV;

    GLuint vertexPos;
    GLuint textureLoc;
    GLint textureYLoc;
    GLint textureULoc;
    GLint textureVLoc;
    GLint uniformProjection;
    GLint uniformRotation;
    GLint uniformScale;

    int rotation;
};

#endif
