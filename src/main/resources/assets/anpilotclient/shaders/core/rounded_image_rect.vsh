#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec4 Color;

out vec2 localPosition;
out vec2 rectangleSize;
out vec4 vertexColor;
out float radius;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    localPosition = UV0;
    rectangleSize = vec2(float(UV2.x & 65535), float(UV2.y & 65535));
    vertexColor = Color;
    radius = float(UV1.x & 255) / 8.0;
}
