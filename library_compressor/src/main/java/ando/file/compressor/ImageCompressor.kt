package ando.file.compressor

import android.content.Context
import android.net.Uri
import android.os.*
import android.text.TextUtils
import android.util.Log
import ando.file.core.FileLogger
import ando.file.core.FileUri.getUriByFile
import ando.file.core.FileUri.getUriByPath
import ando.file.core.FileUtils
import java.io.*
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*

class ImageCompressor private constructor(builder: Builder) : Handler.Callback {

    private var mCacheDir: String?
    private var cache = false
    private var focusAlpha = false
    private val mIgnoreMinCompressSize: Int //Byte
    private val mImageRenameListener: OnImageRenameListener?
    private val mImageCompressListener: OnImageCompressListener?
    private val mImageCompressPredicate: ImageCompressPredicate?
    private val mUriProviders: MutableList<Uri>?
    private val mHandler: Handler

    /**
     * Returns a file with a cache image name in the private cache directory.
     *
     * @param context A context.
     */
    private fun getImageCacheFile(
        context: Context,
        suffix: String,
    ): File {
        if (TextUtils.isEmpty(mCacheDir)) {
            mCacheDir = getImageCacheDir(context)?.absolutePath
        }
        val cacheBuilder = mCacheDir + "/" + System.currentTimeMillis() + (Math.random() * 1000).toInt() +
                if (TextUtils.isEmpty(suffix)) ".jpg" else suffix
        return File(cacheBuilder)
    }

    private fun getImageCustomFile(context: Context, filename: String): File {
        if (TextUtils.isEmpty(mCacheDir)) {
            mCacheDir = getImageCacheDir(context)?.absolutePath
        }
        val cacheBuilder = "$mCacheDir/$filename"
        return File(cacheBuilder)
    }

    private fun getImageCacheDir(context: Context): File? = getImageCacheDir(context, DEFAULT_DISK_CACHE_DIR)

    /**
     * start asynchronous compress thread
     */
    private fun launch(context: Context) {
        if (mUriProviders == null || mUriProviders.isNullOrEmpty()) {
            mImageCompressListener?.onError(NullPointerException("image file cannot be null"))
            return
        }
        var position = -1
        mUriProviders.let {
            val iterator = it.iterator()
            while (iterator.hasNext()) {
                val uri: Uri = iterator.next()
                @Suppress("DEPRECATION")
                AsyncTask.execute {
                    try {
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_COMPRESS_START))
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_COMPRESS_SUCCESS, ++position, -1, compress(context, uri)))
                    } catch (e: IOException) {
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_COMPRESS_ERROR, e))
                    }
                }
                iterator.remove()
            }
        }
    }

    /**
     * start compress and return the file
     */
    @Throws(IOException::class)
    private operator fun get(uri: Uri, context: Context): Uri? =
        ImageCompressEngine.compressCompat(
            uri,
            getImageCacheFile(context, ImageChecker.extSuffix(uri)),
            cache,
            focusAlpha
        )

    @Throws(IOException::class)
    private operator fun get(context: Context): List<Uri> {
        val results: MutableList<Uri> = ArrayList()
        mUriProviders?.let {
            val iterator = it.iterator()
            while (iterator.hasNext()) {
                results.add(compress(context, iterator.next()) ?: continue)
                iterator.remove()
            }
            return results
        }
        return emptyList()
    }

    @Throws(IOException::class)
    private fun compress(context: Context, uri: Uri): Uri? = compressReal(context, uri)

    @Throws(IOException::class)
    private fun compressReal(context: Context, uri: Uri): Uri? {
        var targetFile = getImageCacheFile(context, ImageChecker.extSuffix(uri))
        if (mImageRenameListener != null) {
            var filename = mImageRenameListener.rename(uri)
            if (filename.isNullOrBlank()) {
                val originName: String = FileUtils.getFileNameFromUri(uri) ?: UUID.randomUUID().toString().replace("-", "")
                val md = MessageDigest.getInstance("MD5")
                md.update(originName.toByteArray())
                filename = BigInteger(1, md.digest()).toString(32)
            }
            targetFile = getImageCustomFile(context, filename ?: return uri)
        }
        return if (mImageCompressPredicate != null) {
            if (mImageCompressPredicate.apply(uri) && ImageChecker.needCompress(mIgnoreMinCompressSize, uri))
                ImageCompressEngine.compressCompat(uri, targetFile, cache, focusAlpha) else uri
        } else {
            if (ImageChecker.needCompress(mIgnoreMinCompressSize, uri))
                ImageCompressEngine.compressCompat(uri, targetFile, cache, focusAlpha) else uri
        }
    }

    override fun handleMessage(msg: Message): Boolean {
        if (mImageCompressListener == null) return false
        when (msg.what) {
            MSG_COMPRESS_START -> mImageCompressListener.onStart()
            MSG_COMPRESS_SUCCESS -> mImageCompressListener.onSuccess(msg.arg1, msg.obj as? Uri)
            MSG_COMPRESS_ERROR -> mImageCompressListener.onError(msg.obj as? Throwable)
        }
        return false
    }

    class Builder internal constructor(private val context: Context) {
        var mTargetDir: String? = null
        var mCache = false
        var mFocusAlpha = false
        var mIgnoreMinCompressSize = 100 //Byte
        var mImageRenameListener: OnImageRenameListener? = null
        var mImageCompressListener: OnImageCompressListener? = null
        var mImageCompressPredicate: ImageCompressPredicate? = null
        var mUriProviders: MutableList<Uri> = mutableListOf()

        private fun build(): ImageCompressor {
            return ImageCompressor(this)
        }

        fun <T> load(list: List<T>): Builder {
            for (src in list) {
                if (src == null) continue
                when (src) {
                    is String -> load(src as String)
                    is File -> load(src as File)
                    is Uri -> load(src as Uri)
                    else -> throw IllegalArgumentException("Incoming data type exception, it must be String, File, Uri or Bitmap")
                }
            }
            return this
        }

        fun load(file: File): Builder {
            getUriByFile(file)?.let {
                mUriProviders.add(it)
            }
            return this
        }

        fun load(string: String): Builder {
            getUriByPath(string)?.let {
                mUriProviders.add(it)
            }
            return this
        }

        fun load(uri: Uri): Builder {
            mUriProviders.add(uri)
            return this
        }

        fun setRenameListener(listenerImage: OnImageRenameListener?): Builder {
            mImageRenameListener = listenerImage
            return this
        }

        fun setImageCompressListener(listenerImage: OnImageCompressListener?): Builder {
            mImageCompressListener = listenerImage
            return this
        }

        fun setTargetDir(targetDir: String?): Builder {
            mTargetDir = targetDir
            return this
        }

        fun enableCache(cache: Boolean): Builder {
            this.mCache = cache
            return this
        }

        /**
         * Do I need to keep the image's alpha channel
         *
         * @param focusAlpha
         *
         * true - to keep alpha channel, the compress speed will be slow.
         *
         *  false - don't keep alpha channel, it might have a black background.
         */
        fun setFocusAlpha(focusAlpha: Boolean): Builder {
            this.mFocusAlpha = focusAlpha
            return this
        }

        /**
         * do not compress when the origin image file size less than one value
         *
         * @param size the value of file size, unit KB, default 100K
         */
        fun ignoreBy(size: Int): Builder {
            mIgnoreMinCompressSize = size
            return this
        }

        /**
         * do compress image when return value was true, otherwise, do not compress the image file
         *
         * @param imageCompressPredicate A predicate callback that returns true or false for the given input path should be compressed.
         */
        fun filter(imageCompressPredicate: ImageCompressPredicate?): Builder {
            mImageCompressPredicate = imageCompressPredicate
            return this
        }

        /**
         * begin compress image with asynchronous
         */
        fun launch() {
            build().launch(context)
        }

        @Throws(IOException::class)
        operator fun get(path: String): Uri? {
            return getUriByPath(path)?.let {
                build()[it, context]
            }
        }

        /**
         * begin compress image with synchronize
         *
         * @return the thumb image file list
         */
        @Throws(IOException::class)
        fun get(): List<Uri> {
            return build()[context]
        }
    }

    companion object {
        private const val TAG = "ImageCompressor"

        private const val DEFAULT_DISK_CACHE_DIR = "image_disk_cache"
        private const val MSG_COMPRESS_SUCCESS = 0
        private const val MSG_COMPRESS_START = 1
        private const val MSG_COMPRESS_ERROR = 2

        fun with(context: Context): Builder {
            return Builder(context)
        }

        /**
         * Returns a directory with the given name in the private cache directory of the application to
         * use to store retrieved media and thumbnails.
         *
         * @param context   A context.
         * @param cacheName The name of the subdirectory in which to store the cache.
         * @see .getImageCacheDir
         */
        private fun getImageCacheDir(context: Context, cacheName: String): File? {
            val cacheDir = context.externalCacheDir
            if (cacheDir != null) {
                val result = File(cacheDir, cacheName)
                return if (!result.mkdirs() && (!result.exists() || !result.isDirectory)) {
                    // File wasn't able to create a directory, or the result exists but not a directory
                    null
                } else result
            }
            if (Log.isLoggable(TAG, Log.ERROR)) {
                FileLogger.e("default disk cache dir is null")
            }
            return null
        }
    }

    init {
        mCacheDir = builder.mTargetDir
        mImageRenameListener = builder.mImageRenameListener
        mUriProviders = builder.mUriProviders
        cache = builder.mCache
        focusAlpha = builder.mFocusAlpha
        mImageCompressListener = builder.mImageCompressListener
        mIgnoreMinCompressSize = builder.mIgnoreMinCompressSize
        mImageCompressPredicate = builder.mImageCompressPredicate
        mHandler = Handler(Looper.getMainLooper(), this)
    }

}