package dev.photodine.core.engine.history

interface Command {
    fun execute()
    fun undo()
}
