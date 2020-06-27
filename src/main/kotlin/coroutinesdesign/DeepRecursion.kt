package coroutinesdesign

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Based on https://medium.com/@elizarov/deep-recursion-with-coroutines-7c53e15993e3
 */
object DeepRecursion {

    class Tree(val left: Tree?, val right: Tree?) {
        override fun toString(): String {
            val l = left?.run { "Tree@${this.hashCode()}" } ?: "null"
            val r = right?.run { "Tree@${this.hashCode()}" } ?: "null"
            return "Tree($l, $r)"
        }
    }

//    val n = 100_000
    val n = 1

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

        override fun toString(): String {
            return "DeepRecursiveScope-" + super.toString()
        }

        suspend fun callRecursive(value: T): R =
                suspendCoroutineUninterceptedOrReturn { cont ->
                    println("callRecursive: cont=$cont, value=$value")
                    this.cont = cont
                    this.value = value
                    COROUTINE_SUSPENDED
                }

        fun runCallLoop(): R {
            while (true) {
                println("runCallLoop -- top: this.value=${this.value}, this.result=${this.result}, this.cont=${this.cont}")
                val result = this.result
                val cont = this.cont // null means done
                        ?: return result.getOrThrow()
                // ~startCoroutineUninterceptedOrReturn
                val r = try {
                    println("runCallLoop -- before function: this.value=${this.value}, result=$result, cont=$cont")
                    function(this, value, cont)
                } catch (e: Throwable) {
                    cont.resumeWithException(e)
                    continue
                }
                println("runCallLoop -- after function: r=$r, this.value=${this.value}, this.result=${this.result}, this.cont=${this.cont}")
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
        println("DeepRecursiveFunction -- entry: t=$t")
        if (t == null) 0 else maxOf(
                run {
                    println("DeepRecursiveFunction -- before callRecursive(t.left): t=$t")
                    val resL = callRecursive(t.left)
                    println("DeepRecursiveFunction -- after callRecursive(t.left): t=$t")
                    resL
                },
                run {
                    println("DeepRecursiveFunction -- before callRecursive(t.right): t=$t")
                    val resR = callRecursive(t.right)
                    println("DeepRecursiveFunction -- after callRecursive(t.right): t=$t")
                    resR
                }
        ) + 1
    }

    @JvmStatic
    fun main(args: Array<String>) {
        println(depth(deepTree))
    }
}