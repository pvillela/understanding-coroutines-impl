package coroutinesdesign

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


/**
 * Demonstration of continuation passing style with state machines when there are loops involved.
 */
object StateMachineWithLoop {
    fun f1(x: Int): Int = x + 1

    fun f2(x: Int): Int = x * 10

    fun f(x: Int): Int {
        val y = x + 10
        var z0 = 0
        for (i in 1..10_000) {
            z0 = f1(y + z0)
        }
        val z = z0
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

    // Reuses the same state machine instance at each step. Deep stack.
    fun fCpsSm(x: Int, cont: SmCont) {
        var label = 0
        val y = x + 10
        var i0 = 0
        val sm = object : SmCont {
            override fun invoke(input: Any?) {
                when (label) {
                    0 -> {
                        val z0 = input as Int
                        ++i0
                        if (i0 == 10_000) ++label
                        f1Cps(y + z0, this)
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
        sm(0)
    }

    // Reuses the same state machine instance at each step. Shallow stack.
    fun CoroutineScope.fCpsSmLaunch(x: Int, cont: SmCont) {
        var label = 0
        var i0 = 0
        val sm = object : SmCont {
            override fun invoke(input: Any?) {
                val self = this
                when (label) {
                    0 -> {
                        val y = x + 10
                        val z0 = input as Int
                        ++i0
                        if (i0 == 10_000) ++label
                        launch { f1Cps(y + z0, self) }
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
        sm(0)
    }

    fun fCpsSmCoroutine(context: CoroutineContext, x: Int, cont: SmCont) {
        var label = 0
        var i0 = 0
        val sm = object : SmCont {
            override fun invoke(input: Any?) {
                val self = this
                when (label) {
                    0 -> {
                        val y = x + 10
                        val z0 = input as Int
                        ++i0
                        if (i0 == 10_000) ++label
                        runAsCoroutine(context) { f1Cps(y + z0, self) }
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
        sm(0)
    }

    val finalCont: SmCont = { x ->
        println(x)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        println("Direct style")
        println(f(1))

        println("fCpsSm: state machcine")
        try {
            fCpsSm(1, finalCont)
        } catch (e: StackOverflowError) {
            println(e)
        }

        println("fCpsSmLaunch: state machine with launch -- runBlocking")
        runBlocking { fCpsSmLaunch(1, finalCont) }

        val pause = 200L

        println("fCpsSmLaunch: state machine with launch -- CoroutineScope(EmptyCoroutineContext)")
        with(CoroutineScope(EmptyCoroutineContext)) { fCpsSmLaunch(1, finalCont) }
        Thread.sleep(pause)

        println("fCpsSmCoroutine: state machine with runAsCoroutine -- runBlocking coroutineContext")
        runBlocking {
            val context = this.coroutineContext
            fCpsSmCoroutine(context, 1, finalCont)
        }
        Thread.sleep(pause)

        println("fCpsSmCoroutine: state machine with runAsCoroutine -- run EmptyCoroutineContext")
        run {
            val context = EmptyCoroutineContext
            fCpsSmCoroutine(context, 1, finalCont)
        }
        Thread.sleep(pause)

        println("fCpsSmCoroutine: state machine with runAsCoroutine -- run Dispatchers.Default")
        run {
            val context = Dispatchers.Default
            fCpsSmCoroutine(context, 1, finalCont)
        }
        Thread.sleep(pause)

        println("fCpsSmCoroutine: state machine with runAsCoroutine -- GlobalScope")
        with(GlobalScope) {
            val context = this.coroutineContext
            fCpsSmCoroutine(context, 1, finalCont)
        }
        Thread.sleep(pause)

        println("fCpsSmCoroutine: state machine with runAsCoroutine -- GlobalScope + Dispatchers.Default")
        with(GlobalScope) {
            val context = this.coroutineContext + Dispatchers.Default
            fCpsSmCoroutine(context, 1, finalCont)
        }
        Thread.sleep(pause)

        println("fCpsSmCoroutine: state machine with runAsCoroutine -- CoroutineScope(EmptyCoroutineContext)")
        with(CoroutineScope(EmptyCoroutineContext)) {
            val context = this.coroutineContext
            fCpsSmCoroutine(context, 1, finalCont)
        }
        Thread.sleep(pause)

        println("fCpsSmCoroutine: state machine with runAsCoroutine -- CoroutineScope(Dispatchers.Default)")
        with(CoroutineScope(Dispatchers.Default)) {
            val context = this.coroutineContext
            fCpsSmCoroutine(context, 1, finalCont)
        }
        Thread.sleep(pause)
    }
}
