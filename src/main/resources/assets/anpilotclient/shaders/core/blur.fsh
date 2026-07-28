#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 texCoord;
in vec2 localPosition;
in vec2 rectangleSize;
in vec4 tintColor;
in float blurRadius;
in float cornerRadius;

out vec4 fragColor;

float roundedBoxDistance(vec2 point, vec2 halfSize, float radius) {
    vec2 q = abs(point) - halfSize + vec2(radius);
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

void main() {
    vec2 halfSize = rectangleSize * 0.5;
    vec2 centeredPoint = localPosition - halfSize;
    float distance = roundedBoxDistance(centeredPoint, halfSize, cornerRadius);
    float shapeAlpha = 1.0 - smoothstep(-0.75, 0.75, distance);

    if (shapeAlpha <= 0.0) {
        discard;
    }

    vec2 texel = 1.0 / vec2(textureSize(Sampler0, 0));
    vec2 stepSize = texel * max(blurRadius, 1.0);

    vec4 color = texture(Sampler0, texCoord) * 0.20;
    color += texture(Sampler0, texCoord + vec2(stepSize.x, 0.0)) * 0.12;
    color += texture(Sampler0, texCoord - vec2(stepSize.x, 0.0)) * 0.12;
    color += texture(Sampler0, texCoord + vec2(0.0, stepSize.y)) * 0.12;
    color += texture(Sampler0, texCoord - vec2(0.0, stepSize.y)) * 0.12;
    color += texture(Sampler0, texCoord + vec2(stepSize.x, stepSize.y)) * 0.08;
    color += texture(Sampler0, texCoord + vec2(-stepSize.x, stepSize.y)) * 0.08;
    color += texture(Sampler0, texCoord + vec2(stepSize.x, -stepSize.y)) * 0.08;
    color += texture(Sampler0, texCoord + vec2(-stepSize.x, -stepSize.y)) * 0.08;

    vec4 finalColor = color * tintColor * ColorModulator;
    finalColor.a *= shapeAlpha;
    fragColor = finalColor;
}
