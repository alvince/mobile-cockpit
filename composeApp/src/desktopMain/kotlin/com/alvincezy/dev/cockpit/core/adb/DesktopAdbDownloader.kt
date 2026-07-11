package com.alvincezy.dev.cockpit.core.adb

import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okio.FileSystem
import okio.Path
import okio.buffer

/**
 * Downloads platform-tools archives with Ktor.
 *
 * URL strategy: try the configured mirror base(s) first, then fall back to the
 * official Google repository. Retries use exponential backoff.
 */
class DesktopAdbDownloader(
    private val httpClient: HttpClient,
    private val fileSystem: FileSystem,
    private val configStore: AdbConfigStore,
    private val mirrorBaseUrls: List<String> = emptyList(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AdbDownloader {

    override fun download(
        version: String,
        platform: AdbPlatform,
        destination: Path,
    ): Flow<DownloadProgress> = flow {
        val config = configStore.load()
        val urls = buildUrls(version, platform)
        val partial = destination.parent!! / "${destination.name}.partial"

        destination.parent?.let { fileSystem.createDirectories(it) }

        var lastError: Throwable? = null
        for (url in urls) {
            var attempt = 1
            while (attempt <= config.retryCount) {
                try {
                    emit(DownloadProgress.Started(url))
                    downloadTo(url, partial) { read, total ->
                        emit(DownloadProgress.InProgress(read, total))
                    }
                    if (fileSystem.exists(destination)) fileSystem.delete(destination)
                    fileSystem.atomicMove(partial, destination)
                    emit(DownloadProgress.Done(destination))
                    return@flow
                } catch (cancel: kotlinx.coroutines.CancellationException) {
                    runCatching { if (fileSystem.exists(partial)) fileSystem.delete(partial) }
                    throw cancel
                } catch (t: Throwable) {
                    lastError = t
                    runCatching { if (fileSystem.exists(partial)) fileSystem.delete(partial) }
                    if (attempt < config.retryCount) {
                        val backoff = config.retryBaseDelayMs * (1L shl (attempt - 1))
                        delay(backoff)
                    }
                    attempt++
                }
            }
        }
        emit(DownloadProgress.Failed(lastError ?: IllegalStateException("download failed: $urls")))
    }.flowOn(ioDispatcher)

    private suspend inline fun downloadTo(
        url: String,
        partial: Path,
        crossinline onProgress: suspend (read: Long, total: Long) -> Unit,
    ) {
        httpClient.prepareGet(url).execute { response ->
            val total = response.contentLength() ?: -1L
            val channel = response.bodyAsChannel()
            var read = 0L
            val buffer = ByteArray(DEFAULT_CHUNK)
            fileSystem.sink(partial).buffer().use { sink ->
                while (!channel.isClosedForRead) {
                    currentCoroutineContext().ensureActive()
                    val count = channel.readAvailable(buffer, 0, buffer.size)
                    if (count <= 0) continue
                    sink.write(buffer, 0, count)
                    read += count
                    onProgress(read, total)
                }
                sink.flush()
            }
        }
    }

    private fun buildUrls(version: String, platform: AdbPlatform): List<String> {
        val zip = platform.zipName(version)
        val mirrors = mirrorBaseUrls.map { base -> base.trimEnd('/') + "/" + zip }
        val official = "$GOOGLE_BASE/$zip"
        return (mirrors + official).distinct()
    }

    private companion object {
        const val GOOGLE_BASE = "https://dl.google.com/android/repository"
        const val DEFAULT_CHUNK = 64 * 1024
    }
}
