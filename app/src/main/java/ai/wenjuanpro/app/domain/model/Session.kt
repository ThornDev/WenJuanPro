package ai.wenjuanpro.app.domain.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Session(
    val studentId: String,
    val deviceId: String,
    val config: Config,
    val sessionStart: LocalDateTime,
    val resultFileName: String,
    val cursor: Int = 0,
    val completedQids: Set<String> = emptySet(),
) {
    companion object {
        private val FILE_NAME_TIMESTAMP: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

        fun composeFileName(
            deviceId: String,
            studentId: String,
            configId: String,
            sessionStart: LocalDateTime,
        ): String = "${deviceId}_${studentId}_${configId}_${sessionStart.format(FILE_NAME_TIMESTAMP)}.txt"
    }
}
