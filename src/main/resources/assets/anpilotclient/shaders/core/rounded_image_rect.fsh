#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 localPosition;
in vec2 rectangleSize;
in vec4 vertexColor;
in float radius;

out vec4 fragColor;

float roundedBoxDistance(vec2 point, vec2 halfSize, float radiusValue) {
    float safeRadius = min(max(radiusValue, 0.0), min(halfSize.x, halfSize.y));
    vec2 innerHalfSize = max(halfSize - vec2(safeRadius), vec2(0.0));
    vec2 q = abs(point) - innerHalfSize;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - safeRadius;
}

float coverage(float distance, float pixelWidth) {
    float antialias = max(pixelWidth, 1e-4);
    return 1.0 - smoothstep(0.0, antialias, distance);
}

void main() {
    vec2 halfSize = rectangleSize * 0.5;
    vec2 centered = localPosition - halfSize;
    vec2 dx = dFdx(localPosition);
    vec2 dy = dFdy(localPosition);
    float pixelWidth = 0.5 * (length(dx) + length(dy)) + 1e-6;
    float shapeAlpha = coverage(roundedBoxDistance(centered, halfSize, radius), pixelWidth);

    if (shapeAlpha <= 0.0 || vertexColor.a <= 0.0) {
        discard;
    }

    vec2 texCoord = clamp(localPosition / rectangleSize, vec2(0.0), vec2(1.0));
    vec4 texColor = texture(Sampler0, texCoord);
    if (texColor.a <= 0.0) {
        discard;
    }

    fragColor = vec4(texColor.rgb, texColor.a * shapeAlpha) * vertexColor * ColorModulator;
}
