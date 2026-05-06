package ai.wenjuanpro.app.data.parser

import ai.wenjuanpro.app.core.io.FileSystem
import ai.wenjuanpro.app.domain.model.Config
import ai.wenjuanpro.app.domain.model.ConfigWarning
import ai.wenjuanpro.app.domain.model.OptionContent
import ai.wenjuanpro.app.domain.model.PresentMode
import ai.wenjuanpro.app.domain.model.Question
import ai.wenjuanpro.app.domain.model.StemContent
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigParser
    @Inject
    constructor(
        private val fileSystem: FileSystem,
    ) {
        fun parse(sourceName: String, bytes: ByteArray): ParseResult {
            val warnings = mutableListOf<ConfigWarning>()
            val stripped = stripBom(bytes, warnings)

            val text =
                try {
                    decodeUtf8Strict(stripped)
                } catch (_: CharacterCodingException) {
                    return ParseResult.Failure(
                        listOf(
                            ParseError(
                                line = 0,
                                field = null,
                                code = ParseErrorCode.ENCODING_INVALID,
                                message = "$sourceName: 文件编码非 UTF-8，无法解析",
                            ),
                        ),
                    )
                }

            val lines = if (text.isEmpty()) emptyList() else text.split(LINE_SPLIT_REGEX)
            val errors = mutableListOf<ParseError>()

            val header = parseHeader(lines, sourceName, errors)
            val sections = splitSections(lines, header.nextLineIndex, sourceName, errors)

            val seenQids = linkedSetOf<String>()
            val questions = mutableListOf<Question>()
            for (section in sections) {
                if (!seenQids.add(section.qid)) {
                    errors.add(
                        ParseError(
                            section.headerLine,
                            "qid",
                            ParseErrorCode.CONFIG_FIELD_INVALID,
                            "$sourceName [${section.qid}] 第 ${section.headerLine} 行: 重复的题号",
                        ),
                    )
                    continue
                }
                parseQuestion(sourceName, section, errors)?.let { questions.add(it) }
            }

            if (sections.isEmpty() && questions.isEmpty()) {
                errors.add(
                    ParseError(
                        line = 0,
                        field = null,
                        code = ParseErrorCode.CONFIG_FIELD_INVALID,
                        message = "$sourceName: config 至少包含一道题",
                    ),
                )
            }

            val configId = header.configId
            val title = header.title
            return if (errors.isEmpty() && configId != null && title != null) {
                ParseResult.Success(
                    Config(
                        configId = configId,
                        title = title,
                        sourceFileName = sourceName,
                        questions = questions.toList(),
                        parseWarnings = warnings.toList(),
                    ),
                )
            } else {
                ParseResult.Failure(errors.toList())
            }
        }

        // ---------------------------------------------------------------------
        // Encoding / BOM
        // ---------------------------------------------------------------------

        private fun stripBom(bytes: ByteArray, warnings: MutableList<ConfigWarning>): ByteArray {
            if (bytes.size >= 3 &&
                bytes[0] == 0xEF.toByte() &&
                bytes[1] == 0xBB.toByte() &&
                bytes[2] == 0xBF.toByte()
            ) {
                warnings.add(ConfigWarning(1, "BOM 已自动跳过"))
                return bytes.copyOfRange(3, bytes.size)
            }
            return bytes
        }

        private fun decodeUtf8Strict(bytes: ByteArray): String {
            val decoder =
                StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
            return decoder.decode(ByteBuffer.wrap(bytes)).toString()
        }

        // ---------------------------------------------------------------------
        // Header
        // ---------------------------------------------------------------------

        private data class Header(
            val configId: String?,
            val title: String?,
            val nextLineIndex: Int,
        )

        private fun parseHeader(
            lines: List<String>,
            sourceName: String,
            errors: MutableList<ParseError>,
        ): Header {
            var configIdRaw: String? = null
            var titleRaw: String? = null
            var configIdLine = -1
            var titleLine = -1
            var idx = 0
            while (idx < lines.size) {
                val raw = lines[idx]
                val trimmed = raw.trim()
                if (SECTION_REGEX.matches(trimmed)) break
                if (SECTION_LIKE_REGEX.matches(trimmed)) break // invalid section header; let splitSections flag it
                if (trimmed.isEmpty()) {
                    idx++
                    continue
                }
                if (trimmed.startsWith("#")) {
                    val match = HEADER_ENTRY_REGEX.find(trimmed)
                    if (match != null) {
                        val key = match.groupValues[1].trim()
                        val value = match.groupValues[2].trim()
                        when (key) {
                            "configId" -> {
                                configIdRaw = value
                                configIdLine = idx + 1
                            }
                            "title" -> {
                                titleRaw = value
                                titleLine = idx + 1
                            }
                        }
                    }
                    idx++
                    continue
                }
                // Non-comment, non-section content: stop header region.
                break
            }

            val titlePresent = !titleRaw.isNullOrBlank()
            val configIdPresent = !configIdRaw.isNullOrBlank()

            if (!titlePresent) {
                val line = if (titleLine > 0) titleLine else 1
                errors.add(
                    ParseError(
                        line = line,
                        field = "title",
                        code = ParseErrorCode.CONFIG_HEADER_MISSING,
                        message = "$sourceName: 第 $line 行缺少必填字段 title",
                    ),
                )
            }
            if (!configIdPresent && !titlePresent) {
                val line = if (configIdLine > 0) configIdLine else 1
                errors.add(
                    ParseError(
                        line = line,
                        field = "configId",
                        code = ParseErrorCode.CONFIG_HEADER_MISSING,
                        message = "$sourceName: 第 $line 行缺少必填字段 configId",
                    ),
                )
            }

            var resolvedConfigId: String? =
                when {
                    configIdPresent -> configIdRaw
                    titlePresent -> sourceName.removeSuffix(".txt")
                    else -> null
                }
            if (resolvedConfigId != null && !CONFIG_ID_REGEX.matches(resolvedConfigId)) {
                val line = if (configIdLine > 0) configIdLine else 1
                errors.add(
                    ParseError(
                        line = line,
                        field = "configId",
                        code = ParseErrorCode.CONFIG_FIELD_INVALID,
                        message = "$sourceName 第 $line 行: configId 格式非法：只允许字母、数字、-、_",
                    ),
                )
                resolvedConfigId = null
            }

            return Header(
                configId = resolvedConfigId,
                title = if (titlePresent) titleRaw else null,
                nextLineIndex = idx,
            )
        }

        // ---------------------------------------------------------------------
        // Sections
        // ---------------------------------------------------------------------

        private data class FieldEntry(val value: String, val line: Int)

        private data class Section(
            val qid: String,
            val headerLine: Int,
            val fields: Map<String, FieldEntry>,
        )

        private fun splitSections(
            lines: List<String>,
            startIdx: Int,
            sourceName: String,
            errors: MutableList<ParseError>,
        ): List<Section> {
            val result = mutableListOf<Section>()
            var i = startIdx
            var currentQid: String? = null
            var currentHeaderLine = -1
            var currentFields: MutableMap<String, FieldEntry>? = null

            fun flush() {
                val qid = currentQid
                val fields = currentFields
                if (qid != null && fields != null) {
                    result.add(Section(qid, currentHeaderLine, fields.toMap()))
                }
                currentQid = null
                currentHeaderLine = -1
                currentFields = null
            }

            while (i < lines.size) {
                val lineNo = i + 1
                val trimmed = lines[i].trim()

                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    i++
                    continue
                }

                if (SECTION_REGEX.matches(trimmed)) {
                    flush()
                    currentQid = trimmed.substring(1, trimmed.length - 1)
                    currentHeaderLine = lineNo
                    currentFields = linkedMapOf()
                    i++
                    continue
                }

                if (SECTION_LIKE_REGEX.matches(trimmed)) {
                    flush()
                    errors.add(
                        ParseError(
                            line = lineNo,
                            field = "qid",
                            code = ParseErrorCode.CONFIG_FIELD_INVALID,
                            message = "$sourceName 第 $lineNo 行: 题号格式非法：$trimmed",
                        ),
                    )
                    i++
                    continue
                }

                val sep = trimmed.indexOf(':')
                if (sep > 0 && currentFields != null) {
                    val key = trimmed.substring(0, sep).trim()
                    val value = trimmed.substring(sep + 1).trim()
                    currentFields!![key] = FieldEntry(value, lineNo)
                }
                i++
            }
            flush()
            return result
        }

        // ---------------------------------------------------------------------
        // Question parsing
        // ---------------------------------------------------------------------

        private fun parseQuestion(
            sourceName: String,
            section: Section,
            errors: MutableList<ParseError>,
        ): Question? {
            val typeField = section.fields["type"]
            if (typeField == null) {
                errors.add(
                    missingField(sourceName, section, "type"),
                )
                return null
            }
            val type = typeField.value
            if (type !in TYPE_VALUES) {
                errors.add(
                    ParseError(
                        line = typeField.line,
                        field = "type",
                        code = ParseErrorCode.CONFIG_FIELD_INVALID,
                        message =
                            "$sourceName [${section.qid}] 第 ${typeField.line} 行: 未知题型：$type（合法值: single/multi/memory）",
                    ),
                )
                return null
            }

            val modeField = section.fields["mode"]
            if (modeField == null) {
                errors.add(missingField(sourceName, section, "mode"))
                return null
            }
            val modeValue = modeField.value
            if (modeValue !in MODE_VALUES) {
                errors.add(
                    ParseError(
                        line = modeField.line,
                        field = "mode",
                        code = ParseErrorCode.CONFIG_FIELD_INVALID,
                        message =
                            "$sourceName [${section.qid}] 第 ${modeField.line} 行: 未知呈现模式：$modeValue（合法值: all_in_one/staged）",
                    ),
                )
                return null
            }
            val mode = if (modeValue == "all_in_one") PresentMode.ALL_IN_ONE else PresentMode.STAGED

            val durations = parseDurations(sourceName, section, mode, errors) ?: return null

            return when (type) {
                "single" -> parseSingleChoice(sourceName, section, mode, durations, errors)
                "multi" -> parseMultiChoice(sourceName, section, mode, durations, errors)
                "memory" -> parseMemoryQuestion(section, mode, durations, errors, sourceName)
                else -> null
            }
        }

        private data class Durations(val stemDurationMs: Long?, val optionsDurationMs: Long)

        private fun parseDurations(
            sourceName: String,
            section: Section,
            mode: PresentMode,
            errors: MutableList<ParseError>,
        ): Durations? {
            return when (mode) {
                PresentMode.ALL_IN_ONE -> {
                    val f = section.fields["durationMs"]
                    if (f == null) {
                        errors.add(missingField(sourceName, section, "durationMs"))
                        return null
                    }
                    val ms = parseDurationValue(sourceName, section.qid, "durationMs", f, errors) ?: return null
                    Durations(stemDurationMs = null, optionsDurationMs = ms)
                }
                PresentMode.STAGED -> {
                    val stem = section.fields["stemDurationMs"]
                    val options = section.fields["optionsDurationMs"]
                    var ok = true
                    var stemMs: Long? = null
                    var optionsMs: Long? = null
                    if (stem == null) {
                        errors.add(missingField(sourceName, section, "stemDurationMs"))
                        ok = false
                    } else {
                        stemMs = parseDurationValue(sourceName, section.qid, "stemDurationMs", stem, errors)
                        if (stemMs == null) ok = false
                    }
                    if (options == null) {
                        errors.add(missingField(sourceName, section, "optionsDurationMs"))
                        ok = false
                    } else {
                        optionsMs = parseDurationValue(sourceName, section.qid, "optionsDurationMs", options, errors)
                        if (optionsMs == null) ok = false
                    }
                    if (!ok || stemMs == null || optionsMs == null) return null
                    Durations(stemDurationMs = stemMs, optionsDurationMs = optionsMs)
                }
            }
        }

        private fun parseDurationValue(
            sourceName: String,
            qid: String,
            fieldName: String,
            entry: FieldEntry,
            errors: MutableList<ParseError>,
        ): Long? {
            val value = entry.value.toLongOrNull()
            if (value == null || value < DURATION_MIN_MS || value > DURATION_MAX_MS) {
                errors.add(
                    ParseError(
                        line = entry.line,
                        field = fieldName,
                        code = ParseErrorCode.CONFIG_FIELD_INVALID,
                        message =
                            "$sourceName [$qid] 第 ${entry.line} 行: 倒计时时长非法：必须为 1000-600000 毫秒之间的正整数",
                    ),
                )
                return null
            }
            return value
        }

        private fun parseSingleChoice(
            sourceName: String,
            section: Section,
            mode: PresentMode,
            durations: Durations,
            errors: MutableList<ParseError>,
        ): Question? {
            val stem = parseStem(sourceName, section, errors) ?: return null
            val options = parseOptions(sourceName, section, errors) ?: return null
            val correctField = section.fields["correct"]
            if (correctField == null) {
                errors.add(missingField(sourceName, section, "correct"))
                return null
            }
            val correctIndex = correctField.value.toIntOrNull()
            if (correctIndex == null || correctIndex < 1 || correctIndex > options.size) {
                errors.add(
                    invalidField(
                        sourceName,
                        section.qid,
                        correctField.line,
                        "correct",
                        "正确答案非法：必须为 1..${options.size} 的整数",
                    ),
                )
                return null
            }
            val scores = parseScores(sourceName, section, options.size, errors) ?: return null
            val showSubmit = parseShowSubmit(section)
            val autoAdvance = parseAutoAdvance(section)
            val optionsPerRow = parseOptionsPerRow(sourceName, section, errors)
            return Question.SingleChoice(
                qid = section.qid,
                mode = mode,
                stemDurationMs = durations.stemDurationMs,
                optionsDurationMs = durations.optionsDurationMs,
                stem = stem,
                options = options,
                correctIndex = correctIndex,
                scores = scores,
                showSubmitButton = showSubmit,
                autoAdvance = autoAdvance,
                optionsPerRow = optionsPerRow,
            )
        }

        private fun parseMultiChoice(
            sourceName: String,
            section: Section,
            mode: PresentMode,
            durations: Durations,
            errors: MutableList<ParseError>,
        ): Question? {
            val stem = parseStem(sourceName, section, errors) ?: return null
            val options = parseOptions(sourceName, section, errors) ?: return null
            val correctField = section.fields["correct"]
            if (correctField == null) {
                errors.add(missingField(sourceName, section, "correct"))
                return null
            }
            val parts = correctField.value.split(",").map { it.trim() }
            val parsed = parts.map { it.toIntOrNull() }
            if (parts.isEmpty() || parsed.any { it == null }) {
                errors.add(
                    invalidField(
                        sourceName, section.qid, correctField.line, "correct",
                        "多选正确答案非法：必须为逗号分隔的正整数",
                    ),
                )
                return null
            }
            val ints = parsed.map { it!! }
            if (ints.any { it < 1 || it > options.size }) {
                errors.add(
                    invalidField(
                        sourceName, section.qid, correctField.line, "correct",
                        "多选正确答案非法：索引越界（合法范围 1..${options.size}）",
                    ),
                )
                return null
            }
            for (k in 1 until ints.size) {
                if (ints[k] <= ints[k - 1]) {
                    errors.add(
                        invalidField(
                            sourceName, section.qid, correctField.line, "correct",
                            "多选正确答案非法：必须严格升序且不重复",
                        ),
                    )
                    return null
                }
            }
            val scores = parseScores(sourceName, section, options.size, errors) ?: return null
            val showSubmit = parseShowSubmit(section)
            val optionsPerRow = parseOptionsPerRow(sourceName, section, errors)
            return Question.MultiChoice(
                qid = section.qid,
                mode = mode,
                stemDurationMs = durations.stemDurationMs,
                optionsDurationMs = durations.optionsDurationMs,
                stem = stem,
                options = options,
                correctIndices = ints.toSet(),
                scores = scores,
                showSubmitButton = showSubmit,
                optionsPerRow = optionsPerRow,
            )
        }

        private fun parseMemoryQuestion(
            section: Section,
            mode: PresentMode,
            durations: Durations,
            errors: MutableList<ParseError>,
            sourceName: String,
        ): Question? {
            val dotsField = section.fields["dotsPositions"]
            if (dotsField == null) {
                errors.add(missingField(sourceName, section, "dotsPositions"))
                return null
            }
            val parts = dotsField.value.split(",").map { it.trim() }
            val parsed = parts.map { it.toIntOrNull() }
            val invalid =
                parsed.any { it == null } ||
                    parts.size != MEMORY_DOTS_COUNT ||
                    parsed.any { it != null && (it < 0 || it > 63) } ||
                    parsed.filterNotNull().toSet().size != MEMORY_DOTS_COUNT
            if (invalid) {
                errors.add(
                    invalidField(
                        sourceName, section.qid, dotsField.line, "dotsPositions",
                        "记忆题点位非法：必须为 10 个互不重复的 0-63 索引",
                    ),
                )
                return null
            }
            val positions = parsed.filterNotNull()
            val flashDurationMs =
                section.fields["flashDurationMs"]?.value?.toLongOrNull() ?: DEFAULT_FLASH_DURATION_MS
            val flashIntervalMs =
                section.fields["flashIntervalMs"]?.value?.toLongOrNull() ?: DEFAULT_FLASH_INTERVAL_MS
            return Question.Memory(
                qid = section.qid,
                mode = mode,
                stemDurationMs = durations.stemDurationMs,
                optionsDurationMs = durations.optionsDurationMs,
                dotsPositions = positions,
                flashDurationMs = flashDurationMs,
                flashIntervalMs = flashIntervalMs,
            )
        }

        // ---------------------------------------------------------------------
        // Stem / Options / Scores
        // ---------------------------------------------------------------------

        private fun parseStem(
            sourceName: String,
            section: Section,
            errors: MutableList<ParseError>,
        ): StemContent? {
            val field = section.fields["stem"]
            if (field == null) {
                errors.add(missingField(sourceName, section, "stem"))
                return null
            }
            val parts = field.value.split("|").map { it.trim() }
            if (parts.isEmpty() || parts.any { it.isEmpty() }) {
                errors.add(
                    invalidField(
                        sourceName, section.qid, field.line, "stem",
                        "题干非法：不能包含空段",
                    ),
                )
                return null
            }
            val pieces =
                parts.map { raw ->
                    when {
                        raw.startsWith(IMAGE_PREFIX) -> {
                            val ref = parseImageRef(raw)
                            if (!validateAsset(sourceName, section, field.line, "stem", ref.fileName, errors)) {
                                return null
                            }
                            StemContent.Image(ref.fileName, ref.widthDp, ref.heightDp)
                        }
                        raw.startsWith(AUDIO_PREFIX) -> {
                            val audioRef = parseAudioRef(raw)
                            if (audioRef == null) {
                                errors.add(
                                    invalidField(
                                        sourceName, section.qid, field.line, "stem",
                                        "音频引用非法：扩展名必须为 ${AUDIO_EXT.joinToString("/")}",
                                    ),
                                )
                                return null
                            }
                            if (!validateAsset(sourceName, section, field.line, "stem", AUDIO_SUBDIR + audioRef.fileName, errors)) {
                                return null
                            }
                            StemContent.Audio(fileName = audioRef.fileName, autoPlay = audioRef.autoPlay)
                        }
                        else -> StemContent.Text(raw)
                    }
                }
            return if (pieces.size == 1) pieces[0] else StemContent.Mixed(pieces)
        }

        private fun parseOptions(
            sourceName: String,
            section: Section,
            errors: MutableList<ParseError>,
        ): List<OptionContent>? {
            val field = section.fields["options"]
            if (field == null) {
                errors.add(missingField(sourceName, section, "options"))
                return null
            }
            val parts = field.value.split("|").map { it.trim() }
            if (parts.size < 2 || parts.any { it.isEmpty() }) {
                errors.add(
                    invalidField(
                        sourceName, section.qid, field.line, "options",
                        "选项非法：不能包含空选项",
                    ),
                )
                return null
            }
            val result = mutableListOf<OptionContent>()
            for (part in parts) {
                val parsed = parseOptionPart(sourceName, section, field.line, part, errors)
                    ?: return null
                result.add(parsed)
            }
            return result
        }

        /**
         * Parses a single option part. Supports mixed text+image within one option
         * using `+` as sub-delimiter: `A. +img:opt_a.png` → Mixed([Text, Image]).
         */
        private fun parseOptionPart(
            sourceName: String,
            section: Section,
            line: Int,
            raw: String,
            errors: MutableList<ParseError>,
        ): OptionContent? {
            if (!raw.contains(OPTION_MIX_DELIMITER)) {
                // No sub-delimiter → pure text or pure image (original behavior)
                return if (raw.startsWith(IMAGE_PREFIX)) {
                    val ref = parseImageRef(raw)
                    if (!validateAsset(sourceName, section, line, "options", ref.fileName, errors)) {
                        return null
                    }
                    OptionContent.Image(ref.fileName, ref.widthDp, ref.heightDp)
                } else {
                    OptionContent.Text(raw)
                }
            }
            // Mixed option: split by + and parse each segment
            val segments = raw.split(OPTION_MIX_DELIMITER).map { it.trim() }
            if (segments.any { it.isEmpty() }) {
                errors.add(
                    invalidField(
                        sourceName, section.qid, line, "options",
                        "选项混排段不能为空（检查多余的 + 分隔符）",
                    ),
                )
                return null
            }
            val pieces = segments.map { seg ->
                if (seg.startsWith(IMAGE_PREFIX)) {
                    val ref = parseImageRef(seg)
                    if (!validateAsset(sourceName, section, line, "options", ref.fileName, errors)) {
                        return null
                    }
                    OptionContent.Image(ref.fileName, ref.widthDp, ref.heightDp)
                } else {
                    OptionContent.Text(seg)
                }
            }
            return if (pieces.size == 1) pieces[0] else OptionContent.Mixed(pieces)
        }

        /**
         * Parses an image reference like `img:file.png`, `img:file.png:400`,
         * or `img:file.png:400x300`. Returns (fileName, widthDp?, heightDp?).
         */
        private data class ImageRef(
            val fileName: String,
            val widthDp: Int? = null,
            val heightDp: Int? = null,
        )

        private fun parseImageRef(raw: String): ImageRef {
            val body = raw.removePrefix(IMAGE_PREFIX)
            val match = IMAGE_SIZE_REGEX.matchEntire(body)
            return if (match != null) {
                val fileName = match.groupValues[1]
                val w = match.groupValues[2].toIntOrNull()
                val h = match.groupValues[3].takeIf { it.isNotEmpty() }?.toIntOrNull()
                ImageRef(fileName, w, h)
            } else {
                ImageRef(body)
            }
        }

        /**
         * Parses an audio reference like `audio:clip.mp3` or
         * `audio:clip.mp3:autoplay=false`. The extension must be one of
         * AUDIO_EXT; autoplay defaults to true.
         */
        private data class AudioRef(
            val fileName: String,
            val autoPlay: Boolean = true,
        )

        private fun parseAudioRef(raw: String): AudioRef? {
            val body = raw.removePrefix(AUDIO_PREFIX).trim()
            if (body.isEmpty()) return null
            val segments = body.split(":").map { it.trim() }
            val fileName = segments.first()
            val ext = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
            if (ext.isEmpty() || ext !in AUDIO_EXT) return null
            var autoPlay = true
            for (seg in segments.drop(1)) {
                val eq = seg.indexOf('=')
                if (eq <= 0) continue
                val key = seg.substring(0, eq).trim().lowercase()
                val value = seg.substring(eq + 1).trim().lowercase()
                if (key == "autoplay") {
                    autoPlay = value !in setOf("false", "0", "no", "off")
                }
            }
            return AudioRef(fileName = fileName, autoPlay = autoPlay)
        }

        private fun validateAsset(
            sourceName: String,
            section: Section,
            line: Int,
            fieldName: String,
            fileName: String,
            errors: MutableList<ParseError>,
        ): Boolean {
            if (!fileSystem.exists(ASSET_DIR + fileName)) {
                errors.add(
                    ParseError(
                        line = line,
                        field = fieldName,
                        code = ParseErrorCode.ASSET_NOT_FOUND,
                        message =
                            "$sourceName [${section.qid}]: ${fieldName}引用的资源 $ASSET_DIR$fileName 不存在",
                    ),
                )
                return false
            }
            return true
        }

        private fun parseScores(
            sourceName: String,
            section: Section,
            expectedLength: Int,
            errors: MutableList<ParseError>,
        ): List<Int>? {
            val field = section.fields["score"]
            if (field == null) {
                errors.add(missingField(sourceName, section, "score"))
                return null
            }
            val parts = field.value.split("|").map { it.trim() }
            val parsed = parts.map { it.toIntOrNull() }
            if (parsed.any { it == null } || parts.size != expectedLength) {
                errors.add(
                    invalidField(
                        sourceName, section.qid, field.line, "score",
                        "评分非法：必须为 $expectedLength 个竖线分隔的整数",
                    ),
                )
                return null
            }
            return parsed.filterNotNull()
        }

        private fun parseShowSubmit(section: Section): Boolean {
            val raw = section.fields["showSubmit"]?.value?.trim()?.lowercase() ?: return true
            return when (raw) {
                "false", "0", "no", "hide", "off" -> false
                else -> true
            }
        }

        private fun parseAutoAdvance(section: Section): Boolean {
            val raw = section.fields["autoAdvance"]?.value?.trim()?.lowercase() ?: return false
            return when (raw) {
                "true", "1", "yes", "on" -> true
                else -> false
            }
        }

        private fun parseOptionsPerRow(
            sourceName: String,
            section: Section,
            errors: MutableList<ParseError>,
        ): Int? {
            val entry = section.fields["optionsPerRow"] ?: return null
            val v = entry.value.trim().toIntOrNull()
            if (v == null || v !in 1..3) {
                errors.add(
                    invalidField(
                        sourceName, section.qid, entry.line, "optionsPerRow",
                        "每行选项个数非法：必须为 1-3 之间的整数",
                    ),
                )
                return null
            }
            return v
        }

        // ---------------------------------------------------------------------
        // Error helpers
        // ---------------------------------------------------------------------

        private fun missingField(
            sourceName: String,
            section: Section,
            fieldName: String,
        ): ParseError =
            ParseError(
                line = section.headerLine,
                field = fieldName,
                code = ParseErrorCode.CONFIG_FIELD_INVALID,
                message =
                    "$sourceName [${section.qid}] 第 ${section.headerLine} 行: 缺少必填字段 $fieldName",
            )

        private fun invalidField(
            sourceName: String,
            qid: String,
            line: Int,
            fieldName: String,
            reason: String,
        ): ParseError =
            ParseError(
                line = line,
                field = fieldName,
                code = ParseErrorCode.CONFIG_FIELD_INVALID,
                message = "$sourceName [$qid] 第 $line 行: $reason",
            )

        companion object {
            private const val ASSET_DIR = "/sdcard/WenJuanPro/assets/"
            private const val AUDIO_SUBDIR = "audios/"
            private const val IMAGE_PREFIX = "img:"
            private const val AUDIO_PREFIX = "audio:"
            private val AUDIO_EXT = setOf("mp3", "wav", "m4a", "ogg", "aac")
            private const val OPTION_MIX_DELIMITER = "+"
            private const val DURATION_MIN_MS = 1000L
            private const val DURATION_MAX_MS = 600_000L
            private const val MEMORY_DOTS_COUNT = 10
            private const val DEFAULT_FLASH_DURATION_MS = 1000L
            private const val DEFAULT_FLASH_INTERVAL_MS = 500L
            private val LINE_SPLIT_REGEX = Regex("\r?\n")
            private val SECTION_REGEX = Regex("^\\[Q[1-9]\\d*\\]$")
            private val SECTION_LIKE_REGEX = Regex("^\\[.*\\]$")
            private val HEADER_ENTRY_REGEX = Regex("^#\\s*([^:]+?)\\s*:\\s*(.*)$")
            private val CONFIG_ID_REGEX = Regex("^[A-Za-z0-9_-]{1,64}$")
            private val IMAGE_SIZE_REGEX = Regex("^(.+\\.\\w+):(\\d+)(?:x(\\d+))?$")
            private val TYPE_VALUES = setOf("single", "multi", "memory")
            private val MODE_VALUES = setOf("all_in_one", "staged")
        }
    }
