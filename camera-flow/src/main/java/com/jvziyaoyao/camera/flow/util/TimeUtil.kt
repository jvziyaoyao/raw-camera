package com.jvziyaoyao.camera.flow.util

/**
 * @program: TestFocusable
 *
 * @description:
 *
 * @author: JVZIYAOYAO
 *
 * @create: 2024-02-25 15:51
 **/

inline fun testTime(block: () -> Unit): Long {
    val t0 = System.currentTimeMillis()
    block.invoke()
    val t1 = System.currentTimeMillis()
    return t1 - t0
}

inline fun <T> testTime(block: () -> T): Pair<T, Long> {
    val t0 = System.currentTimeMillis()
    val result = block.invoke()
    val t1 = System.currentTimeMillis()
    val delta = t1 - t0
    return Pair(result, delta)
}