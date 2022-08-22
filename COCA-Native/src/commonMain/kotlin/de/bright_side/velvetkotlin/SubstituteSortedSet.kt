package de.bright_side.velvetkotlin

/**
 * A very basic (not well preforming, not thread-safe, ...) implementation of a sorted Set (such as TreeSet in Java)
 * which also works in Kotlin/Native while e.g. TreeSet is not available in Kotlin/Native
 */
class SubstituteSortedSet<E: Comparable<E>>(): MutableSet<E>{

    constructor(elements: Collection<E>) : this() {
        addAll(elements)
    }

    private var backing: MutableSet<E> = mutableSetOf()
    private var cachedOrderedItems: MutableList<E>? = null

    private fun prepareOrderedItems(): MutableList<E> {
        var result = cachedOrderedItems
        if (result == null){
            result = backing.sorted().toMutableList()
            cachedOrderedItems = result
        }
        return result
    }

    private fun clearOrderedItems(){
        cachedOrderedItems = null
    }

    override fun iterator(): MutableIterator<E> = prepareOrderedItems().iterator()

    override fun remove(element: E): Boolean {
        clearOrderedItems()
        return backing.remove(element)
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        clearOrderedItems()
        return backing.removeAll(elements)
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        clearOrderedItems()
        return backing.retainAll(elements)
    }

    override val size: Int
        get() = backing.size

    override fun contains(element: E): Boolean = backing.contains(element)
    override fun containsAll(elements: Collection<E>): Boolean = backing.containsAll(elements)
    override fun isEmpty(): Boolean = backing.isEmpty()

    override fun add(element: E): Boolean {
        clearOrderedItems()
        return backing.add(element)
    }

    override fun addAll(elements: Collection<E>): Boolean {
        clearOrderedItems()
        return backing.addAll(elements)
    }

    override fun clear() {
        clearOrderedItems()
        backing.clear()
    }

}