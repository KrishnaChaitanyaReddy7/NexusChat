package nexuschat.network;

import nexuschat.model.Message;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Unit III Syllabus:
 * - Multithreading: Inter-thread communication using wait(), notify(), and notifyAll()
 * - Synchronizing threads: synchronized blocks and synchronized methods
 * - Thread-safe Producer-Consumer Queue implementation
 */
public class SynchronizedMessageBuffer {

    private final Queue<Message> queue;
    private final int capacity;

    public SynchronizedMessageBuffer(int capacity) {
        this.capacity = capacity;
        this.queue = new LinkedList<>(); // Unit IV: Queue / LinkedList from Collections Framework
    }

    /**
     * Producer method: Adds a message to the buffer.
     * Uses wait() if the buffer is full.
     */
    public synchronized void produce(Message message) throws InterruptedException {
        while (queue.size() >= capacity) {
            // Buffer is full; release lock and wait for consumer to take an item
            wait();
        }
        queue.add(message);
        // Notify all waiting consumer threads that a new message is available
        notifyAll();
    }

    /**
     * Consumer method: Retrieves and removes a message from the buffer.
     * Uses wait() if the buffer is empty.
     */
    public synchronized Message consume() throws InterruptedException {
        while (queue.isEmpty()) {
            // Buffer is empty; release lock and wait for producer to add an item
            wait();
        }
        Message message = queue.poll();
        // Notify all waiting producer threads that space has opened up
        notifyAll();
        return message;
    }

    public synchronized int size() {
        return queue.size();
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }
}
