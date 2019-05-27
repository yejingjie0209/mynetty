
package com.jason.nettydemo.utils

import android.util.Log
import kotlinx.coroutines.*

/**
 * Kotlin协程辅助类，同一封装协程，避免单独使用中的错误
 */
@Suppress("unused")
object CoroutinesUtils {

    /**
     * 捕获协程运行期间产生的错误，防止引起crash
     */
    private val handler = CoroutineExceptionHandler { _, exception ->
        Log.e("CoroutineException", "$exception")
    }

    /**
     * launch在IO线程中
     */
    fun launchInIO(block: suspend CoroutineScope.() -> Unit)
        = GlobalScope.launch(Dispatchers.IO.plus(handler), CoroutineStart.DEFAULT, block)

    fun launchInUI(block: suspend CoroutineScope.() -> Unit)
            = GlobalScope.launch(Dispatchers.Main.plus(handler), CoroutineStart.DEFAULT, block)

    fun launch(block: suspend CoroutineScope.() -> Unit)
            = GlobalScope.launch(Dispatchers.Default.plus(handler), CoroutineStart.DEFAULT, block)

    fun launchLazy(block: suspend CoroutineScope.() -> Unit)
            = GlobalScope.launch(Dispatchers.Default.plus(handler), CoroutineStart.LAZY, block)

    fun launchInIOLazy(block: suspend CoroutineScope.() -> Unit)
            = GlobalScope.launch(Dispatchers.IO.plus(handler), CoroutineStart.LAZY, block)

    fun launchInUILazy(block: suspend CoroutineScope.() -> Unit)
            = GlobalScope.launch(Dispatchers.Main.plus(handler), CoroutineStart.LAZY, block)

    fun <T> runBlocking(block: suspend CoroutineScope.() -> T)
            = kotlinx.coroutines.runBlocking(Dispatchers.Default.plus(handler), block)

    fun <T> runBlockingInUI(block: suspend CoroutineScope.() -> T)
            = kotlinx.coroutines.runBlocking(Dispatchers.Main.plus(handler), block)

    fun <T> runBlockingInIO(block: suspend CoroutineScope.() -> T)
            = kotlinx.coroutines.runBlocking(Dispatchers.IO.plus(handler), block)

    fun <T> async(block: suspend CoroutineScope.() -> T)
            = GlobalScope.async(Dispatchers.Default.plus(handler),CoroutineStart.DEFAULT,block)

    fun <T> asyncInIO(block: suspend CoroutineScope.() -> T)
            = GlobalScope.async(Dispatchers.IO.plus(handler),CoroutineStart.DEFAULT,block)

    fun <T> asyncLazy(block: suspend CoroutineScope.() -> T)
            = GlobalScope.async(Dispatchers.IO.plus(handler),CoroutineStart.LAZY, block)

    fun <T> asyncInIOLazy(block: suspend CoroutineScope.() -> T)
            = GlobalScope.async(Dispatchers.IO.plus(handler),CoroutineStart.LAZY, block)

    fun launchInNewScope(block: suspend CoroutineScope.() -> Unit): Job{
        val scope = CoroutineScope(Dispatchers.Default.plus(handler))
        return scope.launch(start = CoroutineStart.DEFAULT, block = block)
    }

    fun launchInNewIOScope(block: suspend CoroutineScope.() -> Unit): Job{
        val scope = CoroutineScope(Dispatchers.IO.plus(handler))
        return scope.launch(start = CoroutineStart.DEFAULT, block = block)
    }

    fun asyncInNewIOScope(block: suspend CoroutineScope.() -> Unit): Job{
        val scope = CoroutineScope(Dispatchers.IO.plus(handler))
        return scope.async(start = CoroutineStart.DEFAULT, block = block)
    }

}