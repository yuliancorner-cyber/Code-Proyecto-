package com.jarvis.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.app.data.AppData
import com.jarvis.app.data.AppGraph
import com.jarvis.app.data.Habit
import com.jarvis.app.logic.CheckIndex
import com.jarvis.app.logic.Days
import com.jarvis.app.logic.Formats
import com.jarvis.app.logic.HabitStats
import com.jarvis.app.logic.Intention
import com.jarvis.app.ui.components.CheckCircle
import com.jarvis.app.ui.components.EmptyState
import com.jarvis.app.ui.components.JarvisCard
import com.jarvis.app.ui.components.Meter
import com.jarvis.app.ui.components.SectionLabel
import com.jarvis.app.ui.components.is24Hour
import com.jarvis.app.ui.components.launchAction
import com.jarvis.app.ui.theme.Cyan
import com.jarvis.app.ui.theme.CyanData
import com.jarvis.app.ui.theme.Hairline
import com.jarvis.app.ui.theme.HairlineStrong
import com.jarvis.app.ui.theme.TextMuted
import com.jarvis.app.ui.theme.TextPrimary
import com.jarvis.app.ui.theme.TextSecondary
import com.jarvis.app.ui.theme.Warning
import java.time.LocalDate
import java.time.YearMonth

/**
 * Habitos del mes: la misma cuadricula de tu hoja de calculo (habitos en
 * filas, dias en columnas) con sus totales, y debajo el detalle de cada uno.
 */
@Composable
fun HabitsScreen(
    data: AppData,
    today: LocalDate,
    onOpenHabit: (Long) -> Unit,
    onNewHabit: () -> Unit
) {
    // 0 = mes actual, -1 = mes pasado...
    var monthOffset by rememberSaveable { mutableStateOf(0) }
    val month = YearMonth.from(today).plusMonths(monthOffset.toLong())
    val habits = remember(data.habits) { data.habits.filter { !it.archived } }
    val index = remember(data.habitChecks) { CheckIndex(data.habitChecks) }
    val is24 = is24Hour()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "header") {
            Column(Modifier.padding(top = 8.dp)) {
                Text("Habits", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
                Text(
                    "Never miss twice.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        if (habits.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Rounded.GridView,
                    title = "No habits yet",
                    text = "Add the first habit you want to track every day."
                )
            }
        } else {
            item(key = "month") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { monthOffset-- }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month")
                    }
                    Text(
                        Formats.monthYear(month.atDay(1)),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { monthOffset++ }, enabled = monthOffset < 0) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next month")
                    }
                }
            }

            item(key = "grid") {
                MonthGrid(
                    habits = habits,
                    index = index,
                    month = month,
                    today = today,
                    onToggle = { habit, date -> launchAction { AppGraph.repo.toggleHabit(habit, date) } },
                    onOpenHabit = onOpenHabit
                )
            }

            item(key = "summary-label") { SectionLabel("This month") }
            items(habits, key = { "habit-${it.id}" }) { habit ->
                val doneDays = index.doneDays(habit)
                val summary = HabitStats.month(habit.days, doneDays, month, today)
                JarvisCard(onClick = { onOpenHabit(habit.id) }, padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            habitTitle(habit),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${summary.done}/${summary.expected} days · ${summary.percent}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Meter(
                        fraction = summary.percent / 100f,
                        height = 6.dp,
                        description = "${habit.name} ${summary.percent} percent this month"
                    )
                    Spacer(Modifier.height(8.dp))
                    val streak = HabitStats.currentStreak(habit.days, doneDays, today)
                    val best = HabitStats.bestStreak(habit.days, doneDays, today)
                    Text(
                        "Streak $streak · Best $best",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    val plan = Intention.forHabit(habit, is24)
                    Text(
                        plan.ifEmpty { "No plan yet. Tap to say when and where." },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (plan.isEmpty()) TextMuted else TextSecondary
                    )
                }
            }
        }

        item(key = "add") {
            OutlinedButton(onClick = onNewHabit, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("New habit")
            }
        }
    }
}

/** "📖 Reading" */
internal fun habitTitle(habit: Habit): String =
    listOf(habit.emoji.trim(), habit.name.trim()).filter { it.isNotEmpty() }.joinToString(" ")

private val CELL = 32.dp
private val HEADER = 40.dp
private val NAME_WIDTH = 134.dp

/**
 * La cuadricula del mes. La columna de nombres queda fija y los dias se
 * deslizan en horizontal. Tocar una casilla la marca o la desmarca.
 */
@Composable
private fun MonthGrid(
    habits: List<Habit>,
    index: CheckIndex,
    month: YearMonth,
    today: LocalDate,
    onToggle: (Habit, Long) -> Unit,
    onOpenHabit: (Long) -> Unit
) {
    val days = remember(month) { (1..month.lengthOfMonth()).map { month.atDay(it) } }
    val scroll = rememberScrollState()
    val density = LocalDensity.current

    // Al abrir el mes actual, desliza hasta dejar el dia de hoy a la vista.
    LaunchedEffect(month) {
        val target = if (YearMonth.from(today) == month) {
            with(density) { (CELL * (today.dayOfMonth - 5).coerceAtLeast(0)).roundToPx() }
        } else 0
        scroll.scrollTo(target)
    }

    JarvisCard(padding = 10.dp) {
        Row {
            // --- Columna fija: nombres de los habitos ---
            Column(Modifier.width(NAME_WIDTH)) {
                Box(Modifier.height(HEADER), contentAlignment = Alignment.CenterStart) {
                    Text("HABIT", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                }
                for (habit in habits) {
                    Box(
                        modifier = Modifier
                            .height(CELL)
                            .fillMaxWidth()
                            .clickable { onOpenHabit(habit.id) },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            habitTitle(habit),
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
                Box(Modifier.height(CELL), contentAlignment = Alignment.CenterStart) {
                    Text("% DAY", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                }
            }

            // --- Dias del mes, deslizables ---
            Row(
                Modifier
                    .weight(1f)
                    .horizontalScroll(scroll)
            ) {
                for (date in days) {
                    val day = date.toEpochDay()
                    val isToday = date == today
                    val future = date.isAfter(today)
                    Column(Modifier.width(CELL), horizontalAlignment = Alignment.CenterHorizontally) {
                        Column(
                            Modifier.height(HEADER),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                Days.initial(date.dayOfWeek),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isToday) Cyan else TextMuted
                            )
                            Text(
                                date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) Cyan else TextSecondary
                            )
                        }
                        for (habit in habits) {
                            GridCell(
                                habit = habit,
                                date = date,
                                count = index.count(habit.id, day),
                                future = future,
                                onToggle = { onToggle(habit, day) }
                            )
                        }
                        Box(Modifier.height(CELL), contentAlignment = Alignment.Center) {
                            if (!future) {
                                val percent = HabitStats.dayPercent(
                                    habits.map { it.days to index.isDone(it, day) }, date
                                )
                                Text(
                                    percent.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isToday) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Una casilla de la cuadricula:
 *  - rellena  = hecho
 *  - borde    = tocaba y no se hizo (aun)
 *  - numero   = contador a medias (3 de 8 vasos)
 *  - punto    = ese dia no tocaba
 *  - tenue    = dia futuro (no se puede marcar)
 */
@Composable
private fun GridCell(habit: Habit, date: LocalDate, count: Int, future: Boolean, onToggle: () -> Unit) {
    val done = HabitStats.isDone(count, habit.target)
    val scheduled = HabitStats.isScheduled(habit.days, date)
    val shape = RoundedCornerShape(6.dp)
    val state = when {
        done -> "done"
        future -> "upcoming"
        count > 0 -> "$count of ${habit.target}"
        else -> "not done"
    }
    Box(
        modifier = Modifier
            .size(CELL)
            .clip(shape)
            .then(if (future) Modifier else Modifier.clickable(role = Role.Checkbox, onClick = onToggle))
            .semantics { contentDescription = "${habit.name}, day ${date.dayOfMonth}, $state" },
        contentAlignment = Alignment.Center
    ) {
        when {
            done -> Box(
                Modifier
                    .size(22.dp)
                    .background(CyanData, shape)
            )
            future -> Box(
                Modifier
                    .size(22.dp)
                    .border(1.dp, Hairline, shape)
            )
            count > 0 -> Box(
                Modifier
                    .size(22.dp)
                    .border(1.5.dp, CyanData, shape),
                contentAlignment = Alignment.Center
            ) {
                Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = TextPrimary)
            }
            scheduled -> Box(
                Modifier
                    .size(22.dp)
                    .border(1.5.dp, HairlineStrong, shape)
            )
            else -> Box(
                Modifier
                    .size(4.dp)
                    .background(TextMuted, CircleShape)
            )
        }
    }
}

/**
 * Un habito en la lista de HOY (pantalla de inicio): casilla, nombre, su plan
 * y, si es un contador (agua), un boton "+1".
 */
@Composable
internal fun HabitRow(
    habit: Habit,
    count: Int,
    streak: Int,
    missedLastTime: Boolean,
    onToggle: () -> Unit,
    onIncrement: () -> Unit,
    onClick: () -> Unit
) {
    val done = HabitStats.isDone(count, habit.target)
    val plan = Intention.forHabit(habit, is24Hour())

    JarvisCard(onClick = onClick, padding = 6.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CheckCircle(checked = done, onCheckedChange = { onToggle() }, label = habit.name)
            Column(
                Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    habitTitle(habit),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (done) TextMuted else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                when {
                    done -> Unit
                    // "Never miss twice": fallar una vez pasa; dos seguidas, no.
                    missedLastTime -> Text(
                        "Missed last time. Never miss twice.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Warning
                    )
                    plan.isNotEmpty() -> Text(
                        plan,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    habit.tinyVersion.isNotBlank() -> Text(
                        "Two-minute version: ${habit.tinyVersion.trim()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (habit.target > 1) {
                Text(
                    "$count/${habit.target}",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                IconButton(onClick = onIncrement, enabled = !done) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Add one ${habit.unit.ifBlank { "time" }} to ${habit.name}",
                        tint = if (done) TextMuted else Cyan
                    )
                }
            } else if (streak >= 2) {
                Text(
                    "$streak-day streak",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            } else {
                Spacer(Modifier.width(12.dp))
            }
        }
    }
}
