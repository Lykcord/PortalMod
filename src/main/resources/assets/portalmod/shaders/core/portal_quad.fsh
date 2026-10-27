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

#ifdef PORTAL_FRAME
uniform sampler2D FrameTexture;
#else
uniform sampler2D MaskTexture;
#ifdef PORTAL_VIEW
uniform sampler2D SceneTexture;
#endif
#endif

layout(location = 0) in vec2 portalUv;
layout(location = 1) in vec4 portalScreenPosition;
layout(location = 2) in float portalClipDistance;
layout(location = 0) out vec4 fragColor;

void main() {
    if (portalClipDistance < 0.0) {
        discard;
    }
    float depth = portalScreenPosition.z / portalScreenPosition.w;
    if (PortalAnimation.w < 0.5) {
        depth = depth * 0.5 + 0.5;
    }
    gl_FragDepth = clamp(depth, 0.0, 1.0);
#ifdef PORTAL_FRAME
#ifdef PORTAL_HIGHLIGHT
    fragColor = texture(FrameTexture, portalUv);
    if (gl_FrontFacing) {
        fragColor.a *= smoothstep(0.0, 1.0, (PortalAnimation.z - 2.0) / 2.0);
    }
#else
    vec2 uv = vec2(1.0 - portalUv.x, (portalUv.y + PortalAnimation.x) / PortalAnimation.y);
    fragColor = texture(FrameTexture, uv);
#endif
#else
    if (texture(MaskTexture, portalUv).a <= 0.1) {
        discard;
    }
#ifdef PORTAL_VIEW
    vec2 screenUv = portalScreenPosition.xy / portalScreenPosition.w * 0.5 + 0.5;
    fragColor = vec4(texture(SceneTexture, screenUv * PortalSceneUv.xy + PortalSceneUv.zw).rgb, 1.0);
#else
    fragColor = PortalFogColor;
#endif
#endif
}
