package ando.file.compressor

import android.net.Uri
import java.io.File

interface OnImageCompressListener {
    /**
     * Fired when the compression is started, override to handle in your own code
     */
    fun onStart()

    /**
     * Fired when a compression returns successfully, override to handle in your own code
     */
    fun onSuccess(uri: Uri?)

    /**
     * Fired when a compression fails to complete, override to handle in your own code
     */
    fun onError(e: Throwable?)

}