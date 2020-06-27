package coroutinesdesign

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield


object DeepRecursionWithYield {

    class Tree(val left: Tree?, val right: Tree?)

    suspend fun depth0(t: Tree?): Int =
            if (t == null) 0 else maxOf(
                    run {
                        yield()
                        depth0(t.left)
                    },
                    run {
                        yield()
                        depth0(t.right) // recursive call two
                    }
            ) + 1

    fun depth(t: Tree?): Int = runBlocking { depth0(t) }

    val n = 100_000

    val deepTree = generateSequence(Tree(null, null)) { prev ->
        Tree(prev, null)
    }.take(n).last()

    @JvmStatic
    fun main(args: Array<String>) {
        println(depth(deepTree))
    }
}