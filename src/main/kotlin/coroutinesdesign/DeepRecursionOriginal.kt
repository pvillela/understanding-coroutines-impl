package coroutinesdesign

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.system.measureTimeMillis


/**
 * Original code from https://medium.com/@elizarov/deep-recursion-with-coroutines-7c53e15993e3.
 */
object DeepRecursionOriginal {

    class Tree(val left: Tree?, val right: Tree?)

    val n = 100_000

    val deepTree = generateSequence(Tree(null, null)) { prev ->
        Tree(prev, null)
    }.take(n).last()

    class DeepRecursiveFunction<T, R>(
            val block: suspend DeepRecursiveScope<T, R>.(T) -> R
    )

    @Suppress("UNCHECKED_CAST")
    class DeepRecursiveScope<T, R>(
            block: suspend DeepRecursiveScope<T, R>.(T) -> R,
            value: T
    ) : Continuation<R> {
        private val function = block as Function3<Any?, Any?, Continuation<R>, Any?>
        private var result: Result<R> = Result.success(null) as Result<R>
        private var value: Any? = value
        private var cont: Continuation<R>? = this

        suspend fun callRecursive(value: T): R =
                suspendCoroutineUninterceptedOrReturn { cont ->
                    this.cont = cont
                    this.value = value
                    COROUTINE_SUSPENDED
                }

        fun runCallLoop(): R {
            while (true) {
                val result = this.result
                val cont = this.cont // null means done
                        ?: return result.getOrThrow()
                // ~startCoroutineUninterceptedOrReturn
                val r = try {
                    function(this, value, cont)
                } catch (e: Throwable) {
                    cont.resumeWithException(e)
                    continue
                }
                if (r !== COROUTINE_SUSPENDED)
                    cont.resume(r as R)
            }
        }

        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(result: Result<R>) {
            this.cont = null
            this.result = result
        }
    }

    operator fun <T, R> DeepRecursiveFunction<T, R>.invoke(value: T): R =
            DeepRecursiveScope<T, R>(block, value).runCallLoop()

    val depth = DeepRecursiveFunction<Tree?, Int> { t ->
        if (t == null) 0 else maxOf(
                callRecursive(t.left),
                callRecursive(t.right)
        ) + 1
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val time = measureTimeMillis {
            println(depth(deepTree))
        }
        println("computation time in milliseconds: $time")
    }
}
