#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec2 localPosition;
in vec2 rectangleSize;
in float radius;
in vec4 vertexColor;
out vec4 fragColor;

float roundedBoxDistance(vec2 point, vec2 halfSize, float radius) {
    vec2 q = abs(point) - halfSize + vec2(radius);
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

float coverage(float distance) {
    float antialias = max(fwidth(distance), 1e-4);
    return 1.0 - smoothstep(0.0, antialias, distance);
}

void main() {
    vec2 halfSize = rectangleSize * 0.5;
    vec2 centeredPoint = localPosition - halfSize;
    float distance = roundedBoxDistance(centeredPoint, halfSize, radius);
    float alpha = coverage(distance);
    if (alpha <= 0.0) {
        discard;
    }
    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha) * ColorModulator;
}
