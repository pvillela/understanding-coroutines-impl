package coroutinesdesign

import coroutinesdesign.DeepRecursionCommon.Tree
import coroutinesdesign.DeepRecursionCommon.deepTree
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield


object DeepRecursionWithYield {

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

    @JvmStatic
    fun main(args: Array<String>) {
//        val n = 100_000
        val n = 2
        println(depth(deepTree(n)))
    }
}
