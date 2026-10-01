package dev.photodine.core.engine

/**
 * Primary test seam for layer compositing.
 * Full GPU implementation (ping-pong FBOs, cached blend programs) lands in ticket 02.
 */
interface Compositor {
    fun isReady(): Boolean
}
