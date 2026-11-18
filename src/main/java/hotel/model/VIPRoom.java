    package hotel.model;

    import hotel.model.enums.RoomStatus;
    import hotel.util.CheckingExpression;
    import hotel.util.FileUtil;

    // Phòng VIP: kế thừa Room, tính giá có phụ thu
    public class VIPRoom extends Room {

        private double surchargeRate;  // hệ số phụ thu, ví dụ 0.3 = phụ thu 30%
        private String amenities;      // tiện nghi
        private String viewType;       // hướng nhìn

        // Constructor đầy đủ
        public VIPRoom(String roomId, String roomNumber, int floor, int capacity,
                    double basePrice, RoomStatus status, boolean active,
                    double surchargeRate, String amenities, String viewType) {
            super(roomId, roomNumber, floor, capacity, basePrice, status, active);
            // hệ số phụ thu không được âm
            CheckingExpression.checkNonNegativeDouble(surchargeRate, "Hệ số phụ thu không được âm");
            // hướng nhìn không được để trống
            CheckingExpression.checkNotNullOrEmpty(viewType, "Hướng nhìn không được để trống");
            this.surchargeRate = surchargeRate;
            this.amenities = amenities;
            this.viewType = viewType;
        }

        // Ghi đè: giá = giá gốc x (1 + hệ số phụ thu) x số đêm, làm tròn đến đồng
        @Override
        public double calculateRoomPrice(int nights) {
            // số đêm phải dương
            CheckingExpression.checkPositiveInt(nights, "Số đêm phải > 0");
            return Math.round(getBasePrice() * (1 + surchargeRate) * nights);
        }

        // Ghi đè: thêm thông tin riêng của phòng VIP vào chuỗi thông tin
        @Override
        public String getRoomInfo() {
            return "[VIP]      " + super.getRoomInfo()
                    + String.format(" | Phụ thu %.0f%% | View: %s | Tiện nghi: %s",
                    surchargeRate * 100, viewType, amenities);
        }

        // Ghi đè: một dòng file của phòng VIP
        @Override
        public String toFileString() {
            return FileUtil.join("VIP", baseFileString(), surchargeRate,
                    FileUtil.clean(amenities), FileUtil.clean(viewType));
        }

        public double getSurchargeRate() { return surchargeRate; }
        public String getAmenities() { return amenities; }
        public String getViewType() { return viewType; }
        public void setSurchargeRate(double surchargeRate) { 
            // hệ số phụ thu không được âm
            CheckingExpression.checkNonNegativeDouble(surchargeRate, "Hệ số phụ thu không được âm");
            this.surchargeRate = surchargeRate; }
        public void setAmenities(String amenities) { this.amenities = amenities; }
        public void setViewType(String viewType) { 
            // hướng nhìn không được để trống
            CheckingExpression.checkNotNullOrEmpty(viewType, "Hướng nhìn không được để trống");
            this.viewType = viewType; }
    }