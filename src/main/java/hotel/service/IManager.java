package hotel.service;

import java.util.ArrayList;
// CHỨC NĂNG: Quy định các chức năng quản lý chung cho các Manager
// Bao gồm thêm, sửa, xóa, tìm kiếm và lấy danh sách đối tượng.

public interface IManager<T> {
    boolean add(T item);
    boolean update(T item);
    boolean remove(String id);
    T findById(String id); // trả về một đối tượng thuộc kiểu T
    ArrayList<T> getAll();
}
