package ai.wenjuanpro.app.data.parser

import ai.wenjuanpro.app.domain.model.Config

sealed interface ParseResult {
    data class Success(val config: Config) : ParseResult

    data class Failure(val errors: List<ParseError>) : ParseResult
}
