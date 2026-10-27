package net.portalmod.mixins.accessors;

import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface ActiveRenderInfoAccessor {
    @Invoker(value = "setPosition")
    void pmSetPosition(Vec3 value);

    @Accessor(value = "position")
    Vec3 pmGetPosition();

    @Accessor(value = "blockPosition")
    BlockPos.MutableBlockPos pmGetBlockPosition();

    @Accessor(value = "eyeHeight")
    float pmGetEyeHeight();

    @Accessor(value = "eyeHeightOld")
    float pmGetEyeHeightOld();

    @Invoker(value = "setRotation")
    void pmSetRotation(float yaw, float pitch);

    @Accessor("initialized")
    void pmSetInitialized(boolean initialized);

    @Accessor("fov")
    void pmSetFov(float fov);

    @Accessor("hudFov")
    float pmGetHudFov();

    @Accessor("hudFov")
    void pmSetHudFov(float fov);

    @Accessor("depthFar")
    float pmGetDepthFar();

    @Accessor("depthFar")
    void pmSetDepthFar(float distance);

    @Accessor("up")
    Vector3f pmGetUp();

    @Accessor("left")
    Vector3f pmGetLeft();

    @Invoker("setupPerspective")
    void pmSetupPerspective(float near, float far, float fov, float width, float height);

    @Invoker("createProjectionMatrixForCulling")
    Matrix4f pmProjectionForCulling();

    @Invoker("prepareCullFrustum")
    void pmPrepareCullFrustum(Matrix4fc view, Matrix4f projection, Vec3 position);
}
