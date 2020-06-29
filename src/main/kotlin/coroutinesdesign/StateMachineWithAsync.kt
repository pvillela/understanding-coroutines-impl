package coroutinesdesign

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ArrayBlockingQueue


/**
 * Demonstration of implementation of Deferred using launch and continuation passing style.
 */
object StateMachineWithAsync {

    class MyDeferredB<T> {
        private var hasValue = false
        private var value: T? = null
        private val queue = ArrayBlockingQueue<T>(1)

        fun set(value: T) { queue.put(value) }

        fun await(): T {
            synchronized(this) {
                if (!hasValue) {
                    value = queue.take()
                    hasValue = true
                }
                return value as T
            }
        }

        fun awaitCps(cont: SmCont) {
            cont(await())
        }
    }

    fun <T> CoroutineScope.myDeferredBOf(block: () -> T): MyDeferredB<T> {
        val deferred = MyDeferredB<T>()
        launch { deferred.set(block()) }
        return deferred
    }

    class MyDeferredS<T> {
        private var hasValue = false
        private var value: T? = null
        private val queue = Channel<T>(1)
        private val mutex = Mutex()

        suspend fun set(value: T) { queue.send(value) }

        suspend fun await(): T {
            mutex.withLock {
                if (!hasValue) {
                    value = queue.receive()
                    hasValue = true
                }
                return value as T
            }
        }

        suspend fun awaitCps(cont: SmCont) {
            cont(await())
        }
    }

    fun <T> CoroutineScope.myDeferredSOf(block: () -> T): MyDeferredS<T> {
        val deferred = MyDeferredS<T>()
        launch { deferred.set(block()) }
        return deferred
    }

    fun f1(x: Int): Int = x + 1

    fun CoroutineScope.f1DB(x: Int): MyDeferredB<Int> = myDeferredBOf { f1(x) }

    fun CoroutineScope.f1DS(x: Int): MyDeferredS<Int> = myDeferredSOf { f1(x) }

    fun f2(x: Int): Int = x * 10

    fun f2Cps(x: Int, cont: SmCont) {
        cont(x * 10)
    }

    fun CoroutineScope.fB(x: Int): Int {
        val y = x + 10
        val dz = f1DB(y)
        val u = dz.await() + 2
        val v = f2(u)
        val w = v + 3
        return w
    }

    suspend fun fS(x: Int): Int = coroutineScope {
        val y = x + 10
        val dz = f1DS(y)
        val u = dz.await() + 2
        val v = f2(u)
        val w = v + 3
        w
    }

    fun CoroutineScope.fBCpsSmLaunch(x: Int, cont: SmCont) {
        var label: Int = 0
        val sm = object : SmCont {
            override fun invoke(input: Any?) {
                val self = this
                when (label) {
                    0 -> {
                        val y = x + 10
                        ++label
                        val dz = f1DB(y)
                        launch { dz.awaitCps(self) }
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
                        launch { cont(w) }
                    }
                }
            }
        }
        sm(null)
    }

    fun CoroutineScope.fSCpsSmLaunch(x: Int, cont: SmCont) {
        var label: Int = 0
        val sm = object : SmCont {
            override fun invoke(input: Any?) {
                val self = this
                when (label) {
                    0 -> {
                        val y = x + 10
                        ++label
                        val dz = f1DS(y)
                        launch { dz.awaitCps(self) }
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
                        launch { cont(w) }
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
        println("fB: Direct style -- MyDeferredB")
        runBlocking(Dispatchers.Default) { println(fB(1)) }

        println("fS: Direct style -- MyDeferredS")
        runBlocking(Dispatchers.Default) { println(fS(1)) }

        println("fBCpsSmLaunch: state machine with launch -- MyDeferredB")
        runBlocking { fBCpsSmLaunch(1, finalCont) }

        println("fSCpsSmLaunch: state machine with launch -- MyDeferredS")
        runBlocking { fSCpsSmLaunch(1, finalCont) }
    }
}
