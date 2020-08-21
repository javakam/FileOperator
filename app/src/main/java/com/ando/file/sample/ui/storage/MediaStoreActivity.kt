package com.ando.file.sample.ui.storage

import ando.file.androidq.*
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.DialogInterface
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
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ando.file.core.*
import com.ando.file.sample.R
import com.ando.file.sample.utils.PermissionManager
import com.ando.file.sample.utils.PermissionManager.havePermissions
import com.ando.file.sample.REQUEST_CODE_SENDER
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_media_store.*
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

/**
 * Title: MediaStoreActivity
 * <p>
 * Description: 沙盒 -> APP卸载,数据删除
 * </p>
 * @author javakam
 * @date 2020/6/2  15:12
 */
class MediaStoreActivity : AppCompatActivity() {

    companion object {

        val RELATIVE_PATH = "${Environment.DIRECTORY_PICTURES}${File.separator}img"
    }

    private var mInsertUri: Uri? = null
    private var mQueryUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_store)

        title = "MediaStore"

        tvMediaStoreTip.text =
            """
                    👉 1.增删不需要权限,读取文件需要READ_EXTERNAL_STORAGE权限
                    👉 2.系统只提供了多媒体文件的读权限，没有提供写权限，应用无法直接通过申请写权限修改其他应用生成的文件 ,
                      如果需要 修改/删除 其他应用文件需要捕获 RecoverableSecurityException 进一步处理。 
                """.trimIndent()

        //1.MediaStore 创建文件
        insertBitmapToPictures.setOnClickListener {
            insertBitmapToPictures()
        }
        //2.MediaStore 查询文件
        queryFileByMediaStore()
        //3.MediaStore 修改文件
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            updateFileByMediaStore()
        }
        //4.MediaStore 删除文件
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            deleteFileByMediaStore()
        }
    }

    /**
     * 使用MediaStore创建文件
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
        //创建了一个红色的图片
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
     * 通过MediaStore查询文件
     */
    private fun queryFileByMediaStore() {

        queryFileByDisplayName.setOnClickListener {

            //通过 DisplayName 查询图片
            //1.读取文件需要权限 READ_EXTERNAL_STORAGE
            //2.系统只提供了多媒体文件的读权限，没有提供写权限，应用无法通过申请写权限修改其他应用生成的文件
            if (!havePermissions(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                PermissionManager.verifyStoragePermissions(this)
                return@setOnClickListener
            } else {
                mQueryUri = queryMediaStoreImages(
                    "BitmapImage.png", true
                )
            }

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
        }

        //查询全部
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
            val view = layoutInflater.inflate(R.layout.layout_gallery, parent, false)
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
            if (havePermissions(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
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
                FileLogger.d("没有READ_EXTERNAL_STORAGE权限，请动态申请")
                PermissionManager.verifyStoragePermissions(this)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PermissionManager.REQUEST_EXTERNAL_STORAGE -> {

                PermissionManager.handleRequestPermissionsResult(
                    this,
                    permissions,
                    grantResults
                ) { result: Boolean, showRationale: Boolean ->
                    if (result) {
                        Toast.makeText(this, "申请权限成功!", Toast.LENGTH_SHORT).show()
                    } else {
                        FileLogger.w("showRationale =$showRationale ")


                        if (showRationale) {
                            //无权限
                            Toast.makeText(this, "申请权限失败!", Toast.LENGTH_SHORT).show()
                        } else {
                            //用户点了禁止获取权限，并勾选不再提示
                            //Toast.makeText(this, "请申请存储权限!", Toast.LENGTH_LONG).show()
                            //PermissionManager.goToSettings(this)

                            //or 弹窗提示更友好
                            showRequestPermissionSystem()
                        }
                    }
                }
            }
        }
    }

    private fun showRequestPermissionSystem() {
        /*
          Caused by: java.lang.IllegalArgumentException: com.google.android.material.dialog.MaterialAlertDialogBuilder requires a value for the com.ando.file.sample:attr/colorSurface attribute to be set in your app theme. You can either set the attribute in your theme or update your theme to inherit from Theme.MaterialComponents (or a descendant).
        at com.google.android.material.resources.MaterialAttributes.resolveOrThrow(MaterialAttributes.java:69)
        at com.google.android.material.color.MaterialColors.getColor(MaterialColors.java:64)
        at com.google.android.material.dialog.MaterialAlertDialogBuilder.<init>(MaterialAlertDialogBuilder.java:120)
        at com.google.android.material.dialog.MaterialAlertDialogBuilder.<init>(MaterialAlertDialogBuilder.java:103)
        at com.ando.file.sample.ui.storage.MediaStoreActivity.showRequestPermissionSystem(MediaStoreActivity.kt:273)

        Fixed: parent="Theme.MaterialComponents.DayNight.DarkActionBar">
         */
        MaterialAlertDialogBuilder(this)
            .setTitle("是否去系统页面申请存储权限？")
            .setPositiveButton("确定") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                PermissionManager.goToSettings(this)
            }
            .setNegativeButton("取消") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            .show()
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