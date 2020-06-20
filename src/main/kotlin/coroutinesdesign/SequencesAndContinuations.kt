package coroutinesdesign

import kotlinx.coroutines.runBlocking


/**
 * Examples of implementation of Sequence-like functionality using continuations.
 */
object SequencesAndContinuations {
    val fooSeq = sequence<Int> {
        var i = 0
        while (true) {
            ++i
            yield(i * i)
        }
    }

    val fooStatefulCont: () -> Int = run {
        var i = 0
        val cont = {
            ++i
            i * i
        }
        cont
    }

    val fibSeq = sequence {
        yield(1) // first Fibonacci number
        var cur = 1
        var next = 1
        while (true) {
            yield(next) // next Fibonacci number
            val tmp = cur + next
            cur = next
            next = tmp
        }
    }

    val fibStatefulCont: () -> Int = run {
        var cur = 0
        var next = 1
        val cont = {
            val res = next
            val tmp = cur + next
            cur = next
            next = tmp
            res
        }
        cont
    }

    val barSeq = sequence<Int> {
        yield(1)
        yield(7)
        yield(42)
    }

    val barContOneShot: () -> Pair<Int, () -> Any?> = run {
        val s1 = 1
        val s2 = 7
        val s3 = 42
        {
            s1 to {
                s2 to {
                    s3 to null
                }
            }
        }
    }

    fun fibContOneShot(): () -> Pair<Int, () -> Any?> = run {
        var cur = 0
        var next = 1
        fun cont(): Pair<Int, () -> Any?> = run {
            val res = next
            res to {
                val tmp = cur + next
                cur = next
                next = tmp
                cont()
            }
        }
        ::cont
    }

    var fibContOneShotContState = fibContOneShot()
    fun fibContOneShotCpsStateful(callback: (Int) -> Unit) {
        val pair = fibContOneShotContState()
        fibContOneShotContState = pair.second as () -> Pair<Int, () -> Any?>
        callback(pair.first)
    }

    fun fibContOneShotCpsStateless(): ((Int) -> Unit) -> Unit {
        var fibContOneShotContState = fibContOneShot()
        fun closure(callback: (Int) -> Unit) {
            val pair = fibContOneShotContState()
            fibContOneShotContState = pair.second as () -> Pair<Int, () -> Any?>
            callback(pair.first)
        }
        return ::closure
    }

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {

        println("fooSeq")
        val fooIter0 = fooSeq.iterator()
        for (i in 1..10) println(fooIter0.next())

        println("fooStatefulCont")
        for (i in 1..10) println(fooStatefulCont())

        println("fibSeq")
        val fibIter0 = fibSeq.iterator()
        for (i in 1..10) println(fibIter0.next())

        println("fibStatefulCont")
        for (i in 1..10) println(fibStatefulCont())

        println("barSeq")
        val barIter0 = barSeq.iterator()
        for (i in 1..3) println(barIter0.next())

        println("barContOneShot")
        run {
            val (v1, cont1) = barContOneShot()
            println(v1)
            val (v2, cont2) = cont1() as Pair<Int, () -> Any?>
            println(v2)
            val (v3, cont3) = cont2() as Pair<Int, () -> Any?>
            println(v3)
        }

        println("fibContOneShot")
        run {
            var cont = fibContOneShot()
            for (i in 1..10) {
                val pair = cont()
                val res = pair.first
                cont = pair.second as () -> Pair<Int, () -> Any?>
                println(res)
            }
        }

        println("fibContOneShotCpsStateful")
        run {
            for (i in 1..10) {
                fibContOneShotCpsStateful { println(it) }
            }
        }

        println("fibContOneShotCpsStateless")
        run {
            val fibContOneShotCps = fibContOneShotCpsStateless()
            for (i in 1..10) {
                fibContOneShotCps { println(it) }
            }
        }
    }
}
