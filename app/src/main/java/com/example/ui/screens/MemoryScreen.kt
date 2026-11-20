package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.MemoryEntity
import com.example.model.AppLanguage
import com.example.ui.SathiUiState

@Composable
fun MemoryScreen(
    uiState: SathiUiState,
    onAddMemory: (category: String, title: String, content: String) -> Unit,
    onUpdateMemory: (MemoryEntity) -> Unit,
    onTogglePin: (MemoryEntity) -> Unit,
    onDeleteMemory: (Long) -> Unit,
    onDeleteAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBn = uiState.settings.language == AppLanguage.BENGALI
    var searchQuery by remember { mutableStateOf("") }
    var editingMemory by remember { mutableStateOf<MemoryEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val filtered = remember(uiState.memories, searchQuery) {
        if (searchQuery.isBlank()) uiState.memories
        else uiState.memories.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.content.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    if (showAddDialog) {
        MemoryEditorDialog(
            initial = null,
            isBn = isBn,
            onSave = { cat, title, content ->
                onAddMemory(cat, title, content)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    editingMemory?.let { target ->
        MemoryEditorDialog(
            initial = target,
            isBn = isBn,
            onSave = { cat, title, content ->
                onUpdateMemory(target.copy(category = cat, title = title, content = content))
                editingMemory = null
            },
            onDismiss = { editingMemory = null }
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
            title = {
                Text(if (isBn) "সব ডেটা মুছে ফেলবেন?" else "Delete All Your Data?")
            },
            text = {
                Text(
                    if (isBn) {
                        "এটি আপনার সব সংরক্ষিত স্মৃতি, নাম এবং কথোপকথনের ইতিহাস স্থায়ীভাবে মুছে ফেলবে।"
                    } else {
                        "This permanently deletes all saved memories, preferences, and conversation history on this device."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAllData()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isBn) "হ্যাঁ, সব মুছুন" else "Yes, Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isBn) "সাথীর স্মৃতি" else "Sathi Memory",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    FilledTonalButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_memory_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isBn) "নতুন যোগ" else "Add Memory")
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text(if (isBn) "স্মৃতি খুঁজুন..." else "Search memories...") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (filtered.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isBn) {
                                    "এখনও কোনো স্মৃতি সংরক্ষিত নেই। আপনার নাম, পছন্দ বা নোট যোগ করুন।"
                                } else {
                                    "No memories saved yet. Add your name, preferences, or notes."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(filtered, key = { it.id }) { memory ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssistChip(
                                onClick = {},
                                label = { Text(memory.category) }
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { onTogglePin(memory) }) {
                                Icon(
                                    imageVector = if (memory.isPinned) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = if (isBn) "পিন" else "Pin",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { editingMemory = memory }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = if (isBn) "সম্পাদনা" else "Edit"
                                )
                            }
                            IconButton(onClick = { onDeleteMemory(memory.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = if (isBn) "মুছুন" else "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Text(
                            text = memory.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = memory.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showDeleteAllDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("delete_all_data_button")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isBn) "আমার সব ডেটা মুছুন" else "Delete All My Data")
                }
            }
        }
    }
}

@Composable
private fun MemoryEditorDialog(
    initial: MemoryEntity?,
    isBn: Boolean,
    onSave: (category: String, title: String, content: String) -> Unit,
    onDismiss: () -> Unit
) {
    var category by remember {
        mutableStateOf(initial?.category ?: (if (isBn) "পছন্দ" else "Preference"))
    }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var content by remember { mutableStateOf(initial?.content ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) {
                    if (isBn) "নতুন স্মৃতি যোগ করুন" else "Add Memory"
                } else {
                    if (isBn) "স্মৃতি সম্পাদনা" else "Edit Memory"
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(if (isBn) "ধরন (যেমন পছন্দ, পরিচয়)" else "Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isBn) "শিরোনাম" else "Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(if (isBn) "বিস্তারিত" else "Details") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && content.isNotBlank(),
                onClick = { onSave(category, title, content) }
            ) {
                Text(if (isBn) "সংরক্ষণ" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isBn) "বাতিল" else "Cancel")
            }
        }
    )
}
