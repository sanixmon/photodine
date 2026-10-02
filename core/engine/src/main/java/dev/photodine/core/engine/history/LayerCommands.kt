package dev.photodine.core.engine.history

import dev.photodine.core.engine.BrushStamp
import dev.photodine.core.engine.Compositor
import dev.photodine.core.engine.Layer
import java.util.UUID

class LayerAddCommand(
    private val compositor: Compositor,
    private val layer: Layer,
    private val index: Int? = null
) : Command {
    override fun execute() {
        compositor.addLayer(layer, index)
    }

    override fun undo() {
        compositor.removeLayer(layer.id)
    }
}

class LayerDeleteCommand(
    private val compositor: Compositor,
    private val layer: Layer,
    private val index: Int? = null
) : Command {
    override fun execute() {
        compositor.removeLayer(layer.id)
    }

    override fun undo() {
        compositor.addLayer(layer, index)
    }
}

class LayerPropertyCommand(
    private val compositor: Compositor,
    private val oldLayer: Layer,
    private val newLayer: Layer
) : Command {
    override fun execute() {
        compositor.updateLayer(newLayer)
    }

    override fun undo() {
        compositor.updateLayer(oldLayer)
    }
}

class LayerReorderCommand(
    private val compositor: Compositor,
    private val fromIndex: Int,
    private val toIndex: Int
) : Command {
    override fun execute() {
        compositor.reorderLayers(fromIndex, toIndex)
    }

    override fun undo() {
        compositor.reorderLayers(toIndex, fromIndex)
    }
}

class StrokeCommand(
    private val compositor: Compositor,
    private val targetLayerId: UUID,
    private val stamps: List<BrushStamp>,
    private val undoAction: () -> Unit
) : Command {
    override fun execute() {
        compositor.renderStamps(stamps, targetLayerId)
    }

    override fun undo() {
        undoAction()
    }
}
