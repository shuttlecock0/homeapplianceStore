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
    @Autowired MessageRepository messageRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverDeliveryStarted_SendMessage(@Payload DeliveryStarted deliveryStarted){

        if(!deliveryStarted.validate()) return;

        System.out.println("\n\n##### listener SendMessage : " + deliveryStarted.toJson() + "\n\n");



        // Logic //
        Message message = new Message();
        message.setOrderId(deliveryStarted.getOrderId());
        message.setCustomerName(deliveryStarted.getCustomerName());
        message.setItemName(deliveryStarted.getItemName());
        message.setDeliveryAddress(deliveryStarted.getDeliveryAddress());
        message.setDeliveryPhoneNumber(deliveryStarted.getDeliveryPhoneNumber());
        message.setOrderStatus(deliveryStarted.getOrderStatus());
        message.setMessage(
            "The delivery of the " + message.getItemName() + " (order No: " +
            message.getOrderId() + ") ordered by " + message.getCustomerName() +
            " has been started. [Shipping to: " + message.getDeliveryAddress() +
            " |Shpping Contact: " + message.getDeliveryPhoneNumber() + " ]"
        );
        messageRepository.save(message);

    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverDeliveryCanceled_SendMessage(@Payload DeliveryCanceled deliveryCanceled){

        if(!deliveryCanceled.validate()) return;

        System.out.println("\n\n##### listener SendMessage : " + deliveryCanceled.toJson() + "\n\n");



        // Logic //
        Message message = new Message();
        message.setOrderId(deliveryCanceled.getOrderId());
        message.setCustomerName(deliveryCanceled.getCustomerName());
        message.setItemName(deliveryCanceled.getItemName());
        message.setDeliveryAddress(deliveryCanceled.getDeliveryAddress());
        message.setDeliveryPhoneNumber(deliveryCanceled.getDeliveryPhoneNumber());
        message.setOrderStatus(deliveryCanceled.getOrderStatus());
        message.setMessage(
            "The delivery of the " + message.getItemName() + " (order No: " +
            message.getOrderId() + ") ordered by " + message.getCustomerName() +
            " has been canceled. [Shipping to: " + message.getDeliveryAddress() +
            " |Shpping Contact: " + message.getDeliveryPhoneNumber() + " ]"
        );
        messageRepository.save(message);

    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
