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
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OkHttpResultUploader
    @Inject
    constructor(
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ResultUploader {
        private val client: OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

        override suspend fun upload(file: File): Result<Unit> =
            withContext(ioDispatcher) {
                if (!file.exists()) {
                    return@withContext Result.failure(
                        IllegalStateException("result file missing: ${file.absolutePath}"),
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
                        .post(body)
                        .build()
                runCatching {
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            Result.success(Unit)
                        } else {
                            Timber.w(
                                "result upload http=%d file=%s",
                                response.code,
                                file.name,
                            )
                            Result.failure(
                                IOException("upload http ${response.code}"),
                            )
                        }
                    }
                }.getOrElse { e ->
                    Timber.w(e, "result upload failed file=%s", file.name)
                    Result.failure(e)
                }
            }

        companion object {
            const val UPLOAD_URL: String = "https://ineutech.com/wenjuan/upload"
            private val TEXT_PLAIN = "text/plain; charset=utf-8".toMediaType()
        }
    }
