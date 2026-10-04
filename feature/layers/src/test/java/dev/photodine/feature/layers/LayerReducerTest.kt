package dev.photodine.feature.layers

import dev.photodine.core.engine.BlendMode
import dev.photodine.core.engine.Layer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class LayerReducerTest {

    private val layer1 = Layer(id = UUID.randomUUID(), name = "Layer 1", textureId = 1)
    private val layer2 = Layer(id = UUID.randomUUID(), name = "Layer 2", textureId = 2)

    private val initialState = LayerState(
        layers = listOf(layer1, layer2),
        activeLayerId = layer1.id
    )

    @Test
    fun `select layer updates activeLayerId`() {
        val next = LayerReducer.reduce(initialState, LayerIntent.SelectLayer(layer2.id))
        assertEquals(layer2.id, next.activeLayerId)
    }

    @Test
    fun `select non-existent layer is ignored`() {
        val next = LayerReducer.reduce(initialState, LayerIntent.SelectLayer(UUID.randomUUID()))
        assertEquals(layer1.id, next.activeLayerId)
    }

    @Test
    fun `add layer inserts above active layer and becomes active`() {
        var idCounter = 100
        val next = LayerReducer.reduce(initialState, LayerIntent.AddLayer("New Layer")) { idCounter++ }

        assertEquals(3, next.layers.size)
        // layer1 was active (idx 0), so new layer inserted at idx 1
        assertEquals(layer1.id, next.layers[0].id)
        assertEquals("New Layer", next.layers[1].name)
        assertEquals(layer2.id, next.layers[2].id)
        assertEquals(next.layers[1].id, next.activeLayerId)
    }

    @Test
    fun `delete layer removes target and updates active layer`() {
        val next = LayerReducer.reduce(initialState, LayerIntent.DeleteLayer(layer1.id))
        assertEquals(1, next.layers.size)
        assertEquals(layer2.id, next.layers[0].id)
        assertEquals(layer2.id, next.activeLayerId)
    }

    @Test
    fun `delete last remaining layer is prevented`() {
        val singleLayerState = LayerState(layers = listOf(layer1), activeLayerId = layer1.id)
        val next = LayerReducer.reduce(singleLayerState, LayerIntent.DeleteLayer(layer1.id))
        assertEquals(1, next.layers.size)
        assertEquals(layer1.id, next.layers[0].id)
    }

    @Test
    fun `duplicate layer creates copy with new id directly above source`() {
        var idCounter = 200
        val next = LayerReducer.reduce(initialState, LayerIntent.DuplicateLayer(layer1.id)) { idCounter++ }

        assertEquals(3, next.layers.size)
        assertEquals(layer1.id, next.layers[0].id)
        val dup = next.layers[1]
        assertNotEquals(layer1.id, dup.id)
        assertEquals("${layer1.name} copy", dup.name)
        assertEquals(layer1.blendMode, dup.blendMode)
        assertEquals(layer1.opacity, dup.opacity)
        assertEquals(layer2.id, next.layers[2].id)
        assertEquals(dup.id, next.activeLayerId)
    }

    @Test
    fun `reorder moves layer from source index to destination index`() {
        val next = LayerReducer.reduce(initialState, LayerIntent.ReorderLayer(fromIndex = 0, toIndex = 1))
        assertEquals(listOf(layer2.id, layer1.id), next.layers.map { it.id })
    }

    @Test
    fun `reorder out of bounds is ignored`() {
        val oob1 = LayerReducer.reduce(initialState, LayerIntent.ReorderLayer(fromIndex = -1, toIndex = 1))
        assertEquals(initialState.layers, oob1.layers)

        val oob2 = LayerReducer.reduce(initialState, LayerIntent.ReorderLayer(fromIndex = 0, toIndex = 99))
        assertEquals(initialState.layers, oob2.layers)
    }

    @Test
    fun `set visibility updates layer visibility`() {
        val next = LayerReducer.reduce(initialState, LayerIntent.SetVisibility(layer1.id, visible = false))
        assertFalse(next.layers[0].visible)
        assertTrue(next.layers[1].visible)

        val back = LayerReducer.reduce(next, LayerIntent.SetVisibility(layer1.id, visible = true))
        assertTrue(back.layers[0].visible)
    }

    @Test
    fun `set opacity clamps between 0 and 1`() {
        val normal = LayerReducer.reduce(initialState, LayerIntent.SetOpacity(layer1.id, 0.42f))
        assertEquals(0.42f, normal.layers[0].opacity, 0.001f)

        val clampedLow = LayerReducer.reduce(initialState, LayerIntent.SetOpacity(layer1.id, -0.5f))
        assertEquals(0f, clampedLow.layers[0].opacity)

        val clampedHigh = LayerReducer.reduce(initialState, LayerIntent.SetOpacity(layer1.id, 1.5f))
        assertEquals(1f, clampedHigh.layers[0].opacity)
    }

    @Test
    fun `set blend mode assigns each enum value`() {
        for (mode in BlendMode.entries) {
            val next = LayerReducer.reduce(initialState, LayerIntent.SetBlendMode(layer1.id, mode))
            assertEquals(mode, next.layers[0].blendMode)
        }
    }

    @Test
    fun `toggle expanded switches state`() {
        assertFalse(initialState.isExpanded)
        val expanded = LayerReducer.reduce(initialState, LayerIntent.ToggleExpanded)
        assertTrue(expanded.isExpanded)
        val collapsed = LayerReducer.reduce(expanded, LayerIntent.ToggleExpanded)
        assertFalse(collapsed.isExpanded)
    }
}
