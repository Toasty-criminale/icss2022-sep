package nl.han.ica.datastructures;

public class Node<T> {
    private Node<T> previous;
    private Node<T> next;
    private T value;
    private int index;

    public Node(Node<T> previous, Node<T> next, T value, int index) {
        this.previous = previous;
        this.next = next;
        this.value = value;
        this.index = index;
    }

    public Node<T> getPrevious() {
        return previous;
    }

    public void setPrevious(Node<T> previous) {
        this.previous = previous;
    }

    public Node<T> getNext() {
        return next;
    }

    public void setNext(Node<T> next) {
        this.next = next;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void upIndex() {
        index++;
        if (next != null) next.upIndex();
    }

    public void downIndex() {
        index--;
        if (next != null) next.downIndex();
    }

    public Node<T> findIndex(int i) {
        if (index == i) return this;
        return next == null ? null : next.findIndex(i);
    }
}
