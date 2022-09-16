package de.fmi.searouter.router.astar;

import de.fmi.searouter.dijkstragrid.Node;

import java.util.Arrays;

/**
 * class used to effeciently manage a priority queue of ids (represented as int instead of Integer objects)
 */
public class AStarHeap {
    private final int INITIAL_SIZE = 400;
    private final int SIZE_INCREASE = 200;

    //for each node, the position of it in the heap or -1 if not on the heap
    private final int[] heapPosition;
    //the array representing the heap
    private int[] idHeapArray;
    private int currentSize;
    //used when comparing the distances of ids
    private final AStarRouter router;

    /**
     * constructor for the dijkstra heap
     * @param router the router containing the distance array
     */
    protected AStarHeap(AStarRouter router) {
        this.heapPosition = new int[Node.getSize()];
        this.idHeapArray = new int[INITIAL_SIZE];
        Arrays.fill(idHeapArray, -1);
        this.router = router;
        currentSize = 0;
    }

    /**
     * resets the satate of the heap and prepares it for a new calculation
     */
    protected void resetState() {
        Arrays.fill(heapPosition, -1);
        this.idHeapArray = new int[INITIAL_SIZE];
        Arrays.fill(idHeapArray, -1);
        currentSize = 0;
    }

    /**
     * checks if the heap is empty
     * @return true if no more elements are contained on the heap, else false
     */
    protected boolean isEmpty() {
        return (currentSize == 0);
    }

    /**
     * gets the id of the node with the lowest distance from the start node stored on the heap. Also restores
     * the remaining array to a heap.
     * @return the id of the node with the shortest distance
     */
    protected int getNext() {
        int returnValue = idHeapArray[0];

        currentSize--;
        idHeapArray[0] = idHeapArray[currentSize];
        heapPosition[idHeapArray[0]] = 0;
        idHeapArray[currentSize] = -1;
        heapifyTopDown(currentSize, 0);
        return returnValue;
    }

    /**
     * adds an id to the heap. if the id is already on the heap, updates its position if necessary
     * (for example after an update).
     * @param id the id to add
     */
    protected void add(int id) {
        if(heapPosition[id] != -1) {
            //update, do not add again
            heapifyBottomUp(heapPosition[id]);
        } else {
            if(currentSize == idHeapArray.length) {
                grow();
            }
            idHeapArray[currentSize] = id;
            heapPosition[id] = currentSize;
            heapifyBottomUp(currentSize);
            currentSize++;
        }
    }

    /**
     * restores the heap property of the array after removing the first element.
     * @param n the length of the array
     * @param root the position in the array of the root of the subtree to heapify
     */
    private void heapifyTopDown(int n, int root) {
        int smallest = root; // Initialize smallest as root
        int leftChild = 2 * root + 1;
        int rightChild = 2 * root + 2;

        // If left child is larger than root
        if (leftChild < n && compareValues(leftChild, smallest) < 0) {
            smallest = leftChild;
        }

        // If right child is larger than smallest so far
        if (rightChild < n && compareValues(rightChild, smallest) < 0) {
            smallest = rightChild;
        }

        // If smallest is not root
        if (smallest != root) {
            swap(root, smallest);

            // Recursively heapify the affected sub-tree
            heapifyTopDown(n, smallest);
        }
    }

    /**
     * restores the heap property of the array after adding another element. also used to update the
     * position of an id already contained in the array.
     * @param nodeID the position in the array of the node to check
     */
    private void heapifyBottomUp(int nodeID) {
        if(nodeID <= 0) {
            return;
        }
        // Find parent
        int parent = (nodeID - 1) / 2;

        // For Min-Heap
        // If current node is greater than its parent
        // Swap both of them and call heapify again
        // for the parent
        if (compareValues(nodeID, parent) < 0) {
            swap(nodeID, parent);
            // Recursively heapify the parent node
            heapifyBottomUp(parent);
        }
    }

    /**
     * swaps two elements within the array. Also updates the heap positions of these elements.
     * @param i the position of the first element
     * @param j the position of the second element
     */
    private void swap(int i, int j) {
        int tmp = idHeapArray[i];
        idHeapArray[i] = idHeapArray[j];
        idHeapArray[j] = tmp;

        //also update positions in heap information
        tmp = heapPosition[idHeapArray[i]];
        heapPosition[idHeapArray[i]] = heapPosition[idHeapArray[j]];
        heapPosition[idHeapArray[j]] = tmp;
    }

    /**
     * increases the size of the heap array.
     */
    private void grow() {
        int oldLen = idHeapArray.length;
        idHeapArray = Arrays.copyOf(idHeapArray, oldLen + SIZE_INCREASE);
        Arrays.fill(idHeapArray, oldLen, idHeapArray.length, -1);
    }

    /**
     * compares the distances from the start node of elements at specific positions on the heap.
     * Returns a number indicating the relation of the distances.
     * @param firstID the first position on the heap
     * @param secondID the first position on the heap
     * @return 0 if equal, 1 if distance of first element is larger, else -1
     */
    private int compareValues(int firstID, int secondID) {
        if(router.currDistanceToNodeAStar[idHeapArray[firstID]] == router.currDistanceToNodeAStar[idHeapArray[secondID]]) {
            return 0;
        } else if(router.currDistanceToNodeAStar[idHeapArray[firstID]] > router.currDistanceToNodeAStar[idHeapArray[secondID]]) {
            return 1;
        } else {
            return -1;
        }
    }
}
