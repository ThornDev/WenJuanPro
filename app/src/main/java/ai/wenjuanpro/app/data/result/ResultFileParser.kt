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

    fun parseCompletedQids(fileContent: String): Set<String>? =
        parseQidsByStatus(fileContent)?.let { statusMap ->
            statusMap.filter { it.value == "done" }.keys
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
