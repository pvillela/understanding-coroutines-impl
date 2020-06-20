package coroutinesdesign

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine


typealias SmCont = (Any?) -> Unit

// Simplified version of launch based on KEEP document
// (https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/run/launch.kt).
// without structured coroutines support and does not return immediately, so it
// is not stack-safe.
fun runAsCoroutine(context: CoroutineContext = EmptyCoroutineContext, block: suspend () -> Unit) =
        block.startCoroutine(Continuation(context) { result ->
            result.onFailure { exception ->
//                val currentThread = Thread.currentThread()
//                currentThread.uncaughtExceptionHandler.uncaughtException(currentThread, exception)
                println(exception)
            }
        })
