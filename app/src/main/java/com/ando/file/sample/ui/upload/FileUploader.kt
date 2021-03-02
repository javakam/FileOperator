package com.ando.file.sample.ui.upload

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.closeQuietly
import okio.*
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URLConnection
import java.util.UUID
import kotlin.coroutines.resumeWithException



/**
 * proguard :  -keepnames class okio.RealBufferedSink
 */

class ProgressRequestBody(
    val requestBody: RequestBody,
    val listener: (total: Long, size: Long, increment: Long) -> Unit,
) :
    RequestBody() {

    @Throws(IOException::class)
    override fun contentLength(): Long {
        return requestBody.contentLength()
    }

    override fun contentType(): MediaType? {
        return requestBody.contentType()
    }

    var currentLength = 0

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        // 如果拦截器打印请求消息会导致多次调用writeTo方法，只对真正的网络RealBufferedSink做进度封装
        if (sink.javaClass.simpleName.contains("RealBufferedSink")) {
            currentLength = 0
            val forwardingSink: ForwardingSink = object : ForwardingSink(sink) {
                @Throws(IOException::class)
                override fun write(source: Buffer, byteCount: Long) {
                    currentLength += byteCount.toInt()
                    super.write(source, byteCount)
                    listener(contentLength(), currentLength.toLong(), byteCount)
                }
            }
            val bufferedSink = forwardingSink.buffer()
            requestBody.writeTo(bufferedSink)
            bufferedSink.flush()
        } else {
            requestBody.writeTo(sink)
        }
    }
}

/**
 * 每次进度上传固定大小  SEGMENT_SIZE
 */
class FileProgressRequestBody(
    val file: File,
    val contentType: MediaType? = URLConnection.guessContentTypeFromName(file.absolutePath)
        .toMediaTypeOrNull(),
    val listener: (total: Long, size: Long, increment: Long) -> Unit,
) : RequestBody() {
    override fun contentLength(): Long {
        return file.length()
    }

    override fun contentType(): MediaType? {
        return contentType
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        // 如果拦截器打印请求消息会导致多次调用writeTo方法，只对真正的网络RealBufferedSink做进度封装
        if (sink.javaClass.simpleName.contains("RealBufferedSink")) {
            var source: Source? = null
            try {
                source = file.source()
                var total: Long = 0
                var read: Long
                while (source.read(sink.buffer, SEGMENT_SIZE.toLong()).also { read = it } != -1L) {
                    total += read
                    sink.flush()
                    listener(contentLength(), total, read)
                }
            } finally {
                source?.closeQuietly()
            }
        } else {
            file.source().use { source -> sink.writeAll(source) }
        }
    }

    companion object {
        const val SEGMENT_SIZE = 512
    }
}

fun File.asFileProgressRequestBody(
    contentType: MediaType? = URLConnection.guessContentTypeFromName(absolutePath)
        .toMediaTypeOrNull(),
    listener: (total: Long, size: Long, increment: Long) -> Unit,
): FileProgressRequestBody {
    return FileProgressRequestBody(this, contentType, listener)
}

class ProgressRequestBody2(private val contentType: MediaType?, private val file: File, private val listener: (totalBytesRead: Long) -> Unit) :
    RequestBody() {
    override fun contentType(): MediaType? {
        return contentType
    }

    override fun contentLength(): Long {
        return file.length()
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        var source: Source? = null
        try {
            source = file.source()
            var totalBytesRead: Long = 0
            var readCount: Long
            while (source.read(sink.buffer, 8192).also { readCount = it } != -1L) {
                totalBytesRead += readCount
                listener.invoke(totalBytesRead)
            }
        } finally {
            source?.closeQuietly()
        }
    }
}

fun File.asProgressRequestBody2(
    mediaType: MediaType? = URLConnection.guessContentTypeFromName(absolutePath)
        .toMediaTypeOrNull(),
    listener: (totalBytesRead: Long) -> Unit,
): ProgressRequestBody2 {
    return ProgressRequestBody2(
        mediaType,
        this,
        listener
    )
}

fun File.asProgressRequestBody(
    mediaType: MediaType? = URLConnection.guessContentTypeFromName(absolutePath)
        .toMediaTypeOrNull(),
    listener: (total: Long, size: Long, increment: Long) -> Unit,
): ProgressRequestBody {
    return ProgressRequestBody(
        asRequestBody(mediaType),
        listener
    )
}

////////////////////////////////////////////////////////////////////////////////////////////////////////


inline fun <A> tryOrNull(message: String? = null, operation: () -> A): A? {
    return try {
        operation()
    } catch (any: Throwable) {
        if (message != null) {
            //Timber.e(any, message)
        }
        null
    }
}

internal class ProgressRequestBody3(
    private val delegate: RequestBody,
    private val listener: Listener,
) : RequestBody() {

    private lateinit var countingSink: CountingSink

    override fun contentType(): MediaType? {
        return delegate.contentType()
    }

    override fun isOneShot() = delegate.isOneShot()

    override fun isDuplex() = delegate.isDuplex()

    val length = tryOrNull { delegate.contentLength() } ?: -1

    override fun contentLength() = length

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        countingSink = CountingSink(sink)
        val bufferedSink = countingSink.buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }

    private inner class CountingSink(delegate: Sink) : ForwardingSink(delegate) {

        private var bytesWritten: Long = 0

        @Throws(IOException::class)
        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            bytesWritten += byteCount
            listener.onProgress(bytesWritten, contentLength())
        }
    }

    interface Listener {
        fun onProgress(current: Long, total: Long)
    }
}

internal class FileUploader constructor(
    private val okHttpClient: OkHttpClient,
    private val context: Context,
) {

    private val uploadUrl = "xxxxx"

    suspend fun uploadFile(
        file: File,
        filename: String?,
        mimeType: String?,
        progressListener: ProgressRequestBody3.Listener? = null,
    ) {
        val uploadBody = object : RequestBody() {
            override fun contentLength() = file.length()

            // Disable okhttp auto resend for 'large files'
            override fun isOneShot() = contentLength() == 0L || contentLength() >= 1_000_000

            override fun contentType(): MediaType? {
                return mimeType?.toMediaTypeOrNull()
            }

            override fun writeTo(sink: BufferedSink) {
                file.source().use { sink.writeAll(it) }
            }
        }

        return upload(uploadBody, filename, progressListener)
    }

    suspend fun uploadByteArray(
        byteArray: ByteArray,
        filename: String?,
        mimeType: String?,
        progressListener: ProgressRequestBody3.Listener? = null,
    ) {
        val uploadBody = byteArray.toRequestBody(mimeType?.toMediaTypeOrNull())
        return upload(uploadBody, filename, progressListener)
    }

    suspend fun uploadFromUri(
        uri: Uri,
        filename: String?,
        mimeType: String?,
        progressListener: ProgressRequestBody3.Listener? = null,
    ) {
        val inputStream = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)
        } ?: throw FileNotFoundException()
        val workingFile = File.createTempFile(UUID.randomUUID().toString(), null, context.cacheDir)
        workingFile.outputStream().use {
            inputStream.copyTo(it)
        }
        return uploadFile(workingFile, filename, mimeType, progressListener).also {
            tryOrNull { workingFile.delete() }
        }
    }

    private suspend fun upload(uploadBody: RequestBody, filename: String?, progressListener: ProgressRequestBody3.Listener?) {
        val urlBuilder = uploadUrl.toHttpUrlOrNull()?.newBuilder() ?: throw RuntimeException()

        val httpUrl = urlBuilder
            .addQueryParameter("filename", filename)
            .build()

        val requestBody = if (progressListener != null) ProgressRequestBody3(uploadBody, progressListener) else uploadBody

        val request = Request.Builder()
            .url(httpUrl)
            .post(requestBody)
            .build()

        return okHttpClient.newCall(request).awaitResponse().use { response ->
            if (!response.isSuccessful) {
                //throw response.toFailure(globalErrorReceiver)
            } else {
                response.body?.source()?.let {
                    //responseAdapter.fromJson(it)

                } ?: throw IOException()
            }
        }
    }


    internal suspend fun okhttp3.Call.awaitResponse(): okhttp3.Response {
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                cancel()
            }

            enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response) {
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }
            })
        }
    }
}