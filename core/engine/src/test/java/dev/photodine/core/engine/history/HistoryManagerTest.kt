package dev.photodine.core.engine.history

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HistoryManagerTest {

    private class TestCommand(
        private val onExecute: () -> Unit,
        private val onUndo: () -> Unit
    ) : Command {
        override fun execute() = onExecute()
        override fun undo() = onUndo()
    }

    @Test
    fun `initial history manager is empty`() {
        val history = HistoryManager(maxCapacity = 50)
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
        assertEquals(0, history.undoCount)
        assertEquals(0, history.redoCount)
    }

    @Test
    fun `execute pushes command to undo stack and executes it`() {
        val history = HistoryManager(maxCapacity = 50)
        var executed = false
        val cmd = TestCommand(onExecute = { executed = true }, onUndo = {})

        history.execute(cmd)
        assertTrue(executed)
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)
        assertEquals(1, history.undoCount)
    }

    @Test
    fun `undo executes command undo and moves command to redo stack`() {
        val history = HistoryManager(maxCapacity = 50)
        var value = 0
        val cmd = TestCommand(onExecute = { value = 1 }, onUndo = { value = 0 })

        history.execute(cmd)
        assertEquals(1, value)

        history.undo()
        assertEquals(0, value)
        assertFalse(history.canUndo)
        assertTrue(history.canRedo)
        assertEquals(0, history.undoCount)
        assertEquals(1, history.redoCount)
    }

    @Test
    fun `redo executes command again and moves it back to undo stack`() {
        val history = HistoryManager(maxCapacity = 50)
        var value = 0
        val cmd = TestCommand(onExecute = { value++ }, onUndo = { value-- })

        history.execute(cmd)
        assertEquals(1, value)

        history.undo()
        assertEquals(0, value)

        history.redo()
        assertEquals(1, value)
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)
    }

    @Test
    fun `new command clears redo stack`() {
        val history = HistoryManager(maxCapacity = 50)
        history.execute(TestCommand({}, {}))
        history.execute(TestCommand({}, {}))
        assertEquals(2, history.undoCount)

        history.undo()
        assertTrue(history.canRedo)

        history.execute(TestCommand({}, {}))
        assertFalse(history.canRedo)
        assertEquals(2, history.undoCount)
    }

    @Test
    fun `history capacity enforces max 50 commands evicting oldest`() {
        val history = HistoryManager(maxCapacity = 50)
        for (i in 1..65) {
            history.execute(TestCommand({}, {}))
        }

        assertEquals(50, history.undoCount)
    }
}
