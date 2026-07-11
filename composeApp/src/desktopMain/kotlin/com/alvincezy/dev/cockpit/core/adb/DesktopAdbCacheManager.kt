package com.alvincezy.dev.cockpit.core.adb

import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import okio.FileSystem
import okio.Path
import okio.buffer

/**
 * Manages the platform-tools cache under `~/.cockpit/tools/platform-tools/<version>/`.
 *
 * Installation is atomic: the archive is unzipped into a temp directory, validated,
 * then renamed into the versioned directory. A lock file guards against concurrent
 * installs corrupting the cache.
 */
class DesktopAdbCacheManager(
    private val fileSystem: FileSystem,
    private val toolsDir: Path = DesktopPaths.toolsDir,
    private val platform: AdbPlatform = DesktopPaths.currentPlatform() ?: AdbPlatform.LINUX_X64,
) : AdbCacheManager {

    override fun cachedAdb(version: String): Path? {
        val adb = adbPath(version)
        return if (fileSystem.exists(adb) && validateCache(version)) adb else null
    }

    override fun install(archive: Path, version: String, expectedSha256: String?): Path {
        if (expectedSha256 != null) {
            val actual = sha256(archive)
            if (!actual.equals(expectedSha256, ignoreCase = true)) {
                throw AdbVerificationException(version, expectedSha256, actual)
            }
        }

        fileSystem.createDirectories(toolsDir)
        val lock = toolsDir / "$version.lock"
        withFileLock(lock) {
            val versionDir = toolsDir / version
            val tmpDir = toolsDir / "$version.tmp"

            if (fileSystem.exists(tmpDir)) fileSystem.deleteRecursively(tmpDir)
            fileSystem.createDirectories(tmpDir)

            unzip(archive, tmpDir)
            markExecutable(tmpDir)

            if (fileSystem.exists(versionDir)) fileSystem.deleteRecursively(versionDir)
            fileSystem.atomicMove(tmpDir, versionDir)
        }

        val adb = adbPath(version)
        if (!fileSystem.exists(adb)) {
            throw AdbVerificationException(version, "adb executable present", "missing at $adb")
        }
        return adb
    }

    override fun validateCache(version: String): Boolean {
        val adb = adbPath(version)
        if (!fileSystem.exists(adb)) return false
        if (fileSystem.metadataOrNull(adb)?.isRegularFile != true) return false
        // Best-effort executable check on real filesystems; skipped for virtual ones.
        val file = File(adb.toString())
        return !file.exists() || file.canExecute()
    }

    override fun trimCache(keep: Int) {
        if (keep < 0) return
        if (!fileSystem.exists(toolsDir)) return
        val versionDirs = fileSystem.list(toolsDir)
            .filter { fileSystem.metadataOrNull(it)?.isDirectory == true }
            .filter { !it.name.endsWith(".tmp") }
            .sortedByDescending { fileSystem.metadataOrNull(it)?.lastModifiedAtMillis ?: 0L }

        versionDirs.drop(keep).forEach { fileSystem.deleteRecursively(it) }
    }

    /** platform-tools zips extract to a top-level `platform-tools/` directory. */
    private fun adbPath(version: String): Path =
        toolsDir / version / "platform-tools" / platform.executableName

    private fun unzip(archive: Path, destination: Path) {
        fileSystem.source(archive).buffer().inputStream().use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val outPath = destination / entry.name
                    if (entry.isDirectory) {
                        fileSystem.createDirectories(outPath)
                    } else {
                        outPath.parent?.let { fileSystem.createDirectories(it) }
                        fileSystem.sink(outPath).buffer().use { sink ->
                            val buffer = ByteArray(8192)
                            var read = zip.read(buffer)
                            while (read >= 0) {
                                sink.write(buffer, 0, read)
                                read = zip.read(buffer)
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
    }

    private fun markExecutable(dir: Path) {
        val adb = dir / "platform-tools" / platform.executableName
        if (fileSystem.exists(adb)) {
            File(adb.toString()).setExecutable(true, false)
        }
    }

    private fun sha256(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        fileSystem.source(path).buffer().use { source ->
            val buffer = ByteArray(8192)
            val stream = source.inputStream()
            var read = stream.read(buffer)
            while (read >= 0) {
                digest.update(buffer, 0, read)
                read = stream.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private inline fun withFileLock(lockFile: Path, block: () -> Unit) {
        lockFile.parent?.let { fileSystem.createDirectories(it) }
        // Acquire an OS-level lock on real filesystems. On virtual filesystems the
        // RandomAccessFile path won't exist, so fall back to running unguarded.
        val raf = runCatching { java.io.RandomAccessFile(lockFile.toString(), "rw") }.getOrNull()
        if (raf == null) {
            block()
            return
        }
        raf.use { file ->
            val lock = file.channel.lock()
            try {
                block()
            } finally {
                lock.release()
            }
        }
        runCatching { fileSystem.delete(lockFile) }
    }
}

class AdbVerificationException(version: String, expected: String, actual: String) :
    Exception("platform-tools $version verification failed: expected=$expected actual=$actual")
