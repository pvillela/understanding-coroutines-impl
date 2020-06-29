package coroutinesdesign

import coroutinesdesign.DeepRecursionCommon.Tree
import coroutinesdesign.DeepRecursionCommon.deepTree
import coroutinesdesign.DeepRecursionWithYield.main
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield


/**
 * Based on https://www.youtube.com/watch?v=NURkLq6BzLE.
 * See slide 93 in https://docs.google.com/presentation/d/1fszsKHr69JwsVLWfazB3WAEGapY0Fe0k6GFZDD-4AB0/edit#slide=id.g65b1b3449f_1_24
 * See DeepRecursionWithYield_Manual_Execution_n_2.txt for a step-by-step manual execution of [main]
 * in the case n = 2.
 */
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
