package timings

import coroutinesdesign.DeepRecursionOriginal.DeepRecursiveFunction
import coroutinesdesign.DeepRecursionOriginal.invoke
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import trampoline.Trampoline
import trampoline.delay
import trampoline.done
import trampoline.run
import kotlin.system.measureTimeMillis


fun fibLoop(n: Long): Long {
    var n1 = 1L
    var n2 = 0L
    var res = n1 + n2
    for (i in 1 until n - 1) {
        n2 = n1
        n1 = res
        res = n1 + n2
    }
    return res
}

fun fibRecursive(n: Long): Long =
        if (n <= 1) {
            n
        } else {
            val n1 = fibRecursive(n - 1)
            val n2 = fibRecursive(n - 2)
            n1 + n2
        }

fun fibTrampoline(n: Long): Trampoline<Long> =
        if (n <= 1)
            done(n)
        else delay { fibTrampoline(n - 1) }.flatMap { n1 ->
            fibTrampoline(n - 2).flatMap { n2 ->
                done(n1 + n2)
            }
        }

val fibDeepRecursive = DeepRecursiveFunction<Long, Long> { n ->
    if (n <= 1) {
        n
    } else {
        val n1 = callRecursive(n - 1)
        val n2 = callRecursive(n - 2)
        n1 + n2
    }
}

fun fibRecursiveWithYield(n: Long): Long = runBlocking {
    suspend fun fibRecursiveWithYield0(n: Long): Long =
            if (n <= 1) {
                n
            } else {
                yield()
                val n1 = fibRecursive(n - 1)
                yield()
                val n2 = fibRecursive(n - 2)
                n1 + n2
            }
    fibRecursiveWithYield0(n)
}


fun main() {
    val n = 40L

    println("n = $n")

    println("fibLoop")
    val fibLoopTime = measureTimeMillis {
        println(fibLoop(n))
    }
    println(fibLoopTime)

    println("fibRecursive")
    val fibRecursiveTime = measureTimeMillis {
        try {
            println(fibRecursive(n))
        } catch (e: StackOverflowError) {
            println(e)
        }
    }
    println(fibRecursiveTime)

    println("fibRecursiveWithYield")
    val fibRecursiveWithYieldTime = measureTimeMillis {
        println(fibRecursiveWithYield(n))
    }
    println(fibRecursiveWithYieldTime)

    println("fibDeepRecursive")
    val fibDeepRecursiveTime = measureTimeMillis {
        println(fibDeepRecursive(n))
    }
    println(fibDeepRecursiveTime)

    println("fibTrampoline")
    val fibTrampolineTime = measureTimeMillis {
        println(fibTrampoline(n).run())
    }
    println(fibTrampolineTime)
}