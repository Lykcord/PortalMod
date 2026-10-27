#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform PortalData {
    mat4 PortalMvp;
    mat4 PortalModelView;
    vec4 PortalClipPlane;
    vec4 PortalAnimation;
    vec4 PortalFogColor;
    vec4 PortalSceneUv;
};

layout(location = 0) out vec2 portalUv;
layout(location = 1) out vec4 portalScreenPosition;
layout(location = 2) out float portalClipDistance;

void main() {
    vec2 uv = vec2(gl_VertexIndex & 1, (gl_VertexIndex >> 1) & 1);
    vec4 position = vec4(uv.x, uv.y * 2.0, 0.0, 1.0);
    gl_Position = PortalMvp * position;
    portalScreenPosition = gl_Position;
    portalClipDistance = dot(PortalClipPlane, PortalModelView * position);
    // Keep the opening across the eye plane; the fragment shader restores its depth.
    gl_Position.z = 0.0;
    portalUv = uv;
}
