package com.ando.file.sample.utils

import ando.file.androidq.FileOperatorQ
import ando.file.core.FileOpener
import ando.file.core.FileUri
import ando.file.core.FileUtils
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.*

/**
 * # 获取Kotlin类的所有方法签名
 *
 * - 需要依赖: implementation "org.jetbrains.kotlin:kotlin-reflect:1.4.21"
 *
 * 1. 获取Java类的所有方法签名 https://blog.csdn.net/weixin_38106322/article/details/108218774
 *
 * 2. Kotlin反射 https://www.jianshu.com/p/63da6197913b
 */
object MethodSignKotlinUtils {

    fun dumpMethods(context: Context) {
        //不带弹窗
        //dumpMethodSignInfo(context, FileOperatorQ::class)

        //带弹窗
        //dumpMethodSignInfoWithUi(context,FileOperatorQ::class)
    }

    private fun dumpMethodSignInfo(context: Context, clazz: KClass<*>) {
        val handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message?) {
                super.handleMessage(msg)
                shareFile(context, printMethodSignForKotlin(clazz), "MethodInfo_${clazz.simpleName}.txt")
            }
        }
        Thread {
            SystemClock.sleep(500)
            handler.sendEmptyMessage(0)
        }.start()
    }

    private fun dumpMethodSignInfoWithUi(context: Context, clazz: KClass<*>) {
        showOptionsDialog(context, printMethodSignForKotlin(clazz), "MethodInfo_${clazz.simpleName}.txt")
    }

    fun printMethodSignForKotlin(clazz: KClass<*>): String {
        println("-------------------------------------------------------------")
        //获取该对象声明的全部方法
        //fun com.ando.file.sample.TestReflect.see(): kotlin.String 去掉包名简化 fun see(): kotlin.String
        val sb = StringBuilder("\n")
        val declaredFunctions = clazz.declaredFunctions
        declaredFunctions.forEach {
            val prefix = "${clazz.qualifiedName}."
            var func = "$it"
            func = func.replace(prefix, "")
            sb.append(func).append("\n")
            //println(func)
        }
        //println(sb.toString())
        println("-------------------------------------------------------------")
        return sb.toString()
    }

    private fun showOptionsDialog(context: Context, info: String, fileName: String) {
        AlertDialog.Builder(context)
            .setMessage(info)
            .setCancelable(false)
            .setNegativeButton("关闭") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            .setPositiveButton("分享") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                shareFile(context, info, fileName)
            }
            .setNeutralButton("复制") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                copyToClipBoard(context, info)
                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
            }
            .show()

    }

    private fun shareFile(context: Context, info: String, fileName: String) {
        val filePath = context.externalCacheDir.path
        //val filePathAndName="$filePath${File.separator}$fileName"
        val file = File(filePath, fileName)
        FileUtils.write2File(info.byteInputStream(Charsets.UTF_8), file.path)
        FileUri.getUriByFile(file)?.apply {
            FileOpener.openShare(context, this)
        }
    }

    private fun copyToClipBoard(context: Context, text: String) {
        val cm: ClipboardManager? = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager?
        if (cm != null) {
            //参数一：标签，可为空，参数二：要复制到剪贴板的文本
            cm.primaryClip = ClipData.newPlainText(null, text)
            if (cm.hasPrimaryClip()) {
                cm.primaryClip?.getItemAt(0)?.text
            }
        }
    }

}