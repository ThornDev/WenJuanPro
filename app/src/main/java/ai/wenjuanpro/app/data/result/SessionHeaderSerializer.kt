package ai.wenjuanpro.app.data.result

import ai.wenjuanpro.app.domain.model.Session
import java.time.format.DateTimeFormatter

/**
 * Pure function: serialises session metadata into the header block written
 * at the top of every result file.
 *
 * Output format (5 key:value lines + separator):
 * ```
 * deviceId: {val}
 * studentId: {val}
 * configId: {val}
 * sessionStart: yyyyMMdd-HHmmss
 * appVersion: {val}
 * ---
 * ```
 */
object SessionHeaderSerializer {

    private val TIMESTAMP_FMT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

    fun serialize(session: Session, appVersion: String): String = buildString {
        append("deviceId: ").append(session.deviceId).append('\n')
        append("studentId: ").append(session.studentId).append('\n')
        append("configId: ").append(session.config.configId).append('\n')
        append("sessionStart: ").append(session.sessionStart.format(TIMESTAMP_FMT)).append('\n')
        append("appVersion: ").append(appVersion).append('\n')
        append("---").append('\n')
    }
}
