# Coroutines Design

Experiments and demonstrations related to the official Kotlin Coroutines Design KEEP document.  

Includes detailed examples of:

- The process used by the Kotlin compiler to transform suspend functions with continuation passing style and state machines.
- Continuations implemented with nested closures as well as continuations implemented with state machines.
- Trampolining for stack safety.
