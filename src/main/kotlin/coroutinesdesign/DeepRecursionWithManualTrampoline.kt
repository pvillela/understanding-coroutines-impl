package coroutinesdesign

import coroutinesdesign.DeepRecursionCommon.Tree
import coroutinesdesign.DeepRecursionCommon.deepTree
import kotlinx.coroutines.runBlocking

/**
 * See [DeepRecursion]. Here, we implement a solution using manual trampolining instead.
 */
object DeepRecursionWithManualTrampoline {

    fun depth(t: Tree?): Int =
            if (t == null) 0
            else maxOf(
                    depth(t.left),
                    depth((t.right))
            ) + 1

    fun depthCpsSm(t: Tree?, cont: SmCont) {
        if (t == null) {
            cont(0)
        }
        else {
            val sm = object : SmCont {
                var label = 0
                var resL: Int? = null
                var resR: Int? = null
                override fun invoke(input: Any?) {
                    when (label) {
                        0 -> {
                            ++label
                            depthCpsSm(t.left, this)
                        }
                        1 -> {
                            resL = input as Int
                            ++label
                            depthCpsSm(t.right, this)
                        }
                        2 -> {
                            resR = input as Int
                            val res = maxOf(resL!!, resR!!) + 1
                            cont(res)
                        }
                    }
                }
            }
            sm(null)
        }
    }

    fun depthCpsSmTrampoline(t: Tree?, cont: SmCont) {
        var resumeThunk: () -> Unit = { }
        var completed = false
        fun depthCpsSmTrampoline0(t: Tree?, cont: SmCont) {
            if (t == null) {
                resumeThunk = { cont(0) }
            } else {
                val sm = object : SmCont {
                    var label = 0
                    var resL: Int? = null
                    var resR: Int? = null
                    override fun invoke(input: Any?) {
                        when (label) {
                            0 -> {
                                ++label
                                resumeThunk = { depthCpsSmTrampoline0(t.left, this) }
                            }
                            1 -> {
                                resL = input as Int
                                ++label
                                resumeThunk = { depthCpsSmTrampoline0(t.right, this) }
                            }
                            2 -> {
                                resR = input as Int
                                val res = maxOf(resL!!, resR!!) + 1
                                resumeThunk = { cont(res) }
                            }
                        }
                    }
                }
                sm(null)
            }
        }
        val finalCont: SmCont = { x ->
            cont(x)
            completed = true
        }
        depthCpsSmTrampoline0(t, finalCont)
        do {
            resumeThunk()
        } while (!completed)
    }

    val finalCont: SmCont = { x ->
        println(x)
    }

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val n = 100_000
//        val n = 10

        try {
            println(depth(deepTree(n)))
        } catch (e: StackOverflowError) {
            println(e)
        }

        try {
            depthCpsSm(deepTree(n), finalCont)
        } catch (e: StackOverflowError) {
            println(e)
        }

        try {
            depthCpsSmTrampoline(deepTree(n), finalCont)
        } catch (e: StackOverflowError) {
            println(e)
        }
    }
}
