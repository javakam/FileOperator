package com.ando.file.sample.ui.storage

import ando.file.androidq.*
import ando.file.androidq.FileOperatorQ.buildQuerySelectionStatement
import ando.file.androidq.FileOperatorQ.createContentValues
import ando.file.androidq.FileOperatorQ.deleteUri
import ando.file.androidq.FileOperatorQ.deleteUriDirectory
import ando.file.androidq.FileOperatorQ.deleteUriMediaStoreImage
import ando.file.androidq.FileOperatorQ.insertBitmap
import ando.file.androidq.FileOperatorQ.loadThumbnail
import ando.file.androidq.FileOperatorQ.queryMediaStoreImages
import android.annotation.SuppressLint
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Intent
import android.content.IntentSender
import android.graphics.*
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.TextPaint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ando.file.core.*
import ando.file.core.FileGlobal.MEDIA_TYPE_IMAGE
import ando.file.core.FileGlobal.MODE_READ_ONLY
import ando.file.core.FileGlobal.dumpParcelFileDescriptor
import ando.file.core.FileGlobal.openFileDescriptor
import android.widget.Button
import android.widget.TextView
import com.ando.file.sample.R
import com.ando.file.sample.utils.PermissionManager
import com.bumptech.glide.Glide
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

/**
 * # MediaStoreActivity
 *
 * 沙盒 -> APP卸载,数据删除 (Sandbox -> APP uninstall, data deletion)
 *
 * @author javakam
 * @date 2020/6/2  15:12
 */
class MediaStoreActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CODE_SENDER = 0x10
        val RELATIVE_PATH = "${Environment.DIRECTORY_PICTURES}${File.separator}img"
    }

    private lateinit var tvMediaStoreTip: TextView
    private lateinit var insertBitmapToPictures: Button
    private lateinit var queryFileByDisplayName: Button
    private lateinit var updateFileByMediaStoreBtn: Button
    private lateinit var deleteFileByUri: Button
    private lateinit var deleteFileAll: Button
    private lateinit var queryFileByAll: Button
    private lateinit var imageIv: ImageView
    private lateinit var imageIv1: ImageView
    private lateinit var imageIv2: ImageView
    private lateinit var rvMediaImages: RecyclerView

    private var mInsertUri: Uri? = null
    private var mQueryUri: Uri? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_store)
        tvMediaStoreTip = findViewById(R.id.tvMediaStoreTip)
        insertBitmapToPictures = findViewById(R.id.insertBitmapToPictures)
        queryFileByDisplayName = findViewById(R.id.queryFileByDisplayName)
        imageIv = findViewById(R.id.imageIv)
        imageIv1 = findViewById(R.id.imageIv1)
        imageIv2 = findViewById(R.id.imageIv2)
        updateFileByMediaStoreBtn = findViewById(R.id.updateFileByMediaStoreBtn)
        deleteFileByUri = findViewById(R.id.deleteFileByUri)
        deleteFileAll = findViewById(R.id.deleteFileAll)
        queryFileByAll = findViewById(R.id.queryFileByAll)
        rvMediaImages = findViewById(R.id.rvMediaImages)

        title = "MediaStore"

        tvMediaStoreTip.text =
            """
                    👉 1.增删不需要权限,读取文件需要READ_EXTERNAL_STORAGE权限
                    👉 2.系统只提供了多媒体文件的读权限，没有提供写权限，应用无法直接通过申请写权限修改其他应用生成的文件 ,
                      如果需要 修改/删除 其他应用文件需要捕获 RecoverableSecurityException 进一步处理。 
                """.trimIndent()

        //1.MediaStore Create
        insertBitmapToPictures.setOnClickListener {
            insertBitmapToPictures()
        }
        //2.MediaStore Query
        queryFileByMediaStore()
        //3.MediaStore Modify
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            updateFileByMediaStore()
        }
        //4.MediaStore Delete
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            deleteFileByMediaStore()
        }
    }

    /**
     * 使用`MediaStore`创建文件 (Use `Media Store` to create files)
     */
    private fun createFileByMediaStore() {
        insertBitmapToPictures.setOnClickListener {
            insertBitmapToPictures()
        }
    }

    fun insertBitmapToPictures() {
        val values = createContentValues(
            "BitmapImage.png",
            "This is an image",
            "image/png",
            "Image.png",
            RELATIVE_PATH,
            1
        )
        //创建了一个红色的图片 (Created a red picture)
        val bitmap = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.RED)
        val text = "${SystemClock.currentThreadTimeMillis()}"
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        textPaint.color = Color.WHITE
        textPaint.textSize = 80F
        val rect = Rect()
        textPaint.getTextBounds(text, 0, text.length, rect)
        canvas.drawText(
            text,
            0,
            text.length,
            300F - rect.width() / 2,
            200F + rect.height() / 2,
            textPaint
        )

        mInsertUri = insertBitmap(bitmap, values)
    }

    /**
     * 通过`MediaStore`查询文件 (Query files via `Media Store`)
     */
    private fun queryFileByMediaStore() {

        queryFileByDisplayName.setOnClickListener {

            //通过 DisplayName 查询图片
            //1.读取文件需要权限 READ_EXTERNAL_STORAGE
            //2.系统只提供了多媒体文件的读权限，没有提供写权限，应用无法通过申请写权限修改其他应用生成的文件
            PermissionManager.requestStoragePermission(this) { per ->
                if (per) {
                    mQueryUri = queryMediaStoreImages(
                        "BitmapImage.png", true
                    )

                    // 根据 Uri，获取 Bitmap
                    imageIv.setImageBitmap(null)
                    val pfd: ParcelFileDescriptor? = openFileDescriptor(mQueryUri, MODE_READ_ONLY)
                    pfd?.let {
                        it.use { pfdNoNull ->
                            dumpParcelFileDescriptor(pfdNoNull) //Log

                            // java.lang.NullPointerException: fdObj == null
                            val bitmap = BitmapFactory.decodeFileDescriptor(pfdNoNull.fileDescriptor)
                            imageIv.setImageBitmap(bitmap)
                        }
                    }

                    // 根据 Uri，获取 Thumbnail
                    imageIv1.setImageBitmap(null)
                    val bitmap = loadThumbnail(mQueryUri, 50, 100)
                    imageIv1.setImageBitmap(bitmap)
                } else {

                }
            }
        }

        //查询全部 (Query all)
        queryFileByAll.setOnClickListener {
            //查询全部
//            val images = queryMediaStoreImages()

            //条件查询
//            val projection = arrayOf(
//                MediaStore.Images.Media._ID,
//                MediaStore.Images.Media.DISPLAY_NAME,
//                MediaStore.Images.Media.DATE_ADDED
//            )

            //
            val queryStatement = buildQuerySelectionStatement(
                MEDIA_TYPE_IMAGE, null, null,
                null, null, null, true
            )
            queryStatement.append(
                "${MediaStore.Images.Media.DATE_ADDED} >= ?",
                dateToTimestamp(day = 22, month = 10, year = 2008).toString()
            )
            //
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            //图片集合
            val images = queryMediaStoreImages(null, sortOrder, queryStatement)

            rvMediaImages.layoutManager = GridLayoutManager(this, 5)
            val galleryAdapter = GalleryAdapter { image ->
                val deleteResult = deleteUriMediaStoreImage(this, image, REQUEST_CODE_SENDER)
                if (deleteResult) {
                    queryFileByAll.performClick()
                }
            }
            rvMediaImages.adapter = galleryAdapter
            galleryAdapter.submitList(images)
        }
    }

    @Suppress("SameParameterValue")
    @SuppressLint("SimpleDateFormat")
    private fun dateToTimestamp(day: Int, month: Int, year: Int): Long =
        SimpleDateFormat("dd.MM.yyyy").let { formatter ->
            TimeUnit.MICROSECONDS.toSeconds(formatter.parse("$day.$month.$year")?.time ?: 0)
        }

    val mDiffCallback = object : DiffUtil.ItemCallback<MediaStoreImage>() {
        override fun areItemsTheSame(oldItem: MediaStoreImage, newItem: MediaStoreImage) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: MediaStoreImage, newItem: MediaStoreImage) =
            oldItem == newItem
    }

    private inner class GalleryAdapter(val onClick: (MediaStoreImage) -> Unit) :
        ListAdapter<MediaStoreImage, ImageViewHolder>(mDiffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val view = layoutInflater.inflate(R.layout.layout_media_store_gallery, parent, false)
            return ImageViewHolder(view, onClick)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val mediaStoreImage = getItem(position)
            FileLogger.w("MediaStoreImage = $mediaStoreImage")
            holder.rootView.tag = mediaStoreImage

            Glide.with(holder.imageView)
                .load(mediaStoreImage.uri)
                .thumbnail(0.33f)
                .centerCrop()
                .into(holder.imageView)
        }
    }

    private class ImageViewHolder(view: View, onClick: (MediaStoreImage) -> Unit) :
        RecyclerView.ViewHolder(view) {
        val rootView = view
        val imageView: ImageView = view.findViewById(R.id.image)

        init {
            imageView.setOnClickListener {
                val image = rootView.tag as? MediaStoreImage ?: return@setOnClickListener
                onClick(image)
            }
        }
    }

    /**
     * 根据查询得到的Uri，修改文件
     *
     * todo 2020年5月24日 10:38:52   contentResolver 修改文件
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun updateFileByMediaStore() {

        updateFileByMediaStoreBtn.setOnClickListener {

            //需要 READ_EXTERNAL_STORAGE 权限
            PermissionManager.requestStoragePermission(this) { b: Boolean ->
                if (b) {

                    //这里的 img 是我相册里的图片，需要换成你自己的
                    val queryUri = queryMediaStoreImages("zp1548551182218.jpg")
                    val bitmap = loadThumbnail(queryUri, 400, 100)
                    imageIv2.setImageBitmap(bitmap)

                    var os: OutputStream? = null
                    try {
                        queryUri?.let { uri ->
                            os = contentResolver.openOutputStream(uri)
                            FileLogger.d("修改成功")
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } catch (e1: RecoverableSecurityException) {
                        //e1.printStackTrace()
                        try {
                            @Suppress("DEPRECATION")
                            startIntentSenderForResult(
                                e1.userAction.actionIntent.intentSender,
                                REQUEST_CODE_SENDER,
                                null,
                                0,
                                0,
                                0,
                                null
                            )
                        } catch (e2: IntentSender.SendIntentException) {
                            e2.printStackTrace()
                        }
                    } finally {
                        os?.close()
                    }
                } else {
                    FileLogger.d("没有 READ_EXTERNAL_STORAGE 权限，请动态申请")
                    PermissionManager.requestStoragePermission(this)
                }

            }
        }
    }

    /**
     * 删除MediaStore文件
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun deleteFileByMediaStore() {
        deleteFileByUri.setOnClickListener {
            //根据Uri删除
            deleteUri(this, mInsertUri, REQUEST_CODE_SENDER)

            //根据文件名删除
//            val queryUri = queryMediaStoreImages("BitmapImage (2).png")
//            deleteUri(this, queryUri, REQUEST_CODE_SENDER)

            //或者
//            val queryStatement = buildQuerySelectionStatement(
//                MEDIA_TYPE_IMAGE,
//                "BitmapImage (1).png",
//                null,
//                null,
//                null,
//                null,
//                true
//            )
//            val images = queryMediaStoreImages(null, null, queryStatement)
//            if (!images.isNullOrEmpty()) {
//                deleteUriMediaStoreImage(
//                    this, images[0],
//                    SENDER_REQUEST_CODE
//                )
//            }
        }

        //清空目录
        deleteFileAll.setOnClickListener {
            //删除结果 content://media/external/images/media 0
            //deleteUri(this, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, REQUEST_CODE_SENDER)
            //or
            deleteUriDirectory(this, REQUEST_CODE_SENDER, MEDIA_TYPE_IMAGE)

        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SENDER) {
            if (requestCode == Activity.RESULT_OK) {
                FileLogger.d("授权成功")
                //do something
            } else {
                FileLogger.d("授权失败")
            }

        }
    }
}