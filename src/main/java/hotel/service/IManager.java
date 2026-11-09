package hotel.service;

public interface IManager<T> {
    boolean add(T item);
    boolean update(T item);
    boolean remove(String id);
    T findById(String id); // trả về một đối tượng thuộc kiểu T
    void displayAll();
}
