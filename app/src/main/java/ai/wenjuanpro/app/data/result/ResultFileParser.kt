package ai.wenjuanpro.app.data.result

import timber.log.Timber

/**
 * Pure function: parses a result-file's text content and extracts the set of
 * completed question IDs (the first pipe-delimited field of each data line
 * after the `---` header separator).
 *
 * Returns `null` if the file is corrupt (no `---` separator found).
 */
object ResultFileParser {

    private const val SEPARATOR = "---"
    private const val EXPECTED_FIELD_COUNT = 9

    /**
     * A question counts as "completed for the session" once a terminal
     * result line has been written for it — i.e. the participant either
     * submitted an answer (`done`), let the timer run out (`not_answered`),
     * or hit the partial path. `error` and unknown statuses do not count,
     * so a write retry can still resume the same qid.
     */
    private val TERMINAL_STATUSES = setOf("done", "not_answered", "partial")

    fun parseCompletedQids(fileContent: String): Set<String>? =
        parseQidsByStatus(fileContent)?.let { statusMap ->
            statusMap.filter { it.value in TERMINAL_STATUSES }.keys
        }

    /**
     * Parses result file and returns a map of qid → last status.
     * For duplicate qids, the last line's status wins (append-based override).
     * Returns null if file is corrupt (no separator).
     */
    fun parseQidsByStatus(fileContent: String): Map<String, String>? {
        val lines = fileContent.lines()
        val separatorIndex = lines.indexOfFirst { it.trim() == SEPARATOR }
        if (separatorIndex < 0) return null

        val qidStatus = mutableMapOf<String, String>()
        for (i in (separatorIndex + 1) until lines.size) {
            val line = lines[i]
            if (line.isBlank()) continue
            val fields = line.split('|')
            if (fields.size < EXPECTED_FIELD_COUNT) {
                Timber.w("Skipping malformed data line %d: field count %d", i + 1, fields.size)
                continue
            }
            val qid = fields[0]
            val status = fields[EXPECTED_FIELD_COUNT - 1] // last field = status
            if (qid.isNotBlank()) {
                qidStatus[qid] = status
            }
        }
        return qidStatus
    }
}
