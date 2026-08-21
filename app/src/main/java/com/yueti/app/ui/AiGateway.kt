package com.yueti.app.ui

/** Stable seam for a future HTTPS proxy. The offline build never performs a network request. */
internal interface AiGateway {
    val configured: Boolean
    suspend fun chat(prompt: String, mode: AssistantMode): Result<String>
    suspend fun graph(prompt: String): Result<List<GraphExpression>>
}

internal object OfflineAiGateway : AiGateway {
    override val configured = false
    override suspend fun chat(prompt: String, mode: AssistantMode) =
        Result.failure<String>(IllegalStateException("AI 服务未配置"))

    override suspend fun graph(prompt: String) =
        Result.failure<List<GraphExpression>>(IllegalStateException("AI 服务未配置"))
}
