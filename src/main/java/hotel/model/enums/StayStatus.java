package hotel.model.enums;

/** Trạng thái một lượt ở (StayRecord). Giá trị theo mục 1.3 của kế hoạch. */
public enum StayStatus {
    ACTIVE("Đang ở"),
    CHECKED_OUT("Đã trả phòng"),
    ROOM_CHANGED("Đã đổi phòng");

    private final String label;

    StayStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}