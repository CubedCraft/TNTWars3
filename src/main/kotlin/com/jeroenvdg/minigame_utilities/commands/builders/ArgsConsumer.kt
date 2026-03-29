package com.jeroenvdg.minigame_utilities.commands.builders

@Deprecated("Since 1.21.11, use ArgumentType")
class ArgsConsumer(val args: Array<out String>) {

    var ptr: Int = 0

    fun peek(): String = args[ptr]
    fun consumeWord(): String = args[ptr++]

    fun isFlag(): Boolean {
        return args[ptr].startsWith("-")
    }

    fun consumeToEnd(): String {
        val start = ptr
        return args.copyOfRange(start, args.size).joinToString(" ");
    }

    fun reset() {
        ptr = 0
    }

    fun copy(): ArgsConsumer {
        val copy = ArgsConsumer(args)
        copy.ptr = ptr
        return copy
    }

    fun hasNext() = ptr < args.size
}