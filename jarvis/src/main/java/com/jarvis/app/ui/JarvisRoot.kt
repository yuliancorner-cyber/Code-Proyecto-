package com.jarvis.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.app.data.AppGraph
import com.jarvis.app.logic.NoteCategory
import com.jarvis.app.ui.theme.Black
import com.jarvis.app.ui.theme.Cyan
import com.jarvis.app.ui.theme.CyanContainer
import com.jarvis.app.ui.theme.SurfaceCard
import com.jarvis.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.time.LocalDate

/** Las pestanas de la barra inferior. */
enum class Tab(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Rounded.WbSunny),
    TASKS("Tasks", Icons.Rounded.TaskAlt),
    HABITS("Habits", Icons.Rounded.GridView),
    BRAIN("Brain", Icons.Rounded.Psychology),
    SETTINGS("Settings", Icons.Rounded.Settings)
}

/** Pantallas que se abren ENCIMA de las pestanas (editores). */
private sealed interface Overlay {
    data class TaskEdit(val id: Long?, val presetToday: Boolean) : Overlay
    data class HabitEdit(val id: Long?) : Overlay
    data class NoteEdit(val id: Long?, val category: NoteCategory) : Overlay
    data object Routine : Overlay
}

/**
 * Raiz de la interfaz: decide que se ve.
 *  1. Primera vez -> bienvenida.
 *  2. Un editor abierto -> el editor a pantalla completa.
 *  3. Si no -> la pestana elegida, con la barra inferior.
 */
@Composable
fun JarvisRoot() {
    val settings by AppGraph.settings.flow.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = Black) {
        if (!settings.setupDone) {
            SetupScreen(settings)
            return@Surface
        }

        // null mientras la base de datos se lee por primera vez (un instante).
        val data = AppGraph.data.collectAsStateWithLifecycle().value ?: return@Surface
        val today = rememberToday()

        var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
        var overlay by remember { mutableStateOf<Overlay?>(null) }
        val close = { overlay = null }

        when (val current = overlay) {
            is Overlay.TaskEdit -> TaskEditorScreen(
                taskId = current.id,
                presetToday = current.presetToday,
                today = today,
                projects = remember(data.tasks) {
                    data.tasks.map { it.project.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
                },
                onClose = close
            )
            is Overlay.HabitEdit -> HabitEditorScreen(habitId = current.id, onClose = close)
            is Overlay.NoteEdit -> NoteEditorScreen(
                noteId = current.id,
                initialCategory = current.category,
                onClose = close
            )
            Overlay.Routine -> RoutineEditorScreen(steps = data.routineSteps, onClose = close)
            null -> {
                // En cualquier pestana que no sea la primera, "atras" vuelve a Today.
                BackHandler(enabled = tab != Tab.TODAY) { tab = Tab.TODAY }
                Scaffold(
                    containerColor = Black,
                    bottomBar = {
                        NavigationBar(containerColor = SurfaceCard) {
                            for (item in Tab.entries) {
                                NavigationBarItem(
                                    selected = tab == item,
                                    onClick = { tab = item },
                                    icon = { Icon(item.icon, contentDescription = null) },
                                    label = { Text(item.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Cyan,
                                        selectedTextColor = Cyan,
                                        indicatorColor = CyanContainer,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                ) { padding ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        when (tab) {
                            Tab.TODAY -> TodayScreen(
                                data = data,
                                settings = settings,
                                today = today,
                                onOpenTask = { overlay = Overlay.TaskEdit(it, presetToday = false) },
                                onNewTask = { overlay = Overlay.TaskEdit(null, presetToday = true) },
                                onOpenHabit = { overlay = Overlay.HabitEdit(it) },
                                onEditRoutine = { overlay = Overlay.Routine },
                                onOpenSettings = { tab = Tab.SETTINGS }
                            )
                            Tab.TASKS -> TasksScreen(
                                data = data,
                                today = today,
                                onOpenTask = { overlay = Overlay.TaskEdit(it, presetToday = false) },
                                onNewTask = { overlay = Overlay.TaskEdit(null, presetToday = false) }
                            )
                            Tab.HABITS -> HabitsScreen(
                                data = data,
                                today = today,
                                onOpenHabit = { overlay = Overlay.HabitEdit(it) },
                                onNewHabit = { overlay = Overlay.HabitEdit(null) }
                            )
                            Tab.BRAIN -> BrainScreen(
                                data = data,
                                onOpenNote = { overlay = Overlay.NoteEdit(it, NoteCategory.INBOX) },
                                onNewNote = { overlay = Overlay.NoteEdit(null, it) }
                            )
                            Tab.SETTINGS -> SettingsScreen(
                                data = data,
                                settings = settings,
                                onEditRoutine = { overlay = Overlay.Routine }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** La fecha de hoy, que se actualiza sola al pasar la medianoche. */
@Composable
fun rememberToday(): LocalDate {
    val today by produceState(initialValue = LocalDate.now()) {
        while (true) {
            delay(20_000)
            value = LocalDate.now()
        }
    }
    return today
}
