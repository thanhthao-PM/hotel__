package hotel.model;

import java.util.Arrays;

/**
 * Quản lý danh sách Tiền cọc (Deposit) bằng mảng tĩnh thuần.
 */
public class DepositList {
    private static final int DEFAULT_CAPACITY = 100;

    private final Deposit[] deposits;
    private int size;

    public DepositList() {
        this(DEFAULT_CAPACITY);
    }

    public DepositList(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Kích thước mảng phải lớn hơn 0");
        }
        deposits = new Deposit[capacity];
        size = 0;
    }

    public void add(Deposit deposit) {
        if (deposit == null) {
            throw new IllegalArgumentException("Deposit không được là null");
        }
        if (size == deposits.length) {
            throw new IllegalStateException("Danh sách tiền cọc đã đầy");
        }
        if (findById(deposit.getDepositId()) != null) {
            throw new IllegalArgumentException("Mã Deposit đã tồn tại: " + deposit.getDepositId());
        }
        deposits[size++] = deposit;
    }

    public Deposit get(int index) {
        checkIndex(index);
        return deposits[index];
    }

    public Deposit findById(String depositId) {
        for (int i = 0; i < size; i++) {
            if (depositId == null ? deposits[i].getDepositId() == null
                    : depositId.equalsIgnoreCase(deposits[i].getDepositId())) {
                return deposits[i];
            }
        }
        return null;
    }

    public boolean removeById(String depositId) {
        for (int i = 0; i < size; i++) {
            if (depositId == null ? deposits[i].getDepositId() == null
                    : depositId.equalsIgnoreCase(deposits[i].getDepositId())) {
                int elementsToMove = size - i - 1;
                if (elementsToMove > 0) {
                    System.arraycopy(deposits, i + 1, deposits, i, elementsToMove);
                }
                deposits[--size] = null;
                return true;
            }
        }
        return false;
    }

    public Deposit[] getAll() {
        return Arrays.copyOf(deposits, size);
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        Arrays.fill(deposits, 0, size, null);
        size = 0;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Vị trí không hợp lệ: " + index + ", size: " + size);
        }
    }
}