package org.opentripplanner.netex.loader.support;


public abstract class AbstractHierarchicalMap<K,V> {

    private final AbstractHierarchicalMap<K,V> parent;

    AbstractHierarchicalMap(AbstractHierarchicalMap<K, V> parent) {
        this.parent = parent;
    }


    /**
     * Lookup element, if not found delegate up to the parent.
     * NB! elements of this class and its parents are NOT merged, the closest win.
     * @return an empty collection if no element are found.
     */
    public V lookup(K key) {
        V v = localGet(key);
        return (valueExist(v) || isRoot()) ? v : parent.lookup(key);
    }

    /**
     * The key exist in this Collection or one of the parents (parent, parent´s parent and so on)
     */
    public boolean containsKey(K key) {
        return localContainsKey(key) || (isChild() && parent.localContainsKey(key));
    }

    /** The size of the collection including any parent nodes. Returns the number of key-value pairs for a Map. */
    public int size() {
        return localSize() + (isChild() ? parent.localSize() : 0);
    }

    /** Return a reference to the parent. */
    public AbstractHierarchicalMap<K,V> pop() {
        return parent;
    }

    /** Get value from 'local' map, parent is not queried. */
    protected abstract V localGet(K key);

    /** Check if key exist in 'local' map, parent is not queried. */
    protected abstract boolean localContainsKey(K key);

    /** Return the size of the collection. Returns the number of key-value pairs for a Map. */
    protected abstract int localSize();

    /**
     * Check if a value exist; hence should the value be returned by the {@link #lookup(Object)} method.
     * Override this method to provide a more sophisticated check than just a check on not <code>null</code>.
     * */
    protected boolean valueExist(V value) {
        return value != null;
    }


    /* private methods */

    private boolean isRoot() {
        return parent == null;
    }

    private boolean isChild() {
        return parent != null;
    }

    @Override
    public String toString() {
        // It helps in debugging to se the size before expanding the element
        return "size = " + size();
    }
}


