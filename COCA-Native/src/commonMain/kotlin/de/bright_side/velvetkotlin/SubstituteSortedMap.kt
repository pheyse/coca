package de.bright_side.velvetkotlin

/**
 * A very basic (not well preforming, not thread-safe, ...) implementation of a sorted Map (such as TreeMap in Java)
 * which also works in Kotlin/Native while e.g. TreeMap is not available in Kotlin/Native
 * */
class SubstituteSortedMap<K: Comparable<K>, V>: MutableMap<K, V> {
    private var cachedOrderedKeys: MutableList<K>? = null
    private val backing = mutableMapOf<K, V>()

    private fun clearOrderedKeys(){
        cachedOrderedKeys = null
    }

    private fun prepareOrderedKeys(): MutableList<K> {
        var result = cachedOrderedKeys
        if (result == null){
            result = backing.keys.sorted().toMutableList()
            cachedOrderedKeys = result
        }
        return result
    }

    override fun put(key: K, value: V): V? {
        clearOrderedKeys()
        return backing.put(key, value)
    }

    override val size: Int
        get() = backing.size

    override fun containsKey(key: K): Boolean = backing.containsKey(key)
    override fun containsValue(value: V): Boolean = backing.containsValue(value)
    override fun get(key: K): V? = backing.get(key)
    override fun isEmpty(): Boolean = backing.isEmpty()

    override val keys: MutableSet<K>
        get() = backing.keys

    override fun clear() {
        clearOrderedKeys()
        backing.clear()
    }

    override fun putAll(from: Map<out K, V>) {
        clearOrderedKeys()
        backing.putAll(from)
    }

    override fun remove(key: K): V? {
        clearOrderedKeys()
        return backing.remove(key)
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            return LinkedHashSet(SubstituteSortedSet(backing.entries.map{SortableMapEntry(it.key, it.value)}))



        }

    override val values: MutableCollection<V>
        get() = prepareOrderedKeys().map {backing[it]!! }.toMutableList()

    class SortableMapEntry<K: Comparable<K>, V>(val itemKey: K, var itemValue: V): Comparable<SortableMapEntry<K, V>>, MutableMap.MutableEntry<K, V>{
        override operator fun compareTo(other: SortableMapEntry<K, V>): Int{
            return itemKey.compareTo(other.itemKey)
        }

        override fun setValue(newValue: V): V {
            itemValue = newValue
            return newValue
        }

        override val key: K
            get() = itemKey
        override val value: V
            get() = itemValue
    }

}