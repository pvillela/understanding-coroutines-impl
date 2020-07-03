package trampoline

import coroutinesdesign.DeepRecursionCommon.Tree
import coroutinesdesign.DeepRecursionCommon.balancedTree
import coroutinesdesign.DeepRecursionCommon.deepTree
import coroutinesdesign.DeepRecursionOriginal.DeepRecursiveFunction
import coroutinesdesign.DeepRecursionOriginal.invoke
import coroutinesdesign.SmCont
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.system.measureTimeMillis


fun treeDepthRecursive(t: Tree?): Int =
        if (t == null) 0 else maxOf(
                treeDepthRecursive(t.left),
                treeDepthRecursive(t.right)
        ) + 1

suspend fun treeDepthRecursiveWithYield0(t: Tree?): Int =
        if (t == null) 0 else maxOf(
                run { yield(); treeDepthRecursiveWithYield0(t.left) },
                run { yield(); treeDepthRecursiveWithYield0(t.right) }
        ) + 1

fun treeDepthRecursiveWithYield(t: Tree?): Int = runBlocking {
    treeDepthRecursiveWithYield0(t)
}

val treeDepthDeepRecursive = DeepRecursiveFunction<Tree?, Int> { t ->
    if (t == null) 0 else maxOf(
            callRecursive(t.left),
            callRecursive(t.right)
    ) + 1
}

fun treeDepthManualTrampoline(t: Tree?): Int {
    var resumeThunk: () -> Unit = { }
    var completed = false
    fun treeDepth0(t: Tree?, cont: SmCont) {
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
                            resumeThunk = { treeDepth0(t.left, this) }
                        }
                        1 -> {
                            resL = input as Int
                            ++label
                            resumeThunk = { treeDepth0(t.right, this) }
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
    var ret: Int? = null
    val finalCont: SmCont = { x ->
        ret = x as Int?
        completed = true
    }
    treeDepth0(t, finalCont)
    do {
        resumeThunk()
    } while (!completed)
    return ret!!
}

fun treeDepthTrampoline(t: Tree?): Trampoline<Int> =
        if (t == null) done(0)
        else delay { treeDepthTrampoline(t.left) }.flatMap { resL ->
            treeDepthTrampoline(t.right).flatMap { resR ->
                done(maxOf(resL, resR) + 1)
            }
        }


fun main() {
    val n = 1_000_000
    val tree = deepTree(n)

//    val n = 28
//    val tree = balancedTree(n)

    println("n = $n")

    println("treeDepthRecursive")
    val treeDepthRecursiveTime = measureTimeMillis {
        try {
            println(treeDepthRecursive(tree))
        } catch (e: StackOverflowError) {
            println(e)
        }
    }
    println(treeDepthRecursiveTime)

    println("treeDepthRecursiveWithYield")
    val treeDepthRecursiveWithYieldTime = measureTimeMillis {
        println(treeDepthRecursiveWithYield(tree))
    }
    println(treeDepthRecursiveWithYieldTime)

    println("treeDepthDeepRecursive")
    val treeDepthDeepRecursiveTime = measureTimeMillis {
        println(treeDepthDeepRecursive(tree))
    }
    println(treeDepthDeepRecursiveTime)

    println("treeDepthManualTrampoline")
    val treeDepthManualTrampolineTime = measureTimeMillis {
        println(treeDepthManualTrampoline(tree))
    }
    println(treeDepthManualTrampolineTime)

    println("treeDepthTrampoline")
    val treeDepthTrampolineTime = measureTimeMillis {
        println(treeDepthTrampoline(tree).run())
    }
    println(treeDepthTrampolineTime)
}