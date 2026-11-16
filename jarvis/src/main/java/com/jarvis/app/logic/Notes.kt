package com.jarvis.app.logic

import com.jarvis.app.data.Note

/**
 * Categorias de las notas: el metodo PARA de Tiago Forte ("Building a Second
 * Brain"), mas una bandeja de entrada para capturar rapido y organizar despues.
 * Se guarda el nombre ("INBOX") en la base de datos.
 */
enum class NoteCategory(val label: String, val hint: String) {
    INBOX("Inbox", "Captured quickly. Sort it later."),
    PROJECT("Projects", "Short-term efforts with a goal and a deadline."),
    AREA("Areas", "Ongoing responsibilities: health, studies, finances."),
    RESOURCE("Resources", "Topics and references you may want later."),
    ARCHIVE("Archive", "Inactive items from the other categories.");

    companion object {
        fun of(name: String): NoteCategory = entries.firstOrNull { it.name == name } ?: INBOX
    }
}

object NoteLists {

    /**
     * Notas de una categoria (o de todas si es null) que contienen [query].
     * Fijadas primero; despues, las editadas mas recientemente.
     */
    fun filter(notes: List<Note>, category: NoteCategory?, query: String): List<Note> {
        val q = query.trim()
        return notes
            .filter { category == null || it.category == category.name }
            .filter { note ->
                q.isEmpty() || listOf(note.title, note.body, note.takeaway)
                    .any { it.contains(q, ignoreCase = true) }
            }
            .sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAt })
    }

    /** Titulo a mostrar: el titulo, o la primera linea del texto si no tiene. */
    fun displayTitle(note: Note): String =
        note.title.ifBlank { note.body.lineSequence().firstOrNull { it.isNotBlank() }?.trim() ?: "Untitled" }
}
