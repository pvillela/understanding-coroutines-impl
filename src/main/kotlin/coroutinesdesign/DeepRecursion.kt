package coroutinesdesign

import coroutinesdesign.DeepRecursionCommon.Tree
import coroutinesdesign.DeepRecursionCommon.deepTree
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
    
    fun <T> Continuation<T>.show(): String = "${this.hashCode()}-$this"

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
        private var stateMachine: Continuation<R>? = null

        /** Loop control. */
        private var completed = false

        /**
         * Sets [stateMachine] to the current continuation and sets [value] to the passed-in argument
         * so that these fields can be used in the next invocation of [function] in [runCallLoop].
         * The current continuation is the state machine continuation of `block` at the point of
         * invocation of [callRecursive].
         * It returns [COROUTINE_SUSPENDED], so that [function] also returns [COROUTINE_SUSPENDED].
         */
        suspend fun callRecursive(value: T): R =
                suspendCoroutineUninterceptedOrReturn { cont ->
                    println("callRecursive: stateMachine=${cont.show()}, value=$value")
                    if (this.stateMachine != cont)
                        println("*** stateMachine change from ${stateMachine?.show()} to ${cont.show()}")
                    this.stateMachine = cont
                    this.value = value
                    COROUTINE_SUSPENDED
                }

        fun runCallLoop(): R {
            var i = 0
            while (!completed) {
                println("runCallLoop ${++i} -- top: value=$value, result=$result, stateMachine=${stateMachine?.show()}")
                // ~startCoroutineUninterceptedOrReturn
                val cont = stateMachine ?: this
                val r = try {
                    println("runCallLoop -- before function: value=$value, result=$result, cont=${cont.show()}")
                    // In the first loop iteration, `this` is passed as `cont`, so `this` is the
                    // completion continuation of the state machine of `block` when the state machine
                    // is instantiated here on that first call. On subsequent loop iterations, the
                    // state machine is passed as the `cont` argument and its state changes on each
                    // call. At completion, the state machine calls its cocmpletion continuation `this`
                    // with the value computed by `block`.
                    function(this, value, cont)
                } catch (e: Throwable) {
                    cont.resumeWithException(e)
                    continue
                }
                println("runCallLoop -- after function: r=$r, value=$value, result=$result, stateMachine=${stateMachine?.show()}")
                if (r !== COROUTINE_SUSPENDED) {
                    println("runCallLoop -- before stateMachine.resume: r=$r, value=$value, result=$result, stateMachine=${cont.show()}")
                    cont.resume(r as R)
                    println("runCallLoop -- after stateMachine.resume: r=$r, value=$value, result=$result, stateMachine=${cont.show()}")
                }
            }
            return result.getOrThrow()
        }

        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(result: Result<R>) {
            completed = true
            this.result = result
        }
    }

    class DeepRecursiveFunction<T, R>(
            val block: suspend DeepRecursiveScope<T, R>.(T) -> R
    )

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
//        val n = 100_000
        val n = 2
        println(depth(deepTree(n)))
    }
}
