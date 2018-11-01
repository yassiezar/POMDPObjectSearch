#include <GLRenderer/Renderer.hpp>
#include <GLRenderer/RendererYUV420.hpp>

Renderer::Renderer() : program(0), vertexShader(0), pixelShader(0), width(0), height(0),
                       backingWidth(0), backingHeight(0), isDirty(false), isProgramChanged(false)
{
}

Renderer::~Renderer()
{
    delete_program(program);
}

std::unique_ptr<Renderer> Renderer::createRenderer(int type)
{
    switch(type)
    {
        case YUV420:
        default: return std::unique_ptr<Renderer>(std::make_unique<RendererYUV420>());
    }
}