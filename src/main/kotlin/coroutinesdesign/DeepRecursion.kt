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

    @Suppress("UNCHECKED_CAST")
    class DeepRecursiveScope<T, R>(
            block: suspend DeepRecursiveScope<T, R>.(T) -> R,
            value: T
    ) : Continuation<R> {

        /** Continuation-passing-style view of `block`. */
        private val function = block as Function3<Any?, Any?, Continuation<R>, Any?>

        /** Final result returned by a [DeepRecursiveFunction]. */
        private var result: Result<R> = Result.success(null) as Result<R>

        /** Argument of the current recursive call. */
        private var value: Any? = value

        /** Continuation used by current recursive call. */
        private var cont: Continuation<R>? = this

        override fun toString(): String {
            return "DeepRecursiveScope-" + super.toString()
        }

        /**
         * Sets [cont] to the current continuation and sets [value] to the passed-in argument
         * so that these fields can be used in the next invocation of [function] in [runCallLoop].
         * The current continuation is the state machine continuation of `block` at the point of
         * invocation of [callRecursive].
         * It returns [COROUTINE_SUSPENDED], so that [function] also returns [COROUTINE_SUSPENDED].
         */
        suspend fun callRecursive(value: T): R =
                suspendCoroutineUninterceptedOrReturn { cont ->
                    println("callRecursive: cont=$cont, value=$value")
                    this.cont = cont
                    this.value = value
                    COROUTINE_SUSPENDED
                }

        fun runCallLoop(): R {
            var i = 0
            while (true) {
                println("runCallLoop ${++i} -- top: value=$value, result=$result, cont=$cont")
                val cont = this.cont // null means done
                        ?: return result.getOrThrow()
                // ~startCoroutineUninterceptedOrReturn
                val r = try {
                    println("runCallLoop -- before function: value=$value, result=$result, cont=$cont")
                    // In the first loop iteration, `this` is passed as `cont`, so `this` is the
                    // completion continuation of the state machine of `block` when the state machine
                    // is instantiated here on that first call. On subsequent loop iterations, the
                    // state machine is passed as the `cont` argument and its state changes on each
                    // call. At completion, the state machine calls `this` with the value computed by
                    // `block`.
                    function(this, value, cont)
                } catch (e: Throwable) {
                    cont.resumeWithException(e)
                    continue
                }
                println("runCallLoop -- after function: r=$r, value=$value, result=$result, cont=$cont")
                if (r !== COROUTINE_SUSPENDED) {
                    println("runCallLoop -- before cont.resume: r=$r, value=$value, result=$result, cont=$cont")
                    cont.resume(r as R)
                    println("runCallLoop -- after cont.resume: r=$r, value=$value, result=$result, cont=$cont")
                }
            }
        }

        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(result: Result<R>) {
            cont = null
            this.result = result
        }
    }

    class DeepRecursiveFunction<T, R>(
            val block: suspend DeepRecursiveScope<T, R>.(T) -> R
    )

    operator fun <T, R> DeepRecursiveFunction<T, R>.invoke(value: T): R =
            DeepRecursiveScope<T, R>(block, value).runCallLoop()

    class Tree(val left: Tree?, val right: Tree?) {
        override fun toString(): String {
            val l = left?.run { "Tree@${this.hashCode()}" } ?: "null"
            val r = right?.run { "Tree@${this.hashCode()}" } ?: "null"
            return "Tree($l, $r)"
        }
    }

//        val n = 100_000
    val n = 1

    val deepTree = generateSequence(Tree(null, null)) { prev ->
        Tree(prev, null)
    }.take(n).last()

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