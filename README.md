# Understanding Coroutines Implementation

Experiments and demonstrations to help with the understanding of how Kotlin coroutines are implemented, based on the official [Kotlin Coroutines Design KEEP](https://github.com/Kotlin/KEEP/blob/master/proposals/coroutines.md) document.  

Includes detailed examples of:

- The process used by the Kotlin compiler to transform suspend functions with continuation passing style and state machines.
- Continuations implemented with nested closures as well as continuations implemented with state machines.
- Trampolining for stack safety.
- Execution logs comparing the performance of different implementation approaches.
