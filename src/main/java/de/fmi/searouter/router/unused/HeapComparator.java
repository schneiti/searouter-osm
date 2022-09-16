package de.fmi.searouter.router.unused;

public interface HeapComparator {

    /**
     * compares the distances from the start node of elements at specific positions on the heap.
     * Returns a number indicating the relation of the distances.
     *
     * @param firstNodeID  the node id of the first position on the heap
     * @param secondNodeID the node id of the second position on the heap
     * @return 0 if equal, 1 if distance of first element is larger, else -1
     */
    int compareValues(int firstNodeID, int secondNodeID);

}
