#include <GLRenderer/RendererYUV420.hpp>
#include <GLRenderer/GLShaders.hpp>

// Vertices for a full screen quad.
static const float kVertices[8] = {
        -1.f, 1.f,
        -1.f, -1.f,
        1.f, 1.f,
        1.f, -1.f
};

// Texture coordinates for mapping entire texture.
static const float kTextureCoords[8] = {
        0, 0,
        0, 1,
        1, 0,
        1, 1
};

RendererYUV420::RendererYUV420() : pDataY(nullptr), pDataU(nullptr), pDataV(nullptr),
                                   length(0), sizeY(0), sizeU(0), sizeV(0),
                                   textureIdY(0), textureIdU(0), textureIdV(0),
                                   textureYLoc(0), textureULoc(0), textureVLoc(0),
                                   vertexPos(0), uniformProjection(0), uniformRotation(0),
                                   uniformScale(0), rotation(0)
{
    isProgramChanged = true;
}

RendererYUV420::~RendererYUV420()
{
    deleteTextures();
    delete_program(program);
}

void RendererYUV420::initRenderer(size_t width, size_t height)
{
    backingWidth = width;
    backingHeight = height;
}

void RendererYUV420::renderFrame()
{
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    glClearColor(0.f, 0.f, 0.f, 1.f);

    if(!updateTextures() || !useProgram()) return;

    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
}

void RendererYUV420::updateFrame(const camera_frame& frame)
{
    sizeY = frame.width * frame.height;
    sizeU = frame.width * frame.height / 4;
    sizeV = frame.width * frame.height / 4;

    if(pDataY == nullptr || width != frame.width || height != frame.height)
    {
        pDataY = std::make_unique<uint8_t[]>(sizeY + sizeU + sizeV);
        pDataU = pDataY.get() + sizeY;
        pDataV = pDataU + sizeU;
    }

    width = frame.width;
    height = frame.height;

    if(width == frame.stride_y)
    {
        memcpy(pDataY.get(), frame.y, sizeY);
    }
    else
    {
        uint8_t* pSrcY = frame.y;
        uint8_t* pDstY = pDataY.get();

        for(int h = 0; h < height; h ++)
        {
            memcpy(pDstY, pSrcY, width);

            pSrcY += frame.stride_y;
            pDstY += width;
        }
    }

    if(width / 2 == frame.stride_uv)
    {
        memcpy(pDataU, frame.u, sizeU);
        memcpy(pDataV, frame.v, sizeV);
    }
    else
    {
        uint8_t* pSrcU = frame.u;
        uint8_t* pSrcV = frame.v;
        uint8_t* pDstU = pDataU;
        uint8_t* pDstV = pDataV;

        for (int h = 0; h < height / 2; h++)
        {
            memcpy(pDstU, pSrcU, width / 2);
            memcpy(pDstV, pSrcV, width / 2);

            pDstU += width / 2;
            pDstV += width / 2;

            pSrcU += frame.stride_uv;
            pSrcV += frame.stride_uv;
        }
    }

    isDirty = true;
}

void RendererYUV420::drawFrame(uint8_t* buffer, size_t length_, size_t width, size_t height, int rotation_)
{
    length = length_;
    rotation = rotation_;

    camera_frame frame;
    frame.width = width;
    frame.height = height;
    frame.stride_y = width;
    frame.stride_uv = width/2;
    frame.y = buffer;
    frame.u = buffer + width*height;
    frame.v = buffer + width*height*5/4;

    updateFrame(frame);
}

bool RendererYUV420::createTextures()
{
    GLsizei widthY = (GLsizei)width;
    GLsizei heightY = (GLsizei)height;

    glActiveTexture(GL_TEXTURE0);
    glGenTextures(1, &textureIdY);
    glBindTexture(GL_TEXTURE_2D, textureIdY);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, widthY, heightY, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, NULL);

    if (!textureIdY)
    {
        check_gl_error("Create Y texture");
        return false;
    }

    GLsizei widthU = (GLsizei)width / 2;
    GLsizei heightU = (GLsizei)height / 2;

    glActiveTexture(GL_TEXTURE1);
    glGenTextures(1, &textureIdU);
    glBindTexture(GL_TEXTURE_2D, textureIdU);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, widthU, heightU, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, NULL);

    if (!textureIdU)
    {
        check_gl_error("Create U texture");
        return false;
    }

    GLsizei widthV = (GLsizei)width / 2;
    GLsizei heightV = (GLsizei)height / 2;

    glActiveTexture(GL_TEXTURE2);
    glGenTextures(1, &textureIdV);
    glBindTexture(GL_TEXTURE_2D, textureIdV);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, widthV, heightV, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, NULL);

    if (!textureIdV)
    {
        check_gl_error("Create V texture");
        return false;
    }

    return true;
}

bool RendererYUV420::updateTextures()
{
    if(!textureIdY && !textureIdU && !textureIdV && !createTextures()) return false;

    if(isDirty)
    {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureIdY);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, (GLsizei)width, (GLsizei)height, 0,
                     GL_LUMINANCE, GL_UNSIGNED_BYTE, pDataY.get());

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, textureIdU);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, (GLsizei)width / 2, (GLsizei)height / 2, 0,
                     GL_LUMINANCE, GL_UNSIGNED_BYTE, pDataU);

        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, textureIdV);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, (GLsizei)width / 2, (GLsizei)height / 2, 0,
                     GL_LUMINANCE, GL_UNSIGNED_BYTE, pDataV);

        isDirty = false;

        return true;
    }

    return false;
}

void RendererYUV420::deleteTextures()
{
    if (textureIdY)
    {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, 0);
        glDeleteTextures(1, &textureIdY);

        textureIdY = 0;
    }

    if (textureIdU)
    {
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, 0);
        glDeleteTextures(1, &textureIdU);

        textureIdU = 0;
    }

    if (textureIdV)
    {
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, 0);
        glDeleteTextures(1, &textureIdV);

        textureIdV = 0;
    }
}

GLuint RendererYUV420::createProgram(const char *pVertexSource, const char *pFragmentSource)
{
    program = create_program(pVertexSource, pFragmentSource, vertexShader, pixelShader);

    if (!program)
    {
        check_gl_error("Create program");
        LOGE("Could not create program.");
        return 0;
    }

    vertexPos = (GLuint)glGetAttribLocation(program, "position");
    uniformProjection = glGetUniformLocation(program, "projection");
    uniformRotation = glGetUniformLocation(program, "rotation");
    uniformScale = glGetUniformLocation(program, "scale");
    textureYLoc = glGetUniformLocation(program, "s_textureY");
    textureULoc = glGetUniformLocation(program, "s_textureU");
    textureVLoc = glGetUniformLocation(program, "s_textureV");
    textureLoc = (GLuint)glGetAttribLocation(program, "texcoord");

    return program;
}

GLuint RendererYUV420::useProgram()
{
    if (!program && !createProgram(kVertexShader, kFragmentShader))
    {
        LOGE("Could not use program.");
        return 0;
    }

    if (isProgramChanged)
    {
        glUseProgram(program);

        check_gl_error("Use program.");

        glVertexAttribPointer(vertexPos, 2, GL_FLOAT, GL_FALSE, 0, kVertices);
        glEnableVertexAttribArray(vertexPos);

        float targetAspectRatio = (float)height / (float)width;

        GLfloat projection[16];
        mat4f_load_ortho(-targetAspectRatio, targetAspectRatio, -1.0f, 1.0f, -1.0f, 1.0f, projection);
        glUniformMatrix4fv(uniformProjection, 1, GL_FALSE, projection);

        GLfloat rotationZ[16];
        mat4f_load_rotation_z(rotation, rotationZ);
        glUniformMatrix4fv(uniformRotation, 1, 0, &rotationZ[0]);

        float scaleFactor = aspect_ratio_correction(false, backingWidth, backingHeight, width, height);
        GLfloat scale[16];
        mat4f_load_scale(scaleFactor, scaleFactor, 1.0f, scale);
        glUniformMatrix4fv(uniformScale, 1, 0, &scale[0]);

        glUniform1i(textureYLoc, 0);
        glUniform1i(textureULoc, 1);
        glUniform1i(textureVLoc, 2);
        glVertexAttribPointer(textureLoc, 2, GL_FLOAT, GL_FALSE, 0, kTextureCoords);
        glEnableVertexAttribArray(textureLoc);

        isProgramChanged = false;
    }

    return program;
}
