#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec2 localPosition;
in vec2 rectangleSize;
in vec4 fillColor;
in vec4 borderColor;
in float radius;
in float borderWidth;

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
    float outerDistance = roundedBoxDistance(centeredPoint, halfSize, radius);
    float outerAlpha = coverage(outerDistance);

    vec2 innerHalfSize = max(halfSize - vec2(borderWidth), vec2(0.0));
    float innerRadius = max(radius - borderWidth, 0.0);
    vec2 innerPoint = localPosition - vec2(borderWidth) - innerHalfSize;
    float innerDistance = roundedBoxDistance(innerPoint, innerHalfSize, innerRadius);
    float innerAlpha = borderWidth <= 0.0 ? outerAlpha : coverage(innerDistance);

    float fillAlpha = innerAlpha;
    float borderAlpha = max(outerAlpha - innerAlpha, 0.0);
    float finalAlpha = fillAlpha * fillColor.a + borderAlpha * borderColor.a;

    if (finalAlpha <= 0.0) {
        discard;
    }

    vec3 rgb = (fillColor.rgb * fillAlpha * fillColor.a + borderColor.rgb * borderAlpha * borderColor.a) / finalAlpha;
    fragColor = vec4(rgb, finalAlpha) * ColorModulator;
}
