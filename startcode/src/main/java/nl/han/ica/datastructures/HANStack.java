package nl.han.ica.datastructures;

public class HANStack<T> implements IHANStack<T> {
    private final HANLinkedList<T> stack = new HANLinkedList<>();

    @Override
    public void push(T value) {
        stack.addLast(value);
    }

    @Override
    public T pop() {
        if (isEmpty()) return null;
        T value = stack.getLast();
        stack.deleteLast();
        return value;
    }

    @Override
    public T peek() {
        return stack.getLast();
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public int getSize() {
        return stack.getSize();
    }
}
