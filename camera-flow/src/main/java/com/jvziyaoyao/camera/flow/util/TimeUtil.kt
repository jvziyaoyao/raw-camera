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
fun testTime(block: () -> Unit): Long {
    val t0 = System.currentTimeMillis()
    block.invoke()
    val t1 = System.currentTimeMillis()
    return t1 - t0
}