package coroutinesdesign


typealias CpsIntCont = (Int) -> Unit

object Cps {
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

    fun f1Cps(x: Int, cont: CpsIntCont) {
        cont(x + 1)
    }

    fun f2Cps(x: Int, cont: CpsIntCont) {
        cont(x * 10)
    }

    fun fCps(x: Int, cont: CpsIntCont) {
        val y = x + 10
//    val z = f1(y)
        val cont1 = { z: Int ->
            val u = z + 2
//        val v = f2(u)
            val cont2 = { v: Int ->
                val w = v + 3
                cont(w)
            }
            f2Cps(u, cont2)
        }
        f1Cps(y, cont1)
    }

    val finalCont: CpsIntCont = { x ->
        println(x)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        println("Direct style")
        println(f(1))

        println("Continuation passing style")
        fCps(1, finalCont)
    }
}
