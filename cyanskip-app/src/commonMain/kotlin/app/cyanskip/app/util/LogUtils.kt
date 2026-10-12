package app.cyanskip.app.util

import app.cyanskip.app.logging.FileLogWriter
import app.cyanskip.app.logging.formatLogValue
import app.cyanskip.app.logging.isLogDebuggable
import app.cyanskip.app.logging.logMetadata
import app.cyanskip.app.logging.writePlatformLog
import app.cyanskip.app.storage.appStorage
import li.songe.codeorigin.CallSite

object LogUtils {
    private val writer = lazy { FileLogWriter(appStorage().log, logMetadata()) }
    private var closed = false

    /** After close, entries go only to the platform diagnostic output. */
    @Synchronized
    fun d(
        vararg args: Any?,
        @CallSite loc: String = "",
        @CallSite("{file}") fileName: String = "",
        tag: String = fileName.substringBeforeLast('.'),
    ) {
        val thread = Thread.currentThread().name
        val location = loc.removePrefix("app.cyanskip.app.")
        val texts = args.map(::formatLogValue)
        if (closed || isLogDebuggable()) {
            writePlatformLog(tag, buildString {
                append("$thread, $location")
                texts.forEachIndexed { index, text ->
                    append(if (texts.size == 1) "\n" else "\n[$index]: ")
                    append(text)
                }
            })
        }
        if (!closed) writer.value.append(tag, thread, location, texts, System.currentTimeMillis())
    }

    @Synchronized
    fun flush() {
        if (!closed && writer.isInitialized()) writer.value.flush()
    }

    @Synchronized
    fun close() {
        if (!closed) {
            closed = true
            if (writer.isInitialized()) writer.value.close()
        }
    }
}
