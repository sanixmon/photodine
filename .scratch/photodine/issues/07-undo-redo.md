# 07: Undo / Redo

**What to build:** A user can undo any of the last 50 destructive actions and redo them. This covers brush strokes, eraser strokes, and all layer operations (add, delete, duplicate, reorder, opacity, blend mode, visibility). Undo/redo buttons are always visible in the toolbar.

**Blocked by:** 05 — Brush + Eraser Tool

**Status:** ready-for-agent

- [ ] `HistoryManager` lives in `:core:engine`; it holds a fixed-capacity deque of 50 `Command` objects; when full, the oldest command is evicted and its snapshot memory freed
- [ ] `Command` interface: `execute()` and `undo()`; every mutating engine or layer action is wrapped in a `Command` before being applied
- [ ] **Stroke command**: on `ACTION_DOWN`, the dirty rect of the active layer texture is captured as a CPU-side `ByteBuffer` snapshot; on `ACTION_UP` the command is pushed to the deque; `undo()` restores the snapshot via `glTexSubImage2D` into the layer texture
- [ ] **Layer operation commands**: each layer intent from ticket 04 (add, delete, duplicate, reorder, setOpacity, setBlendMode, setVisibility) is wrapped in a reversible command; layer-add undo deletes the texture and removes the `Layer`; layer-delete undo re-uploads the snapshot texture and re-inserts the `Layer` at the original index
- [ ] Undo and redo buttons in the top toolbar; undo is greyed out when the deque is empty, redo is greyed out when no commands have been undone
- [ ] Performing any new action after an undo clears the redo stack (standard linear history)
- [ ] **Instrumented test**: paint a stroke → undo → assert the layer texture pixel at the stroke centre is transparent (pre-stroke state restored); redo → assert pixel is opaque again; add layer → undo → assert layer list length is restored
