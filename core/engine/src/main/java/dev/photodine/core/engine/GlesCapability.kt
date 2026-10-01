package dev.photodine.core.engine

/**
 * Result of the startup GLES 3.0 conformance capability check.
 *
 * @param versionString raw `glGetString(GL_VERSION)` output, null if no GL context exists.
 * @param isEs3 true when the driver reports an OpenGL ES 3.x context.
 * @param cpuFallback true when the device failed the check; interactive preview keeps running
 *   best-effort on GL while downstream tickets build the CPU-compositing fallback path
 *   (`android.graphics.BlendMode`). A warning is logged whenever this flips to true.
 */
data class GlesCapability(
    val versionString: String?,
    val isEs3: Boolean,
    val cpuFallback: Boolean
)
