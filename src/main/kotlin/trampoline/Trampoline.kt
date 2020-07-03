package trampoline

/*
 * From https://adamschoenemann.dk/posts/2019-02-12-trampolines.html.
 */

sealed class Trampoline<out T> {
    fun <U> flatMap(to: (T) -> Trampoline<U>): Trampoline<U> = FlatMap(this, to)
}

private data class Done<out T>(val t: T) : Trampoline<T>()

// Delay is a specialization of FlatMap but improves performance
private data class Delay<out T>(
        val suspension: () -> Trampoline<T>
) : Trampoline<T>()

private data class FlatMap<T, out U>(
        val waitFor: Trampoline<T>,
        val cont: (T) -> Trampoline<U>
) : Trampoline<U>()

fun <T> done(t: T): Trampoline<T> = Done(t)

fun <T> delay(suspension: () -> Trampoline<T>): Trampoline<T> = Delay(suspension)
// or just FlatMap(done(Unit), { suspension() })

@Suppress("UNCHECKED_CAST")
fun <T> Trampoline<T>.run(): T {
    var r: Trampoline<*> = this
    var stack: ConsList<(Any?) -> Trampoline<*>> = nil
    while (true) {
        when (r) {
            is Done ->
                if (stack.isEmpty) {
                    return r.t as T
                } else {
                    r = stack.unsafeHead()(r.t)
                    stack = stack.unsafeTail()
                }
            is Delay -> {
                r = r.suspension()
            }
            is FlatMap<*, *> -> {
                stack = (r.cont as (Any?) -> Trampoline<*>) cons stack
                r = r.waitFor
            }
        }
    }
}


fun main() {
    val foo: Trampoline<Int> =
            delay { done(2) }.flatMap { done(it * 10) }
    println(foo.run())
}
