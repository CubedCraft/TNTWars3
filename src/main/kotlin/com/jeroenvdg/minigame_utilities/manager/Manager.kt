package com.jeroenvdg.minigame_utilities.manager

abstract class Manager<T : Manageable> : Iterable<T>, Collection<T> {
    protected val elements = ArrayList<T>()
    val enabledElements get() = elements.filter { it.enabled }
    override val size get() = elements.size

    operator fun get(index: Int) = elements[index]
    override fun isEmpty() = elements.isEmpty()
    fun indexOf(element: T) = elements.indexOf(element)
    override fun containsAll(elements: Collection<T>) = elements.containsAll(elements)
    override fun contains(element: T) = elements.contains(element)


    fun add(add: T) {
        elements.add(add)
    }


    fun exists(id: String): Boolean {
        return find(id) != null
    }


    fun find(id: String): T? {
        return elements.find { it.id == id }
    }


    fun remove(id: String): Boolean {
        val element = find(id) ?: return false
        return remove(element)
    }


    fun remove(element: T): Boolean {
        return elements.remove(element)
    }


    fun clear() {
        elements.clear()
    }


    override fun iterator(): Iterator<T> {
        return elements.iterator()
    }
}