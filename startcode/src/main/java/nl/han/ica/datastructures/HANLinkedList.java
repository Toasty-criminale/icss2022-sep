package nl.han.ica.datastructures;

public class HANLinkedList<T> implements IHANLinkedList<T> {
    private Node<T> head;
    private Node<T> tail;
    private int size;

    public HANLinkedList() {
        head = null;
        tail = null;
        size = 0;
    }

    @Override
    public void addFirst(T value) {
        Node<T> node = new Node<>(null, head, value, 0);
        if (head == null) {
            tail = node;
        } else {
            head.setPrevious(node);
            updateIndices(head, 1);
        }
        head = node;
        size++;
    }

    public void addLast(T value) {
        if (tail == null) {
            addFirst(value);
            return;
        }
        Node<T> node = new Node<>(tail, null, value, size);
        tail.setNext(node);
        tail = node;
        size++;
    }

    public void add(T value) {
        addLast(value);
    }

    @Override
    public void insert(int index, T value) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
        if (index == 0) {
            addFirst(value);
            return;
        }
        if (index == size) {
            addLast(value);
            return;
        }
        Node<T> current = nodeAt(index);
        Node<T> node = new Node<>(current.getPrevious(), current, value, index);
        current.getPrevious().setNext(node);
        current.setPrevious(node);
        updateIndices(current, 1);
        size++;
    }

    @Override
    public void delete(int index) {
        Node<T> node = nodeAt(index);
        Node<T> previous = node.getPrevious();
        Node<T> next = node.getNext();
        if (previous == null) head = next;
        else previous.setNext(next);
        if (next == null) tail = previous;
        else {
            next.setPrevious(previous);
            updateIndices(next, -1);
        }
        node.setPrevious(null);
        node.setNext(null);
        size--;
    }

    public void deleteFirst() {
        removeFirst();
    }

    public void deleteLast() {
        if (size != 0) delete(size - 1);
    }

    @Override
    public void removeFirst() {
        if (size != 0) delete(0);
    }

    @Override
    public T get(int pos) {
        return nodeAt(pos).getValue();
    }

    @Override
    public T getFirst() {
        return head == null ? null : head.getValue();
    }

    public T getLast() {
        return tail == null ? null : tail.getValue();
    }

    @Override
    public int getSize() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public void clear() {
        head = null;
        tail = null;
        size = 0;
    }

    private Node<T> nodeAt(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
        Node<T> current;
        if (index < size / 2) {
            current = head;
            for (int i = 0; i < index; i++) current = current.getNext();
        } else {
            current = tail;
            for (int i = size - 1; i > index; i--) current = current.getPrevious();
        }
        return current;
    }

    private void updateIndices(Node<T> first, int difference) {
        Node<T> current = first;
        while (current != null) {
            current.setIndex(current.getIndex() + difference);
            current = current.getNext();
        }
    }
}
