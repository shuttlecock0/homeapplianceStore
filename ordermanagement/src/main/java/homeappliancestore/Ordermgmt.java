package homeappliancestore;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Ordermgmt_table")
public class Ordermgmt {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long orderMgmtId;
    private Long orderId;
    private Long customerId;
    private String customerName;
    private Long itemId;
    private String itemName;
    private Integer qty;
    private Integer itemPrice;
    private String deliveryAddress;
    private String deliveryPhoneNumber;
    private String orderStatus;

    @PostPersist
    public void onPostPersist(){
    }

    @PostUpdate
    public void onPostUpdate(){
        if (this.orderStatus.equals("orderTaken")) {
            OrderTaken orderTaken = new OrderTaken();
            BeanUtils.copyProperties(this, orderTaken);
            orderTaken.publishAfterCommit();
        }
        else if (this.orderStatus.equals("orderCanceled")) {
            CancelOrderTaken cancelOrderTaken = new CancelOrderTaken();
            BeanUtils.copyProperties(this, cancelOrderTaken);
            cancelOrderTaken.publishAfterCommit();
        }
    }
    @PrePersist
    public void onPrePersist(){
    }

    public Long getOrderMgmtId() {
        return orderMgmtId;
    }

    public void setOrderMgmtId(Long orderMgmtId) {
        this.orderMgmtId = orderMgmtId;
    }
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }
    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }
    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }
    public Integer getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(Integer itemPrice) {
        this.itemPrice = itemPrice;
    }
    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }
    public String getDeliveryPhoneNumber() {
        return deliveryPhoneNumber;
    }

    public void setDeliveryPhoneNumber(String deliveryPhoneNumber) {
        this.deliveryPhoneNumber = deliveryPhoneNumber;
    }
    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }




}
