package com.prostuti.student.domain.notification

/** Where a notification's `target_url` leads inside the app (GROUP_ROUTINE_PLAN.md §6.18). */
sealed interface NotificationTarget {
    data class Group(val groupId: String) : NotificationTarget
    data class JoinRequests(val groupId: String) : NotificationTarget
    data object Routine : NotificationTarget

    companion object {
        /**
         * Path patterns, most specific first; `{id}` is one path segment. New formats only need a row
         * here (the server's full list is still to come, §14 B9). Unknown or missing → null: the
         * notification is only marked read.
         */
        private val RULES: List<Pair<List<String>, (Map<String, String>) -> NotificationTarget>> = listOf(
            listOf("groups", "{id}", "join-requests", "*") to { p -> JoinRequests(p.getValue("id")) },
            listOf("groups", "{id}", "join-requests") to { p -> JoinRequests(p.getValue("id")) },
            listOf("groups", "{id}", "*") to { p -> Group(p.getValue("id")) },
            listOf("groups", "{id}") to { p -> Group(p.getValue("id")) },
            listOf("routines", "*") to { _ -> Routine },
            listOf("routines") to { _ -> Routine },
        )

        /** Accepts a bare path or a full URL, with or without `/api/v1`, query or fragment. */
        fun parse(targetUrl: String?): NotificationTarget? {
            if (targetUrl.isNullOrBlank()) return null
            val path = targetUrl.substringBefore('#').substringBefore('?')
                .let { if ("://" in it) it.substringAfter("://").substringAfter('/', "") else it }
            val segments = path.split('/').filter { it.isNotBlank() }.let {
                if (it.size >= 2 && it[0] == "api" && it[1].matches(Regex("v\\d+"))) it.drop(2) else it
            }
            for ((pattern, build) in RULES) {
                val params = match(pattern, segments) ?: continue
                return build(params)
            }
            return null
        }

        /** "*" at the end swallows the rest; `{id}` takes one segment. */
        private fun match(pattern: List<String>, segments: List<String>): Map<String, String>? {
            val params = mutableMapOf<String, String>()
            pattern.forEachIndexed { i, part ->
                if (part == "*") return if (segments.size > i) params else null
                val seg = segments.getOrNull(i) ?: return null
                when {
                    part.startsWith("{") -> params[part.trim('{', '}')] = seg
                    part != seg -> return null
                }
            }
            return if (segments.size == pattern.size) params else null
        }
    }
}
