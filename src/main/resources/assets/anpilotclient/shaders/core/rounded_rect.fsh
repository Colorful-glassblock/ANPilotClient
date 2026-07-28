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
in float glowRadius;
in float glowAlpha;

out vec4 fragColor;

float roundedBoxDistance(vec2 point, vec2 halfSize, float radius) {
    float safeRadius = min(max(radius, 0.0), min(halfSize.x, halfSize.y));
    vec2 innerHalfSize = max(halfSize - vec2(safeRadius), vec2(0.0));
    vec2 q = abs(point) - innerHalfSize;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - safeRadius;
}

float coverage(float distance, float pixelWidth) {
    float antialias = max(pixelWidth, 1e-4);
    return 1.0 - smoothstep(0.0, antialias, distance);
}

float roundedBoxAlpha(vec2 center, vec2 halfSize, float boxRadius, float pixelWidth, vec2 dx, vec2 dy) {
    vec2 offsets[4] = vec2[4](
        vec2(-0.33, -0.33),
        vec2(0.33, -0.33),
        vec2(0.33, 0.33),
        vec2(-0.33, 0.33)
    );

    float alpha = 0.0;
    for (int i = 0; i < 4; i++) {
        vec2 samplePoint = localPosition + dx * offsets[i].x + dy * offsets[i].y - center;
        alpha += coverage(roundedBoxDistance(samplePoint, halfSize, boxRadius), pixelWidth);
    }
    return alpha * 0.25;
}

void main() {
    vec2 halfSize = rectangleSize * 0.5;
    vec2 dx = dFdx(localPosition);
    vec2 dy = dFdy(localPosition);
    float pixelWidth = 0.5 * (length(dx) + length(dy)) + 1e-6;
    float safeRadius = min(max(radius, 0.0), min(halfSize.x, halfSize.y));
    float shapeAlpha = roundedBoxAlpha(halfSize, halfSize, safeRadius, pixelWidth, dx, dy);
    float fillAlpha = 0.0;

    if (borderWidth <= 0.0) {
        fillAlpha = shapeAlpha;
    } else {
        vec2 innerHalfSize = max(halfSize - vec2(borderWidth), vec2(0.0));
        float innerRadius = max(safeRadius - borderWidth, 0.0);
        vec2 innerCenter = vec2(borderWidth) + innerHalfSize;
        fillAlpha = roundedBoxAlpha(innerCenter, innerHalfSize, innerRadius, pixelWidth, dx, dy);
    }

    float borderAlpha = max(shapeAlpha - fillAlpha, 0.0);
    float finalAlpha = max(fillAlpha * fillColor.a, borderAlpha * borderColor.a);

    if (finalAlpha <= 0.0) {
        discard;
    }

    vec3 rgb = (fillColor.rgb * fillAlpha * fillColor.a + borderColor.rgb * borderAlpha * borderColor.a) / finalAlpha;
    fragColor = vec4(rgb, finalAlpha) * ColorModulator;
}
