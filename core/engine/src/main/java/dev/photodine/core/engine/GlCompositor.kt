package dev.photodine.core.engine

import javax.inject.Inject

class GlCompositor @Inject constructor() : Compositor {
    @Suppress("FunctionOnlyReturningConstant")
    override fun isReady(): Boolean = false
}
