package com.spoookify.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spoookify.ui.theme.SpotifyBlack
import com.spoookify.ui.theme.SpotifyGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeCustomizationScreen(
    onBackClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val currentModules by viewModel.homeModules.collectAsState()
    var tempModules by remember(currentModules) { 
        mutableStateOf(currentModules.filter { it != HomeModule.RecentlyPlayed && it != HomeModule.NewReleases }) 
    }

    val allPossibleModules = HomeModule.entries.filter { 
        it != HomeModule.RecentlyPlayed && it != HomeModule.NewReleases 
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customize Home", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.updateModuleOrder(tempModules)
                        onBackClick()
                    }) {
                        Text("SAVE", color = SpotifyGreen, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SpotifyBlack,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = SpotifyBlack
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text(
                text = "Choose and reorder the sections you want to see on your home screen.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(tempModules) { index, module ->
                    ModuleItem(
                        module = module,
                        onMoveUp = if (index > 0) { {
                            val newList = tempModules.toMutableList()
                            val item = newList.removeAt(index)
                            newList.add(index - 1, item)
                            tempModules = newList
                        } } else null,
                        onMoveDown = if (index < tempModules.size - 1) { {
                            val newList = tempModules.toMutableList()
                            val item = newList.removeAt(index)
                            newList.add(index + 1, item)
                            tempModules = newList
                        } } else null,
                        onRemove = {
                            tempModules = tempModules.filter { it != module }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("Add Sections", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }

                val availableToAdd = allPossibleModules.filter { it !in tempModules }
                itemsIndexed(availableToAdd) { _, module ->
                    ListItem(
                        headlineContent = { Text(module.label, color = Color.White) },
                        leadingContent = { 
                            IconButton(onClick = { tempModules = tempModules + module }) {
                                Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = Color.Gray)
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
fun ModuleItem(
    module: HomeModule,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SpotifyGreen)
            }
            Text(
                text = module.label,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Row {
                IconButton(onClick = onMoveUp ?: {}, enabled = onMoveUp != null) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = if (onMoveUp != null) Color.White else Color.DarkGray)
                }
                IconButton(onClick = onMoveDown ?: {}, enabled = onMoveDown != null) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = if (onMoveDown != null) Color.White else Color.DarkGray)
                }
            }
        }
    }
}
