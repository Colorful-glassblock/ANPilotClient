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

out vec2 texCoord;
out vec2 localPosition;
out vec2 rectangleSize;
out vec4 tintColor;
out float blurRadius;
out float cornerRadius;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    texCoord = UV0;
    localPosition = vec2(UV2);
    rectangleSize = vec2(float((UV1.y >> 8) & 255), float(UV2.y));
    tintColor = Color;
    blurRadius = float(UV1.x & 255);
    cornerRadius = float(UV1.y & 255);
}
