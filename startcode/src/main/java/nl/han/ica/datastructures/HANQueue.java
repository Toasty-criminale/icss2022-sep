package nl.han.ica.datastructures;

public class HANQueue<T> implements IHANQueue<T> {
    private final HANLinkedList<T> queue = new HANLinkedList<>();

    @Override
    public void enqueue(T value) {
        queue.addLast(value);
    }

    @Override
    public T dequeue() {
        if (isEmpty()) return null;
        T value = queue.getFirst();
        queue.removeFirst();
        return value;
    }

    @Override
    public T peek() {
        return queue.getFirst();
    }

    @Override
    public void clear() {
        queue.clear();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public int getSize() {
        return queue.getSize();
    }
}
