package ai.wenjuanpro.app.data.upload

import ai.wenjuanpro.app.core.concurrency.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

@Singleton
class OkHttpResultUploader
    @Inject
    constructor(
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ResultUploader {
        // Internal/private deployment: skip TLS chain validation and host
        // matching so the upload works against self-signed or
        // misconfigured-chain backends without bundling certs.
        private val trustAllManager =
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) = Unit

                override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) = Unit

                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }

        private val sslSocketFactory: SSLSocketFactory =
            SSLContext.getInstance("TLS").apply {
                init(null, arrayOf(trustAllManager), SecureRandom())
            }.socketFactory

        private val client: OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .sslSocketFactory(sslSocketFactory, trustAllManager)
                .hostnameVerifier { _, _ -> true }
                .build()

        override suspend fun upload(file: File): Result<Unit> =
            withContext(ioDispatcher) {
                if (!file.exists()) {
                    return@withContext Result.failure(
                        IllegalStateException("result file missing: ${file.absolutePath}"),
                    )
                }
                val length = file.length()
                if (length <= 0L) {
                    return@withContext Result.failure(
                        IllegalStateException("result file empty: ${file.absolutePath}"),
                    )
                }
                if (length > MAX_BYTES) {
                    return@withContext Result.failure(
                        IllegalStateException(
                            "result file exceeds 5MB: ${file.name} (${length}B)",
                        ),
                    )
                }
                if (!FILENAME_WHITELIST.matches(file.name)) {
                    return@withContext Result.failure(
                        IllegalStateException("result filename not whitelist-safe: ${file.name}"),
                    )
                }
                val body =
                    MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(
                            name = "file",
                            filename = file.name,
                            body = file.asRequestBody(TEXT_PLAIN),
                        )
                        .build()
                val request =
                    Request.Builder()
                        .url(UPLOAD_URL)
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/json, */*")
                        .post(body)
                        .build()
                runCatching {
                    client.newCall(request).execute().use { response ->
                        val bodyText =
                            runCatching { response.body?.string() }.getOrNull().orEmpty()
                        if (response.isSuccessful) {
                            Timber.d(
                                "result upload ok http=%d file=%s bytes=%d",
                                response.code,
                                file.name,
                                length,
                            )
                            Result.success(Unit)
                        } else {
                            Timber.w(
                                "result upload http=%d file=%s body=%s",
                                response.code,
                                file.name,
                                bodyText.take(500),
                            )
                            Result.failure(
                                IOException(
                                    "HTTP ${response.code}: ${bodyText.take(200).ifBlank { response.message }}",
                                ),
                            )
                        }
                    }
                }.getOrElse { e ->
                    Timber.w(e, "result upload threw file=%s", file.name)
                    Result.failure(e)
                }
            }

        companion object {
            const val UPLOAD_URL: String = "https://ineutech.com/wenjuan/upload"
            private const val MAX_BYTES: Long = 5L * 1024L * 1024L
            // Server-side whitelist guard: keep our own client-side check in
            // sync to surface a meaningful error instead of an opaque 4xx.
            private val FILENAME_WHITELIST = Regex("^[A-Za-z0-9._-]{1,200}\\.txt$")
            private val TEXT_PLAIN = "text/plain".toMediaType()
            private const val USER_AGENT = "WenJuanPro-Android/1.0"
        }
    }
