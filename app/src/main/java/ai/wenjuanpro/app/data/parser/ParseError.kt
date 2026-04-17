package ai.wenjuanpro.app.data.parser

enum class ParseErrorCode {
    CONFIG_HEADER_MISSING,
    CONFIG_FIELD_INVALID,
    ASSET_NOT_FOUND,
    ENCODING_INVALID,
}

data class ParseError(
    val line: Int,
    val field: String?,
    val code: ParseErrorCode,
    val message: String,
)
