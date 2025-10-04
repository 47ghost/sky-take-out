package com.sky.task;


import com.sky.constant.OrdersConstant;
import com.sky.entity.Orders;
import com.sky.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class OrderTask {

    @Autowired
    private OrderRepository orderRepository;


    private static  final  long PAY_TIME= 3;

    @Transactional
    @Scheduled(cron = "0 * * * * ? ") //每分钟触发一次
    public void processTimeoutOrder(){
        log.info("定时处理超时订单：{}", LocalDateTime.now());

        LocalDateTime time = LocalDateTime.now().minusMinutes(PAY_TIME);

        // select * from orders where status = ? and order_time < (当前时间 - ?分钟)
        List<Orders> ordersList = orderRepository.findByStatusAndOrderTimeBefore(OrdersConstant.PENDING_PAYMENT, time);
        log.info("查询到的未支付订单数为{}",ordersList.size());

        if(ordersList != null && !ordersList.isEmpty()){
            for (Orders orders : ordersList) {
                orders.setStatus(OrdersConstant.CANCELLED);
                orders.setCancelReason("订单超时，自动取消");
                orders.setCancelTime(LocalDateTime.now());
            }
            orderRepository.saveAll(ordersList);
        }
    }

    @Transactional
    @Scheduled(cron = "0 0 1 * * ?") //每天凌晨1点触发一次
    public void processDeliveryOrder(){
        log.info("定时处理处于派送中的订单：{}",LocalDateTime.now());

        LocalDateTime time = LocalDateTime.now().minusMinutes(60);

        List<Orders> ordersList = orderRepository.findByStatusAndOrderTimeBefore(OrdersConstant.DELIVERY_IN_PROGRESS, time);
        log.info("查询到的过日订单数为{}",ordersList.size());

        if(ordersList != null && !ordersList.isEmpty()){
            for (Orders orders : ordersList) {
                orders.setStatus(OrdersConstant.COMPLETED);
            }
            orderRepository.saveAll(ordersList);
        }
    }


}
