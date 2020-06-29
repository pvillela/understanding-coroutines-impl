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
 * Based on https://medium.com/@elizarov/deep-recursion-with-coroutines-7c53e15993e3.
 * See DeepRecursion_Manual_Execution_n_2.txt for a step-by-step manual execution of [main]
 * in the case n = 2.
 */
object DeepRecursion {

    fun <T> Continuation<T>.show(): String =
            if (this is WrappedContinuation<*>) "[W${this.hashCode()}]-[${this.cont.hashCode()}]-${this.cont}"
            else "[${this.hashCode()}]-$this"

    class WrappedContinuation<T>(val cont: Continuation<T>) : Continuation<T> {
        override val context: CoroutineContext
            get() = cont.context

        override fun resumeWith(result: Result<T>) {
            println("=== Resumed continuation: result=$result, cont=${cont.show()}")
            cont.resumeWith(result)
        }
    }

    fun <T> wrappedContinuation(cont: Continuation<T>): Continuation<T> =
            if (cont is WrappedContinuation<T>) cont
            else WrappedContinuation(cont)

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

        /** Current state of continuation stack -- see [runCallLoop]. */
        private var cont: Continuation<R> = wrappedContinuation(this)

        /** Loop control. */
        private var completed = false

        /**
         * Sets [cont] to the current continuation and sets [value] to the passed-in argument
         * so that these fields can be used in the next invocation of [function] in [runCallLoop].
         * The current continuation is the state machine continuation of `block` at the point of
         * invocation of [callRecursive].
         * It returns [COROUTINE_SUSPENDED], so that [function] also returns [COROUTINE_SUSPENDED].
         */
        suspend fun callRecursive(value: T): R =
                suspendCoroutineUninterceptedOrReturn { cont ->
                    println("callRecursive: cont=${cont.show()}, value=$value")
                    val thisCont = this.cont
                    val cont0 =
                            if (thisCont is WrappedContinuation<*>) thisCont.cont
                            else thisCont
                    if (cont0 != cont) {
                        println("*** cont change from ${cont0.show()} to ${cont.show()}")
                        this.cont = wrappedContinuation(cont)
                    }
                    this.value = value
                    COROUTINE_SUSPENDED
                }

        /**
         * Each continuation can be viewed as a stack of continuations. The initial stack has
         * a single element, `this`, which is passed as the `cont` argument on the invocation
         * of [function] on the first iteration of the loop. If a continuation is a
         * state machine SM with completion continuation CC then the top of that stack is SM
         * and the next element is CC. If CC is a state machine SM2 with completion continuation
         * CC2 then the third element of the stack is CC2. And so on. [cont] holds the current
         * continuation stack for the algorithm.
         *
         * An invocation of [function] that results in a call to [callRecursive] pushes the state
         * machine SM instantiated in that invocation onto the stack in the sense that [cont] is
         * SM's completion continuation and [cont] is reassigned to have SM as its value.
         * Such an invocation returns COROUTINE_SUSPENDED, which is returned by [callRecursive],
         * and then the next iteration of the loop ensues.
         *
         * An invocation of [function] that does not involve a call to [callRecursive] returns a
         * value other than COROUTINE_SUSPENDED. In this case, the invocation is followed
         * by the resumption of [cont] with the value returned by [function]. If [cont] is a
         * state machine SM with completion continuation CC then [cont]'s resumption may be of
         * one of two kinds. Kind 1 -- SM transitions to a new state where [callRecursive] is
         * called, in which case the stack remains the same, although the SM object has
         * internally changed state. Kind 2 -- SM transitions to its completion, in which case
         * CC is resumed with the resulting value produced by SM, and we can recursively have
         * either a Kind 1 or Kind 2 scenario. In any case, the stack [cont] either remains
         * the same or it is popped one or more times.
         *
         * The algorithm completes when [function] returns a value other than COROUTINE_SUSPENDED
         * and [cont] is `this` or, more likely, when in scenario of Kind 2 above, CC is `this`.
         */
        fun runCallLoop(): R {
            var i = 0
            while (!completed) {
                println("runCallLoop ${++i} -- top: value=$value, result=$result, cont=${cont.show()}")
                val r = try {
                    println("runCallLoop -- before function: value=$value, result=$result, cont=${cont.show()}")
                    function(this, value, cont)
                } catch (e: Throwable) {
                    cont.resumeWithException(e)
                    continue
                }
                println("runCallLoop -- after function: r=$r, value=$value, result=$result, cont=${cont.show()}")
                if (r !== COROUTINE_SUSPENDED) {
                    println("runCallLoop -- before cont.resume: r=$r, value=$value, result=$result, cont=${cont.show()}")
                    cont.resume(r as R)
                    println("runCallLoop -- after cont.resume: r=$r, value=$value, result=$result, cont=${cont.show()}")
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
