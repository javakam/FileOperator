package com.ando.file.sample

/**
 * Title:
 *
 * @author javakam
 * @date 2021/1/19  15:50
 */
class TestReflect {
    init {
        val holiday: Int = 1992
    }

    companion object {
        const val DEFAULT_NAME = "Tony"
        lateinit var hobby: String

        fun sleep(): Boolean {
            return false
        }
    }

    constructor()

    constructor(name: String)

    private var title: String? = null

    val age: String by lazy { "18" }

    fun say(): Int {
        return 1
    }

    fun say(block: (text: String) -> Boolean) {}

    private fun see(): String {
        return ""
    }
}