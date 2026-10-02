package dev.photodine.app.pixellab

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PixelLabEditorScreen(
    modifier: Modifier = Modifier,
    viewModel: EditorViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    PixelLabEditorContent(
        state = state,
        onAddText = viewModel::addTextLayer,
        onSave = { /* Save action */ },
        onShare = { /* Share action */ },
        onQuote = {
            viewModel.addTextLayer()
        },
        onMore = { /* More options */ },
        onEditClick = { viewModel.showEditDialog(true) },
        onDeleteClick = viewModel::deleteSelected,
        onUndoClick = viewModel::undo,
        onZoomClick = { /* Zoom */ },
        onToggleGrid = viewModel::toggleGrid,
        onLayersClick = { /* Layers */ },
        onSelectLayer = viewModel::selectLayer,
        onClearSelection = viewModel::clearSelection,
        onTransformLayer = viewModel::updateLayerTransform,
        onCopyClick = viewModel::copySelected,
        onBringToFront = viewModel::bringToFront,
        onSendToBack = viewModel::sendToBack,
        onSelectTab = viewModel::selectTab,
        onDismissEditDialog = { viewModel.showEditDialog(false) },
        onConfirmEditText = { newText ->
            val id = state.selectedId
            if (id != null) {
                viewModel.updateText(id, newText)
            }
        },
        modifier = modifier
    )
}

@Composable
fun PixelLabEditorContent(
    state: EditorState,
    onAddText: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onQuote: () -> Unit,
    onMore: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onUndoClick: () -> Unit,
    onZoomClick: () -> Unit,
    onToggleGrid: () -> Unit,
    onLayersClick: () -> Unit,
    onSelectLayer: (String) -> Unit,
    onClearSelection: () -> Unit,
    onTransformLayer: (id: String, offset: androidx.compose.ui.geometry.Offset, scale: Float, rotation: Float) -> Unit,
    onCopyClick: () -> Unit,
    onBringToFront: () -> Unit,
    onSendToBack: () -> Unit,
    onSelectTab: (EditorTab) -> Unit,
    onDismissEditDialog: () -> Unit,
    onConfirmEditText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var dialogText by remember(state.selectedLayer?.text) {
        mutableStateOf(state.selectedLayer?.text ?: "")
    }

    PixelLabTheme {
        Scaffold(
            topBar = {
                EditorTopBar(
                    selectedId = state.selectedId,
                    showGrid = state.showGrid,
                    onAddClick = onAddText,
                    onSaveClick = onSave,
                    onShareClick = onShare,
                    onQuoteClick = onQuote,
                    onMoreClick = onMore,
                    onEditClick = onEditClick,
                    onDeleteClick = onDeleteClick,
                    onUndoClick = onUndoClick,
                    onZoomClick = onZoomClick,
                    onToggleGrid = onToggleGrid,
                    onLayersClick = onLayersClick
                )
            },
            bottomBar = {
                Column {
                    ContextualToolbar(
                        activeTab = state.activeTab,
                        selectedId = state.selectedId,
                        onAddText = onAddText,
                        onQuoteClick = onQuote,
                        onEditClick = onEditClick,
                        onDeleteClick = onDeleteClick,
                        onCopyClick = onCopyClick,
                        onBringToFront = onBringToFront,
                        onSendToBack = onSendToBack
                    )
                    EditorBottomBar(
                        activeTab = state.activeTab,
                        onSelectTab = onSelectTab
                    )
                }
            },
            modifier = modifier
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                CanvasArea(
                    layers = state.layers,
                    selectedId = state.selectedId,
                    showGrid = state.showGrid,
                    onSelectLayer = onSelectLayer,
                    onClearSelection = onClearSelection,
                    onTransformLayer = onTransformLayer,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (state.isEditingText) {
                AlertDialog(
                    onDismissRequest = onDismissEditDialog,
                    title = { Text("Edit Text") },
                    text = {
                        OutlinedTextField(
                            value = dialogText,
                            onValueChange = { dialogText = it },
                            singleLine = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { onConfirmEditText(dialogText) }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismissEditDialog) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Preview(name = "Tanpa Seleksi", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PreviewEditorWithoutSelection() {
    val sampleLayer = TextLayer(text = "PixelLab Text")
    val state = EditorState(
        layers = listOf(sampleLayer),
        selectedId = null,
        activeTab = EditorTab.Text,
        showGrid = true
    )
    PixelLabEditorContent(
        state = state,
        onAddText = {},
        onSave = {},
        onShare = {},
        onQuote = {},
        onMore = {},
        onEditClick = {},
        onDeleteClick = {},
        onUndoClick = {},
        onZoomClick = {},
        onToggleGrid = {},
        onLayersClick = {},
        onSelectLayer = {},
        onClearSelection = {},
        onTransformLayer = { _, _, _, _ -> },
        onCopyClick = {},
        onBringToFront = {},
        onSendToBack = {},
        onSelectTab = {},
        onDismissEditDialog = {},
        onConfirmEditText = {}
    )
}

@Preview(name = "Teks Terpilih", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PreviewEditorWithSelection() {
    val sampleLayer = TextLayer(text = "Selected Text")
    val state = EditorState(
        layers = listOf(sampleLayer),
        selectedId = sampleLayer.id,
        activeTab = EditorTab.Text,
        showGrid = false
    )
    PixelLabEditorContent(
        state = state,
        onAddText = {},
        onSave = {},
        onShare = {},
        onQuote = {},
        onMore = {},
        onEditClick = {},
        onDeleteClick = {},
        onUndoClick = {},
        onZoomClick = {},
        onToggleGrid = {},
        onLayersClick = {},
        onSelectLayer = {},
        onClearSelection = {},
        onTransformLayer = { _, _, _, _ -> },
        onCopyClick = {},
        onBringToFront = {},
        onSendToBack = {},
        onSelectTab = {},
        onDismissEditDialog = {},
        onConfirmEditText = {}
    )
}
