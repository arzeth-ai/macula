package net.mine_diver.macula.shader;

import net.mine_diver.macula.util.GLUtils;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.ARBFramebufferObject.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER;

public class ShadowMap {
    public static boolean shadowEnabled = false;
    public static int shadowResolution = 1024;
    public static float shadowMapHalfPlane = 30f;

    private static final float NEAR = 0.05f;
    private static final float FAR = 256f;

    public static boolean isShadowPass = false;

    public static int shadowFramebufferId = 0;
    public static int shadowDepthTextureId = 0;
    private static int shadowDepthBufferId = 0;

    public static void setupShadowViewport(float f, Vector3f cameraPosition) {
        glViewport(0, 0, shadowResolution, shadowResolution);

        GLUtils.glSetupOrthographicProjection(
                -shadowMapHalfPlane, shadowMapHalfPlane,
                -shadowMapHalfPlane, shadowMapHalfPlane,
                NEAR, FAR
        );

        glTranslatef(0f, 0f, -100f);
        glRotatef(90f, 0f, 0f, -1f);

        float angle = ShaderCore.MINECRAFT.world.method_198(f) * 360f;
        if (angle < 90f || angle > 270f)
            glRotatef(angle - 90f, -1f, 0f, 0f); // Daytime
        else
            glRotatef(angle + 90f, -1f, 0f, 0f); // Nighttime

        // Reduces jitter
        glTranslatef(
                cameraPosition.x % 10f - 5f,
                cameraPosition.y % 10f - 5f,
                cameraPosition.z % 10f - 5f
        );
    }

    public static void initializeShadowMap() {
        if (!shadowEnabled) return;

        createShadowFramebuffer();

        createShadowDepthBuffer();

        createShadowDepthTexture();

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE)
            System.err.println("Failed creating shadow framebuffer! (Status " + status + ")");
    }

    private static void createShadowFramebuffer() {
        if (shadowFramebufferId != 0) glDeleteFramebuffers(shadowFramebufferId);

        shadowFramebufferId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, shadowFramebufferId);

        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);
    }

    private static void createShadowDepthBuffer() {
        if (shadowDepthBufferId != 0) glDeleteFramebuffers(shadowDepthBufferId);

        shadowDepthBufferId = GLUtils.glCreateDepthBuffer(shadowResolution, shadowResolution);

        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, shadowDepthBufferId);
    }

    private static void createShadowDepthTexture() {
        if (shadowDepthTextureId != 0) glDeleteTextures(shadowDepthTextureId);

        shadowDepthTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, shadowDepthTextureId);

        // Set texture wrapping
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);

        // Set linear filtering
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        ByteBuffer shadowMapBuffer = ByteBuffer.allocateDirect(shadowResolution * shadowResolution * 4);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, shadowResolution, shadowResolution, 0,
                GL_DEPTH_COMPONENT,
                GL_FLOAT, shadowMapBuffer);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, shadowDepthTextureId, 0);
    }
}