package dev.photodine.core.engine.history

/**
 * Manages an undo/redo stack of [Command] objects up to [maxCapacity] (default 50).
 * When full, the oldest command is evicted to bound memory usage.
 * Any new command execution clears the redo stack (linear history).
 */
class HistoryManager(val maxCapacity: Int = DEFAULT_CAPACITY) {

    private val undoStack = ArrayDeque<Command>()
    private val redoStack = ArrayDeque<Command>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val undoCount: Int get() = undoStack.size
    val redoCount: Int get() = redoStack.size

    /**
     * Executes [command] and pushes it onto the history stack.
     * Clears the redo stack.
     */
    fun execute(command: Command) {
        command.execute()
        push(command)
    }

    /**
     * Pushes an already executed [command] onto the undo stack.
     * Clears the redo stack.
     */
    fun push(command: Command) {
        if (undoStack.size >= maxCapacity) {
            undoStack.removeFirst()
        }
        undoStack.addLast(command)
        redoStack.clear()
    }

    /**
     * Undoes the most recent command and pushes it onto the redo stack.
     */
    fun undo() {
        if (undoStack.isEmpty()) return
        val command = undoStack.removeLast()
        command.undo()
        redoStack.addLast(command)
    }

    /**
     * Redoes the most recently undone command and pushes it back onto the undo stack.
     */
    fun redo() {
        if (redoStack.isEmpty()) return
        val command = redoStack.removeLast()
        command.execute()
        undoStack.addLast(command)
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    companion object {
        const val DEFAULT_CAPACITY = 50
    }
}
