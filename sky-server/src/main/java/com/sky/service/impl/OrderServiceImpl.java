package com.sky.service.impl;


import com.alibaba.fastjson.JSON;
import com.sky.constant.MessageConstant;
import com.sky.constant.OrdersConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.BaseException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.repository.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebsocketServer;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AddressBookRepository addressBookRepository;

    @Autowired
    private ShoppingCartRepository shoppingCartRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private WebsocketServer websocketServer;

    @Transactional
    @Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //1. 处理各种业务异常（地址簿为空、购物车数据为空）
        AddressBook addressBook=addressBookRepository.findById(ordersSubmitDTO.getAddressBookId())
                .orElseThrow(()-> new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL));
        List<ShoppingCart> shoppingCartList=shoppingCartRepository.findByUserId(BaseContext.getCurrentId());
        if(shoppingCartList == null || shoppingCartList.isEmpty()){
            //抛出业务异常
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //2. 向订单表插入1条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(OrdersConstant.UN_PAID);
        orders.setStatus(OrdersConstant.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));//订单号
        orders.setAddress(addressBook.getDetail());
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(BaseContext.getCurrentId());

        orderRepository.save(orders);

        //3. 向订单明细表插入n条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();//订单明细
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setId(null); // 确保这是一个新的实体
            orderDetail.setOrderId(orders.getId());//设置当前订单明细关联的订单id
            orderDetailList.add(orderDetail);
        }
        orderDetailRepository.saveAll(orderDetailList);

        //4. 清空当前用户的购物车数据
        shoppingCartRepository.deleteByUserId(BaseContext.getCurrentId());


        //5. 封装VO返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();

                return orderSubmitVO;

    }


    @Transactional
    @Override
    public void paySuccess(String orderNumber) {
        LocalDateTime checkoutTime=LocalDateTime.now();
        orderRepository.updatePayment(OrdersConstant.PAID,OrdersConstant.TO_BE_CONFIRMED,checkoutTime,orderNumber);

        Orders ordersDB=orderRepository.findByNumber(orderNumber)
                .orElseThrow(()-> new BaseException("未找到订单"));
        Map<String,Object> map=new HashMap<>();
        map.put("type",1);// 1表示来单提醒 2表示客户催单
        map.put("orderId",ordersDB.getId());
        map.put("content","订单号：" + orderNumber);

        String json = JSON.toJSONString(map);
        websocketServer.sendToAllClient(json);


    }

    @Override
    public PageResult pageQuery4User(int page, int pageSize, Integer status) {

        Long userId=BaseContext.getCurrentId();
        Specification<Orders> spec=(root, query, cb)->{
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (BaseContext.getCurrentId() != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));


        };

        int pageInPagebale=page-1;
        Pageable pageable= PageRequest.of(pageInPagebale,pageSize,
                Sort.by(Sort.Direction.DESC,"orderTime"));
        Page<Orders> ordersPage =orderRepository.findAll(spec,pageable);
        List<Orders> ordersList=ordersPage.getContent();

        List<OrderVO> list = new ArrayList();
        // 查询出订单明细，并封装入OrderVO进行响应
        if (ordersList!=null && !ordersList.isEmpty()) {
            for (Orders orders : ordersList) {
                Long orderId = orders.getId();// 订单id

                // 查询订单明细
                List<OrderDetail> orderDetails = orderDetailRepository.findByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);

                list.add(orderVO);
            }
        }
        return new PageResult(ordersPage.getTotalElements(), list);

    }

    @Override
    public OrderVO details(Long id) {
        Orders orders=orderRepository.findById(id).orElseThrow(()->new BaseException("未找到订单"));
        List<OrderDetail> orderDetails = orderDetailRepository.findByOrderId(id);

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetails);

        return orderVO;
    }

    @Transactional
    @Override
    public void userCancelById(Long id) {
        Orders ordersDB=orderRepository.findById(id)
                .orElseThrow(()-> new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND));
        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 订单处于待接单状态下取消，需要进行退款
        if (ordersDB.getStatus().equals(OrdersConstant.TO_BE_CONFIRMED)) {
/*
            //调用微信支付退款接口
            weChatPayUtil.refund(
                    ordersDB.getNumber(), //商户订单号
                    ordersDB.getNumber(), //商户退款单号
                    new BigDecimal(0.01),//退款金额，单位 元
                    new BigDecimal(0.01));//原订单金额
*/
            //支付状态修改为 退款
            ordersDB.setPayStatus(OrdersConstant.REFUND);
        }

        // 更新订单状态、取消原因、取消时间
        ordersDB.setStatus(OrdersConstant.CANCELLED);
        ordersDB.setCancelReason("用户取消");
        ordersDB.setCancelTime(LocalDateTime.now());

        orderRepository.save(ordersDB);




    }


    @Transactional
    @Override
    public void repetition(Long id) {
        List<OrderDetail> orderDetailList=orderDetailRepository.findByOrderId(id);

        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(BaseContext.getCurrentId());
            //交给@CreationTimestamp完成
            shoppingCart.setCreateTime(null);
            shoppingCart.setId(null);

            return shoppingCart;
        }).toList();
        shoppingCartRepository.saveAll(shoppingCartList);

    }

    //以下为管理端方法


    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        int pageInPagebale=ordersPageQueryDTO.getPage()-1;
        int pageSize=ordersPageQueryDTO.getPageSize();
        Pageable pageable= PageRequest.of(pageInPagebale,pageSize,
                Sort.by(Sort.Direction.DESC,"orderTime"));
        Specification<Orders> spec=(root, query, cb)->{
            List<Predicate> predicates = new ArrayList<>();
            if (ordersPageQueryDTO.getNumber()!= null) {
                predicates.add(cb.like(root.get("number"), "%" + ordersPageQueryDTO.getNumber() + "%"));
            }
            if (ordersPageQueryDTO.getPhone()!=null && !ordersPageQueryDTO.getPhone().isEmpty()) {
                predicates.add(cb.like(root.get("phone"), "%" + ordersPageQueryDTO.getPhone() + "%"));
            }
            if (ordersPageQueryDTO.getUserId()!=null ) {
                predicates.add(cb.equal(root.get("userId"), ordersPageQueryDTO.getUserId()));
            }
            if (ordersPageQueryDTO.getStatus()!=null ) {
                predicates.add(cb.equal(root.get("status"), ordersPageQueryDTO.getStatus()));
            }
            if (ordersPageQueryDTO.getBeginTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderTime"),ordersPageQueryDTO.getBeginTime()));
            }
            if (ordersPageQueryDTO.getEndTime()!=null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("orderTime"), ordersPageQueryDTO.getEndTime()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));

        };

        Page<Orders> ordersPage =orderRepository.findAll(spec,pageable);

        // 需要返回订单菜品信息，自定义OrderVO响应结果
        List<OrderVO> orderVOList=getOrderVOList(ordersPage.getContent());

        return new PageResult(ordersPage.getTotalElements(),orderVOList);
    }
    // 需要返回订单菜品信息，自定义OrderVO响应结果
    private List<OrderVO> getOrderVOList(List<Orders> ordersList) {

        List<OrderVO> orderVOList = new ArrayList<>();

        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                // 将共同字段复制到OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);

                // 将订单菜品信息封装到orderVO中，并添加到orderVOList
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    //根据订单id获取菜品信息字符串
    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailRepository.findByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).toList();

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }

    @Override
    public OrderStatisticsVO statistics() {
        // 根据状态，分别查询出待接单、待派送、派送中的订单数量
        Integer toBeConfirmed=orderRepository.countByStatus(OrdersConstant.TO_BE_CONFIRMED);
        Integer confirmed = orderRepository.countByStatus(OrdersConstant.CONFIRMED);
        Integer deliveryInProgress = orderRepository.countByStatus(OrdersConstant.DELIVERY_IN_PROGRESS);

        // 将查询出的数据封装到orderStatisticsVO中响应
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }


    @Transactional
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders ordersDB = orderRepository.findById(ordersConfirmDTO.getId())
                .orElseThrow(()->new OrderBusinessException("未找到订单"));

        if(!Objects.equals(ordersDB.getStatus(), OrdersConstant.TO_BE_CONFIRMED)){
            throw new BaseException("订单未付款，无法确认");
        }


        if (ordersConfirmDTO.getId()!=null){
        orderRepository.updateStatus(ordersConfirmDTO.getId(),OrdersConstant.CONFIRMED);
        }
    }

    @Transactional
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        // 根据id查询订单
        Orders ordersDB = orderRepository.findById(ordersRejectionDTO.getId())
                .orElseThrow(()->new OrderBusinessException("未找到订单"));

        // 订单只有存在且状态为2（待接单）才可以拒单
        if (!ordersDB.getStatus().equals(OrdersConstant.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //若用户已支付，需要退款
        Integer payStatus = ordersDB.getPayStatus();
        if (Objects.equals(payStatus, OrdersConstant.PAID)) {
          /*      String refund = weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
            log.info("申请退款：{}", refund);*/
            System.out.println("需要给用户退款");
            ordersDB.setPayStatus(OrdersConstant.REFUND);
        }
        // 拒单需要退款，根据订单id更新订单状态、拒单原因、取消时间
        ordersDB.setStatus(OrdersConstant.CANCELLED);
        ordersDB.setCancelTime(LocalDateTime.now());
        ordersDB.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orderRepository.save(ordersDB);
    }

    @Transactional
    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        // 根据id查询订单
        Orders ordersDB = orderRepository.findById(ordersCancelDTO.getId())
                .orElseThrow(()->new OrderBusinessException("未找到订单"));

        //支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (Objects.equals(payStatus, OrdersConstant.PAID)) {
            //用户已支付，需要退款
/*            String refund = weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
            log.info("申请退款：{}", refund);*/
            System.out.println("需要给用户退款");
            ordersDB.setPayStatus(OrdersConstant.REFUND);
        }

        ordersDB.setId(ordersCancelDTO.getId());
        ordersDB.setStatus(OrdersConstant.CANCELLED);
        ordersDB.setCancelReason(ordersCancelDTO.getCancelReason());
        ordersDB.setCancelTime(LocalDateTime.now());

        orderRepository.save(ordersDB);

    }


    @Transactional
    @Override
    public void delivery(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderRepository.findById(id)
                .orElseThrow(()->new OrderBusinessException("未找到订单"));

        // 校验订单是否存在，并且状态为3
        if (!ordersDB.getStatus().equals(OrdersConstant.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        orderRepository.updateStatus(id,OrdersConstant.DELIVERY_IN_PROGRESS);

    }

    @Transactional
    @Override
    public void complete(Long id) {

        // 根据id查询订单
        Orders ordersDB = orderRepository.findById(id)
                .orElseThrow(()->new OrderBusinessException("未找到订单"));

        // 校验订单是否存在，并且状态为4
        if (!ordersDB.getStatus().equals(OrdersConstant.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        ordersDB.setStatus(OrdersConstant.COMPLETED);
        ordersDB.setDeliveryTime(LocalDateTime.now());
        orderRepository.save(ordersDB);

    }

    @Override
    public void reminder(Long id) {

        Orders ordersDB = orderRepository.findById(id)
                .orElseThrow(()->new OrderBusinessException("未找到订单"));

        Map<String,Object> map = new HashMap();
        map.put("type",2); //1表示来单提醒 2表示客户催单
        map.put("orderId",id);
        map.put("content","订单号：" + ordersDB.getNumber());

        //通过websocket向客户端浏览器推送消息
        websocketServer.sendToAllClient(JSON.toJSONString(map));

    }
}
