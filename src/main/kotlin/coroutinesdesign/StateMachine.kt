package coroutinesdesign

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine


object StateMachine {

    fun f1(x: Int): Int = x + 1

    fun f2(x: Int): Int = x * 10

    fun f(x: Int): Int {
        val y = x + 10
        val z = f1(y)
        val u = z + 2
        val v = f2(u)
        val w = v + 3
        return w
    }

    fun f1Cps(x: Int, cont: SmCont) {
        cont(x + 1)
    }

    fun f2Cps(x: Int, cont: SmCont) {
        cont(x * 10)
    }

    // Creates a new state machine lambda at each step. Deep stack.
    fun fCpsSm0(x: Int, cont: SmCont) {
        val y = x + 10
        fun sm(label: Int): SmCont = { input: Any? ->
            when (label) {
                0 -> {
                    f1Cps(y, sm(label + 1))
                }
                1 -> {
                    val z = input as Int
                    val u = z + 2
                    f2Cps(u, sm(label + 1))
                }
                2 -> {
                    val v = input as Int
                    val w = v + 3
                    cont(w)
                }
            }
        }
        sm(0)(null)
    }

    // Reuses the same state machine instance at each step. Deep stack.
    fun fCpsSm(x: Int, cont: SmCont) {
        var label: Int = 0
        val y = x + 10
        val sm = object : SmCont {
            override fun invoke(input: Any?) {
                when (label) {
                    0 -> {
                        ++label
                        f1Cps(y, this)
                    }
                    1 -> {
                        val z = input as Int
                        val u = z + 2
                        ++label
                        f2Cps(u, this)
                    }
                    2 -> {
                        val v = input as Int
                        val w = v + 3
                        cont(w)
                    }
                }
            }
        }
        sm(null)
    }

    // Reuses the same state machine instance at each step. Shallow stack.
    fun CoroutineScope.fCpsSmLaunch(x: Int, cont: SmCont) {
        val scope = this
        var label: Int = 0
        val sm = object : SmCont {
            override fun invoke(input: Any?) {
                val self = this
                when (label) {
                    0 -> {
                        val y = x + 10
                        ++label
                        launch { f1Cps(y, self) }
                    }
                    1 -> {
                        val z = input as Int
                        val u = z + 2
                        ++label
                        launch { f2Cps(u, self) }
                    }
                    2 -> {
                        val v = input as Int
                        val w = v + 3
                        cont(w)
                    }
                }
            }
        }
        sm(null)
    }

    fun fCpsSmCoroutineEmptyCoroutine(context: CoroutineContext, x: Int, cont: SmCont) {
        var label: Int = 0
        val sm = object : SmCont {
            override fun invoke(input: Any?) {
                val self = this
                when (label) {
                    0 -> {
                        val y = x + 10
                        ++label
                        runAsCoroutine { f1Cps(y, self) }
                    }
                    1 -> {
                        val z = input as Int
                        val u = z + 2
                        ++label
                        runAsCoroutine { f2Cps(u, self) }
                    }
                    2 -> {
                        val v = input as Int
                        val w = v + 3
                        cont(w)
                    }
                }
            }
        }
        sm(null)
    }

    fun fCpsSmCoroutine(context: CoroutineContext, x: Int, cont: SmCont) {
        var label: Int = 0
        val sm = object : SmCont {
            override fun invoke(input: Any?) {
                val self = this
                when (label) {
                    0 -> {
                        val y = x + 10
                        ++label
                        runAsCoroutine(context) { f1Cps(y, self) }
                    }
                    1 -> {
                        val z = input as Int
                        val u = z + 2
                        ++label
                        runAsCoroutine(context) { f2Cps(u, self) }
                    }
                    2 -> {
                        val v = input as Int
                        val w = v + 3
                        cont(w)
                    }
                }
            }
        }
        sm(null)
    }

    val finalCont: SmCont = { x ->
        println(x)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        println("Direct style")
        println(f(1))

        println("fCpsSm0: state machine")
        fCpsSm0(1, finalCont)

        println("fCpsSm: state machine")
        fCpsSm(1, finalCont)

        println("fCpsSmLaunch: state machine with launch")
        runBlocking { fCpsSmLaunch(1, finalCont) }

        println("fCpsSmCoroutine: state machine with runAsCoroutine -- EmptyCoroutineContext")
        runBlocking {
            val context = EmptyCoroutineContext
            fCpsSmCoroutine(context, 1, finalCont)
        }
    }
}
