package com.electric.gbyte.internal;

import org.jetbrains.annotations.NotNull;

import java.io.ObjectStreamException;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;

/**
 * @param <K>
 * @param <V>
 * @author bingo
 * @author bingo
 */
public final class LinkedTreeMap<K, V> extends AbstractMap<K, V> implements Serializable {

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final Comparator<Comparable> NATURAL_ORDER = Comparator.naturalOrder();

    Comparator<? super K> comparator;

    Node<K, V> root;

    int size = 0;

    int modCount = 0;

    final Node<K, V> header = new Node<>();

    @SuppressWarnings("unchecked")
    LinkedTreeMap() {
        this((Comparator<? super K>) NATURAL_ORDER);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private LinkedTreeMap(Comparator<? super K> comparator) {
        this.comparator = comparator != null ? comparator : (Comparator) NATURAL_ORDER;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public V get(Object key) {
        Node<K, V> node = findByObject(key);
        return node != null ? node.value : null;
    }

    @Override
    public boolean containsKey(Object key) {
        return findByObject(key) != null;
    }

    @Override
    public V put(K key, V value) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        Node<K, V> created = find(key, true);
        assert created != null;
        V result = created.value;
        created.value = value;
        return result;
    }

    @Override
    public void clear() {
        root = null;
        size = 0;
        modCount++;

        // Clear iteration order
        Node<K, V> h = this.header;
        h.next = h.prev = h;
    }

    @Override
    public V remove(Object key) {
        Node<K, V> node = removeInternalByKey(key);
        return node != null ? node.value : null;
    }

    /**
     * Returns the node at or adjacent to the given key, creating it if
     * requested.
     *
     * @throws ClassCastException if {@code key} and the tree's keys aren't
     *                            mutually comparable.
     */
    private Node<K, V> find(K key, boolean create) {
        Comparator<? super K> c = this.comparator;
        Node<K, V> nearest = root;
        int comparison = 0;

        if (nearest != null) {
            // Micro-optimization: avoid polymorphic calls to Comparator.compare().
            @SuppressWarnings("unchecked") // Throws a ClassCastException below if there's trouble.
            Comparable<Object> comparableKey = (c == NATURAL_ORDER)
                    ? (Comparable<Object>) key
                    : null;

            while (true) {
                comparison = (comparableKey != null)
                        ? comparableKey.compareTo(Objects.requireNonNull(nearest.key))
                        : c.compare(key, nearest.key);

                // We found the requested key.
                if (comparison == 0) {
                    return nearest;
                }

                // If it exists, the key is in a subtree. Go deeper.
                Node<K, V> child = (comparison < 0) ? nearest.left : nearest.right;
                if (child == null) {
                    break;
                }

                nearest = child;
            }
        }

        // The key doesn't exist in this tree.
        if (!create) {
            return null;
        }

        // Create the node and add it to the tree or the table.
        Node<K, V> h = this.header;
        Node<K, V> created;
        if (nearest == null) {
            // Check that the value is comparable if we didn't do any comparisons.
            if (comparator == NATURAL_ORDER && !(key instanceof Comparable)) {
                throw new ClassCastException(key.getClass().getName() + " is not Comparable");
            }
            created = new Node<>(nearest, key, h, h.prev);
            root = created;
        } else {
            created = new Node<>(nearest, key, h, h.prev);
            if (comparison < 0) { // nearest.key is higher
                nearest.left = created;
            } else { // comparison > 0, nearest.key is lower
                nearest.right = created;
            }
            reBalance(nearest, true);
        }
        size++;
        modCount++;

        return created;
    }

    @SuppressWarnings("unchecked")
    Node<K, V> findByObject(Object key) {
        try {
            return key != null ? find((K) key, false) : null;
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Returns this map's entry that has the same key and value as {@code
     * entry}, or null if this map has no such entry.
     *
     * <p>
     * This method uses the comparator for key equality rather than {@code
     * equals}. If this map's comparator isn't consistent with equals (such as
     * {@code String.CASE_INSENSITIVE_ORDER}), then {@code remove()} and {@code
     * contains()} will violate the collections API.
     */
    Node<K, V> findByEntry(Entry<?, ?> entry) {
        Node<K, V> mine = findByObject(entry.getKey());
        boolean valuesEqual = mine != null && equal(mine.value, entry.getValue());
        return valuesEqual ? mine : null;
    }

    private boolean equal(Object a, Object b) {
        return Objects.equals(a, b);
    }

    /**
     * Removes {@code node} from this tree, rearranging the tree's structure as
     * necessary.
     *
     * @param unlink true to also unlink this node from the iteration linked
     *               list.
     */
    void removeInternal(Node<K, V> node, boolean unlink) {
        if (unlink) {
            node.prev.next = node.next;
            node.next.prev = node.prev;
        }

        Node<K, V> left = node.left;
        Node<K, V> right = node.right;
        Node<K, V> originalParent = node.parent;
        if (left != null && right != null) {

            /*
             * To remove a node with both left and right subtrees, move an
             * adjacent node from one of those subtrees into this node's place.
             *
             * Removing the adjacent node may change this node's subtrees. This
             * node may no longer have two subtrees once the adjacent node is
             * gone!
             */
            Node<K, V> adjacent = (left.height > right.height) ? left.last() : right.first();
            removeInternal(adjacent, false); // takes care of reBalance and size--

            int leftHeight = 0;
            left = node.left;
            if (left != null) {
                leftHeight = left.height;
                adjacent.left = left;
                left.parent = adjacent;
                node.left = null;
            }

            int rightHeight = 0;
            right = node.right;
            if (right != null) {
                rightHeight = right.height;
                adjacent.right = right;
                right.parent = adjacent;
                node.right = null;
            }

            adjacent.height = Math.max(leftHeight, rightHeight) + 1;
            replaceInParent(node, adjacent);
            return;
        } else if (left != null) {
            replaceInParent(node, left);
            node.left = null;
        } else if (right != null) {
            replaceInParent(node, right);
            node.right = null;
        } else {
            replaceInParent(node, null);
        }

        reBalance(originalParent, false);
        size--;
        modCount++;
    }

    Node<K, V> removeInternalByKey(Object key) {
        Node<K, V> node = findByObject(key);
        if (node != null) {
            removeInternal(node, true);
        }
        return node;
    }

    private void replaceInParent(Node<K, V> node, Node<K, V> replacement) {
        Node<K, V> parent = node.parent;
        node.parent = null;
        if (replacement != null) {
            replacement.parent = parent;
        }

        if (parent != null) {
            if (parent.left == node) {
                parent.left = replacement;
            } else {
                assert (parent.right == node);
                parent.right = replacement;
            }
        } else {
            root = replacement;
        }
    }

    private void reBalance(Node<K, V> unbalanced, boolean insert) {
        OUTER:
        for (Node<K, V> node = unbalanced; node != null; node = node.parent) {
            Node<K, V> left = node.left;
            Node<K, V> right = node.right;
            int leftHeight = left != null ? left.height : 0;
            int rightHeight = right != null ? right.height : 0;
            int delta = leftHeight - rightHeight;
            switch (delta) {
                case -2 -> {
                    Node<K, V> rightLeft = Objects.requireNonNull(right).left;
                    Node<K, V> rightRight = right.right;
                    int rightRightHeight = rightRight != null ? rightRight.height : 0;
                    int rightLeftHeight = rightLeft != null ? rightLeft.height : 0;
                    int rightDelta = rightLeftHeight - rightRightHeight;
                    if (rightDelta == -1 || (rightDelta == 0 && !insert)) {
                        rotateLeft(node); // AVL right right
                    } else {
                        assert (rightDelta == 1);
                        rotateRight(right); // AVL right left
                        rotateLeft(node);
                    }
                    if (insert) {
                        break OUTER; // no further rotations will be necessary
                    }
                }
                case 2 -> {
                    Node<K, V> leftLeft = Objects.requireNonNull(left).left;
                    Node<K, V> leftRight = left.right;
                    int leftRightHeight = leftRight != null ? leftRight.height : 0;
                    int leftLeftHeight = leftLeft != null ? leftLeft.height : 0;
                    int leftDelta = leftLeftHeight - leftRightHeight;
                    if (leftDelta == 1 || (leftDelta == 0 && !insert)) {
                        rotateRight(node); // AVL left left
                    } else {
                        assert (leftDelta == -1);
                        rotateLeft(left); // AVL left right
                        rotateRight(node);
                    }
                    if (insert) {
                        break OUTER; // no further rotations will be necessary
                    }
                }
                case 0 -> {
                    node.height = leftHeight + 1; // leftHeight == rightHeight
                    if (insert) {
                        break OUTER; // the insert caused balance, so rebalancing is done!
                    }
                }
                default -> {
                    assert (delta == -1 || delta == 1);
                    node.height = Math.max(leftHeight, rightHeight) + 1;
                    if (!insert) {
                        break OUTER; // the height hasn't changed, so rebalancing is done!
                    }
                }
            }
        }
    }

    /**
     * Rotates the subtree so that its root's right child is the new root.
     */
    private void rotateLeft(Node<K, V> root) {
        Node<K, V> left = root.left;
        Node<K, V> pivot = root.right;
        Node<K, V> pivotLeft = pivot.left;
        Node<K, V> pivotRight = pivot.right;

        // move the pivot's left child to the root's right
        root.right = pivotLeft;
        if (pivotLeft != null) {
            pivotLeft.parent = root;
        }

        replaceInParent(root, pivot);

        // move the root to the pivot's left
        pivot.left = root;
        root.parent = pivot;

        // fix heights
        root.height = Math.max(left != null ? left.height : 0,
                pivotLeft != null ? pivotLeft.height : 0) + 1;
        pivot.height = Math.max(root.height,
                pivotRight != null ? pivotRight.height : 0) + 1;
    }

    /**
     * Rotates the subtree so that its root's left child is the new root.
     */
    private void rotateRight(Node<K, V> root) {
        Node<K, V> pivot = root.left;
        Node<K, V> right = root.right;
        Node<K, V> pivotLeft = pivot.left;
        Node<K, V> pivotRight = pivot.right;

        // move the pivot's right child to the root's left
        root.left = pivotRight;
        if (pivotRight != null) {
            pivotRight.parent = root;
        }

        replaceInParent(root, pivot);

        // move the root to the pivot's right
        pivot.right = root;
        root.parent = pivot;

        // fixup heights
        root.height = Math.max(right != null ? right.height : 0,
                pivotRight != null ? pivotRight.height : 0) + 1;
        pivot.height = Math.max(root.height,
                pivotLeft != null ? pivotLeft.height : 0) + 1;
    }

    private EntrySet entrySet;
    private KeySet keySet;

    @Override
    public @NotNull Set<Entry<K, V>> entrySet() {
        EntrySet result = entrySet;
        return result != null ? result : (entrySet = new EntrySet());
    }

    @Override
    public @NotNull Set<K> keySet() {
        KeySet result = keySet;
        return result != null ? result : (keySet = new KeySet());
    }

    static final class Node<K, V> implements Entry<K, V> {

        Node<K, V> parent;
        Node<K, V> left;
        Node<K, V> right;
        Node<K, V> next;
        Node<K, V> prev;
        final K key;
        V value;
        int height;

        /**
         * Create the header entry
         */
        Node() {
            key = null;
            next = prev = this;
        }

        /**
         * Create a regular entry
         */
        Node(Node<K, V> parent, K key, Node<K, V> next, Node<K, V> prev) {
            this.parent = parent;
            this.key = key;
            this.height = 1;
            this.next = next;
            this.prev = prev;
            prev.next = this;
            next.prev = this;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public boolean equals(Object o) {
            if (o instanceof Entry) {
                Entry other = (Entry) o;
                return (key == null ? other.getKey() == null : key.equals(other.getKey()))
                        && (value == null ? other.getValue() == null : value.equals(other.getValue()));
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (key == null ? 0 : key.hashCode())
                    ^ (value == null ? 0 : value.hashCode());
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }

        /**
         * Returns the first node in this subtree.
         */
        public Node<K, V> first() {
            Node<K, V> node = this;
            Node<K, V> child = node.left;
            while (child != null) {
                node = child;
                child = node.left;
            }
            return node;
        }

        /**
         * Returns the last node in this subtree.
         */
        public Node<K, V> last() {
            Node<K, V> node = this;
            Node<K, V> child = node.right;
            while (child != null) {
                node = child;
                child = node.right;
            }
            return node;
        }
    }

    private abstract class LinkedTreeMapIterator<T> implements Iterator<T> {

        Node<K, V> next = header.next;
        Node<K, V> lastReturned = null;
        int expectedModCount = modCount;

        LinkedTreeMapIterator() {

        }

        @Override
        public final boolean hasNext() {
            return next != header;
        }

        final Node<K, V> nextNode() {
            Node<K, V> e = next;
            if (e == header) {
                throw new NoSuchElementException();
            }
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            next = e.next;
            return lastReturned = e;
        }

        @Override
        public final void remove() {
            if (lastReturned == null) {
                throw new IllegalStateException();
            }
            removeInternal(lastReturned, true);
            lastReturned = null;
            expectedModCount = modCount;
        }
    }

    class EntrySet extends AbstractSet<Entry<K, V>> {

        @Override
        public int size() {
            return size;
        }

        @Override
        public @NotNull Iterator<Entry<K, V>> iterator() {
            return new LinkedTreeMapIterator<Entry<K, V>>() {
                @Override
                public Entry<K, V> next() {
                    return nextNode();
                }
            };
        }

        @Override
        public boolean contains(Object o) {
            return o instanceof Entry && findByEntry((Entry<?, ?>) o) != null;
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Entry)) {
                return false;
            }

            Node<K, V> node = findByEntry((Entry<?, ?>) o);
            if (node == null) {
                return false;
            }
            removeInternal(node, true);
            return true;
        }

        @Override
        public void clear() {
            LinkedTreeMap.this.clear();
        }
    }

    final class KeySet extends AbstractSet<K> {

        @Override
        public int size() {
            return size;
        }

        @Override
        public @NotNull Iterator<K> iterator() {
            return new LinkedTreeMapIterator<K>() {
                public K next() {
                    return nextNode().key;
                }
            };
        }

        @Override
        public boolean contains(Object o) {
            return containsKey(o);
        }

        @Override
        public boolean remove(Object key) {
            return removeInternalByKey(key) != null;
        }

        @Override
        public void clear() {
            LinkedTreeMap.this.clear();
        }
    }

    @Serial
    private Object writeReplace() throws ObjectStreamException {
        return new LinkedHashMap<>(this);
    }

}
