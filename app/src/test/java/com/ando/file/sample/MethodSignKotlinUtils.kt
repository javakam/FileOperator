package com.ando.file.sample

import ando.file.androidq.FileOperatorQ
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
    @JvmStatic
    fun main(args: Array<String>) {
        //printMethodSignForKotlin(TestReflect::class)
        printMethodSignForKotlin(FileOperatorQ::class)
    }

    private fun printMethodSignForKotlin(clazz: KClass<*>) {
        println("-------------------------------------------------------------")
        //获取该对象声明的全部方法
        //fun com.ando.file.sample.TestReflect.see(): kotlin.String 去掉包名简化 fun see(): kotlin.String
        val declaredFunctions = clazz.declaredFunctions
        declaredFunctions.forEach {
            val prefix = "${clazz.qualifiedName}."
            var func = "$it"
            func = func.replace(prefix, "")
            println(func)
        }
        println("-------------------------------------------------------------")
    }
}