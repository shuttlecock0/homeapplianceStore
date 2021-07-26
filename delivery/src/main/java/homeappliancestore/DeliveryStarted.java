package homeappliancestore;

public class DeliveryStarted extends AbstractEvent {

    private Long deliveryId;
    private Long orderId;
    private String customerName;
    private String itemName;
    private String 배달주소;
    private String 배달연락처;
    private String orderStatus;

    public DeliveryStarted(){
        super();
    }

    public Long getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(Long deliveryId) {
        this.deliveryId = deliveryId;
    }
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    public String get배달주소() {
        return 배달주소;
    }

    public void set배달주소(String 배달주소) {
        this.배달주소 = 배달주소;
    }
    public String get배달연락처() {
        return 배달연락처;
    }

    public void set배달연락처(String 배달연락처) {
        this.배달연락처 = 배달연락처;
    }
    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }
}
