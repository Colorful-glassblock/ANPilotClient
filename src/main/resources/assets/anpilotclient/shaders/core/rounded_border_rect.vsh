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
out vec4 fillColor;
out vec4 borderColor;
out float radius;
out float borderWidth;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    localPosition = UV0;
    fillColor = Color;

    int p1x = UV1.x;
    int p1y = UV1.y;
    int p2x = UV2.x;
    int p2y = UV2.y;

    radius = float(p1x & 0xFF) / 8.0;
    borderWidth = float((p1x >> 8) & 0xFF) / 16.0;
    borderColor = vec4(
        float((p2y >> 12) & 0x0F) / 15.0,
        float((p2y >> 8) & 0x0F) / 15.0,
        float(p1y & 0x0F) / 15.0,
        float((p1y >> 4) & 0x0F) / 15.0
    );
    rectangleSize = vec2(
        float((p1y >> 8) & 0xFF) + float(p2x & 0xFF) * 256.0,
        float((p2x >> 8) & 0xFF) + float(p2y & 0xFF) * 256.0
    );
}
