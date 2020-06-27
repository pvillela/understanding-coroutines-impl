package coroutinesdesign

object DeepRecursionCommon {

    class Tree(val left: Tree?, val right: Tree?) {
        override fun toString(): String {
            val l = left?.run { "Tree@${this.hashCode()}" } ?: "null"
            val r = right?.run { "Tree@${this.hashCode()}" } ?: "null"
            return "Tree($l, $r)"
        }
    }

    fun narrowTree(depth: Int, leftBias: Boolean): Tree? =
            if (depth == 0)
                null
            else
                generateSequence(Tree(null, null)) { prev ->
                    if (leftBias) Tree(prev, null)
                    else Tree(null, prev)
                }.take(depth).last()

    fun deepTree(depth: Int): Tree? =
            if (depth == 0)
                null
            else
                Tree(narrowTree(depth - 1, true), narrowTree(depth - 1, false))
}
