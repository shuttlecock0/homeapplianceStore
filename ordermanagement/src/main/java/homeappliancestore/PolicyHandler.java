package homeappliancestore;

import homeappliancestore.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired OrdermgmtRepository ordermgmtRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPayApproved_TakeOrderInfo(@Payload PayApproved payApproved){

        if(!payApproved.validate()) return;

        System.out.println("\n\n##### listener TakeOrderInfo : " + payApproved.toJson() + "\n\n");

        Ordermgmt ordermgmt = new Ordermgmt();
        ordermgmt.setOrderId(payApproved.getOrderId());
        ordermgmt.setCustomerId(payApproved.getCustomerId());
        ordermgmt.setCustomerName(payApproved.getCustomerName());
        ordermgmt.setItemId(payApproved.getItemId());
        ordermgmt.setItemName(payApproved.getItemName());
        ordermgmt.setQty(payApproved.getQty());
        ordermgmt.setItemPrice(payApproved.getItemPrice());
        ordermgmt.setDeliveryAddress(payApproved.getDeliveryAddress());
        ordermgmt.setDeliveryPhoneNumber(payApproved.getDeliveryPhoneNumber());
        ordermgmt.setOrderStatus(payApproved.getOrderStatus());
        ordermgmtRepository.save(ordermgmt);
        

    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverOrderCanceled_CancelOrder(@Payload OrderCanceled orderCanceled){

        if(!orderCanceled.validate()) return;

        System.out.println("\n\n##### listener CancelOrder : " + orderCanceled.toJson() + "\n\n");

        ordermgmtRepository.findByOrderId(orderCanceled.getOrderId()).ifPresent(ordermgmt->{
            ordermgmt.setOrderStatus("orderCanceled");
            ordermgmtRepository.save(ordermgmt);
        });

    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
