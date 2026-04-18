package ai.wenjuanpro.app.data.result

import ai.wenjuanpro.app.domain.model.ResultRecord

/**
 * Pure function: serialises a [ResultRecord] into a pipe-delimited line string.
 *
 * Format (9 fields):
 * `qid|type|mode|stemMs|optionsMs|answer|correct|score|status`
 *
 * - `type` / `mode` / `status` are written as lower-case enum names.
 * - `stemMs` is written as `"-"` when null (all_in_one mode).
 */
object ResultRecordSerializer {

    fun serialize(record: ResultRecord): String = buildString {
        append(record.qid)
        append('|')
        append(record.type.name.lowercase())
        append('|')
        append(record.mode.name.lowercase())
        append('|')
        append(record.stemMs?.toString() ?: "-")
        append('|')
        append(record.optionsMs)
        append('|')
        append(record.answer)
        append('|')
        append(record.correct)
        append('|')
        append(record.score)
        append('|')
        append(record.status.name.lowercase())
    }
}
