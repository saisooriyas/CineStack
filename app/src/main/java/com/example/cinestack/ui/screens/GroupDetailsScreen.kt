package com.example.cinestack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cinestack.data.model.Movie
import com.example.cinestack.ui.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    groupId: Int,
    viewModel: SearchViewModel,
    onBackClick: () -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSubGroupClick: (Int) -> Unit
) {
    val library by viewModel.library.collectAsState()
    val allGroups by viewModel.allGroups.collectAsState()
    val group = allGroups.find { it.id == groupId }
    
    var groupItems by remember { mutableStateOf<List<com.example.cinestack.data.local.GroupItemEntity>>(emptyList()) }
    var subGroups by remember { mutableStateOf<List<com.example.cinestack.data.local.GroupEntity>>(emptyList()) }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showCreateSubGroupDialog by remember { mutableStateOf(false) }
    var showDeleteGroupDialog by remember { mutableStateOf(false) }
    var itemToRemove by remember { mutableStateOf<com.example.cinestack.data.local.GroupItemEntity?>(null) }
    var newSubGroupName by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()

    LaunchedEffect(groupId) {
        viewModel.getGroupItems(groupId).collect { items ->
            groupItems = items
        }
    }
    
    LaunchedEffect(groupId) {
        viewModel.getSubGroups(groupId).collect { groups ->
            subGroups = groups
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text(group?.name?.uppercase() ?: "GROUP", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ChevronLeft, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showDeleteGroupDialog = true }) {
                        Icon(Icons.Default.DeleteOutline, null, tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(24.dp)
        ) {
            if (subGroups.isNotEmpty()) {
                item {
                    SectionHeaderWithLine("SUB GROUPS", MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                items(subGroups) { sub ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSubGroupClick(sub.id) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1A1A1A)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(sub.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }

            val movies = groupItems.filter { it.type == "MOVIE" }
            if (movies.isNotEmpty()) {
                item {
                    SectionHeaderWithLine("MOVIES & TV", MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                items(movies.chunked(2)) { chunk ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        chunk.forEach { item ->
                            Box(modifier = Modifier.weight(1f)) {
                                LibraryMediaCard(
                                    title = item.title,
                                    subtitle = "Movie",
                                    imageUrl = item.imageUrl,
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        onMovieClick(Movie(id = item.externalId, title = item.title, posterUrl = item.imageUrl, backdropUrl = "", rating = 0.0, genre = emptyList(), duration = "", releaseYear = 0, synopsis = ""))
                                    }
                                )
                                IconButton(
                                    onClick = { itemToRemove = item },
                                    modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.5f), CircleShape).size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        if (chunk.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            val persons = groupItems.filter { it.type == "PERSON" }
            if (persons.isNotEmpty()) {
                item {
                    SectionHeaderWithLine("CAST & CREW", Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                items(persons) { person ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1A1A1A)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = person.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.size(50.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(person.title, color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { itemToRemove = person }) {
                                Icon(Icons.Default.Delete, null, tint = Color.Gray)
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { showCreateSubGroupDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
                ) {
                    Icon(Icons.Default.CreateNewFolder, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CREATE SUB GROUP")
                }
            }
        }
    }

    if (showDeleteGroupDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupDialog = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("Delete Group", color = Color.White) },
            text = { Text("Are you sure you want to delete this group? All items within it will be removed from the group.", color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGroup(groupId)
                    onBackClick()
                    showDeleteGroupDialog = false
                }) { Text("DELETE", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupDialog = false }) { Text("CANCEL", color = Color.White) }
            }
        )
    }

    if (itemToRemove != null) {
        AlertDialog(
            onDismissRequest = { itemToRemove = null },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("Remove Item", color = Color.White) },
            text = { Text("Remove '${itemToRemove?.title}' from this group?", color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = {
                    itemToRemove?.let { viewModel.removeGroupItem(it) }
                    itemToRemove = null
                }) { Text("REMOVE", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { itemToRemove = null }) { Text("CANCEL", color = Color.White) }
            }
        )
    }

    if (showCreateSubGroupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateSubGroupDialog = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("New Sub Group", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newSubGroupName,
                    onValueChange = { newSubGroupName = it },
                    placeholder = { Text("Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newSubGroupName.isNotBlank()) {
                        viewModel.createGroup(newSubGroupName, groupId)
                        newSubGroupName = ""
                        showCreateSubGroupDialog = false
                    }
                }) { Text("CREATE", color = MaterialTheme.colorScheme.primary) }
            }
        )
    }

    if (showAddDialog) {
        // Simple Add Dialog (In a real app, this would be a search or picker)
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("Add Item", color = Color.White) },
            text = { Text("Go to a Movie or Person's detail page to add them to this group.", color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("OK") }
            }
        )
    }
}
