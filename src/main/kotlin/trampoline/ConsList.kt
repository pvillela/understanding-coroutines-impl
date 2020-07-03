package trampoline

/*
 * From https://adamschoenemann.dk/posts/2019-02-12-trampolines.html.
 */

sealed class ConsList<out A> {

    fun unsafeHead(): A = when (this) {
        Nil -> throw RuntimeException("unsafeHead: head on empty list")
        is Cons -> hd
    }

    fun unsafeTail(): ConsList<A> = when (this) {
        Nil -> throw RuntimeException("unsafeTail: tail on empty list")
        is Cons -> tl
    }

    val isEmpty get() = this is Nil
}

object Nil : ConsList<Nothing>()

data class Cons<A>(val hd: A, val tl: ConsList<A>) : ConsList<A>()

val nil = Nil

infix fun <T> T.cons(l: ConsList<T>): ConsList<T> = Cons(this, l)
