package com.sky.service.impl;

import com.sky.constant.OrdersConstant;
import com.sky.constant.StatusConstant;
import com.sky.repository.DishRepository;
import com.sky.repository.OrderRepository;
import com.sky.repository.SetmealRepository;
import com.sky.repository.UserRepository;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;


@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DishRepository dishRepository;

    @Autowired
    private SetmealRepository setmealRepository;

    @Override
    public BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end) {

        //1.营业额
        BigDecimal totalAmount=orderRepository.sumAmountByOrderTimeBetweenAndStatus(begin,end, OrdersConstant.COMPLETED);
        Double total = totalAmount != null ? totalAmount.doubleValue() : 0.0;

        //2.有效订单
        long l1 = orderRepository.countByCondition(begin, end, OrdersConstant.COMPLETED);

        //3.订单完成率
        //4.平均客单价
        long l2 =orderRepository.countByCondition(begin, end,null);
        Double unitPrice = 0.0;
        Double orderCompletionRate = 0.0;
        if(l2 != 0 && l1!=0){
            //计算订单完成率
            orderCompletionRate =(double) l1 / l2;
            unitPrice=total/l1;
        }

        //5.新增用户数
        long  newUser  =userRepository.countByCreateTimeBetween(begin,end);





        return BusinessDataVO.builder()
                .turnover(total)
                .validOrderCount((int)l1)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers((int)newUser)
                .build();
    }

    @Override
    public OrderOverViewVO getOrderOverView() {
        LocalDateTime begin=LocalDateTime.now().with(LocalTime.MIN);
        long waitingOrders = orderRepository.countByCondition(begin, null, OrdersConstant.TO_BE_CONFIRMED);
        long deliveredOrders=orderRepository.countByCondition(begin, null, OrdersConstant.CONFIRMED);
        long completedOrders=orderRepository.countByCondition(begin, null, OrdersConstant.COMPLETED);
        long cancelledOrders=orderRepository.countByCondition(begin, null, OrdersConstant.CANCELLED);
        long allOrders=orderRepository.countByCondition(begin, null, null);

        return OrderOverViewVO.builder()
                .waitingOrders((int)waitingOrders)
                .deliveredOrders((int)deliveredOrders)
                .completedOrders((int)completedOrders)
                .cancelledOrders((int)cancelledOrders)
                .allOrders((int)allOrders)
                .build();
    }

    @Override
    public DishOverViewVO getDishOverView() {
        long lenable = dishRepository.countByStatus(StatusConstant.ENABLE);
        long lunable=dishRepository.countByStatus(StatusConstant.DISABLE);
        return DishOverViewVO.builder()
                .sold((int)lenable)
                .discontinued((int)lunable)
                .build();
    }

    @Override
    public SetmealOverViewVO getSetmealOverView() {
        long sold=  setmealRepository.countByStatus(StatusConstant.ENABLE);
        long  discontinued=  setmealRepository.countByStatus(StatusConstant.DISABLE);
        return SetmealOverViewVO.builder()
                .sold((int)sold)
                .discontinued((int)discontinued)
                .build();
    }
}
