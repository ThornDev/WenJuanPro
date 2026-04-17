package ai.wenjuanpro.app.data.config

import ai.wenjuanpro.app.data.parser.ParseError
import ai.wenjuanpro.app.domain.model.Config

sealed interface ConfigLoadResult {
    data class Valid(val config: Config) : ConfigLoadResult

    data class Invalid(val fileName: String, val errors: List<ParseError>) : ConfigLoadResult
}
