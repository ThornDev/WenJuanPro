package ai.wenjuanpro.app.domain.model

data class AppFailure(val code: String, val cause: Throwable? = null) :
    RuntimeException("AppFailure(code=$code)", cause)

class SsaidUnavailableException(message: String = "SSAID_UNAVAILABLE") : RuntimeException(message)
