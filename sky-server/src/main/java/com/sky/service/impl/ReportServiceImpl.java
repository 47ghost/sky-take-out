package com.sky.service.impl;


import com.sky.constant.OrdersConstant;
import com.sky.dto.GoodsSalesDTO;
import com.sky.repository.OrderRepository;
import com.sky.repository.UserRepository;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {


    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;


    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList=new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            //日期计算，计算指定日期的后一天对应的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        String stringDateList= StringUtils.join(dateList,",");

        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            //查询date日期对应的营业额数据，营业额是指：状态为“已完成”的订单金额合计
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // select sum(amount) from orders where order_time > beginTime and order_time < endTime and status = 5
            BigDecimal totalAmount=orderRepository.sumAmountByOrderTimeBetweenAndStatus(beginTime,endTime,OrdersConstant.COMPLETED);
            Double total = totalAmount != null ? totalAmount.doubleValue() : 0.0;
            turnoverList.add(total);
        }

        return TurnoverReportVO.builder()
                .dateList(stringDateList)
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();

    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //存放从begin到end之间的每天对应的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);

        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //存放每天的新增用户数量 select count(id) from user where create_time < ? and create_time > ?
        List<Long> newUserList = new ArrayList<>();
        //存放每天的总用户数量 select count(id) from user where create_time < ?
        List<Long> totalUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //总用户数量
            long totalUser= userRepository.countByCreateTimeBefore(endTime);
            //新增用户数量
            long  newUser  =userRepository.countByCreateTimeBetween(beginTime,endTime);
            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }


    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        //存放从begin到end之间的每天对应的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);

        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //存放每天的订单总数
        List<Integer> orderCountList = new ArrayList<>();
        //存放每天的有效订单数
        List<Integer> validOrderCountList = new ArrayList<>();
        //遍历dateList集合，查询每天的有效订单数和订单总数
        for (LocalDate date : dateList) {
            //查询每天的订单总数 select count(id) from orders where order_time > ? and order_time < ?
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            long orderCountlong=orderRepository.countByCondition(beginTime,endTime,null);
            Integer orderCount = (int) orderCountlong;

            //查询每天的有效订单数 select count(id) from orders where order_time > ? and order_time < ? and status = 5
            long validOrderCountlong=orderRepository.countByCondition(beginTime,endTime,OrdersConstant.COMPLETED);
            Integer validOrderCount=(int) validOrderCountlong;

            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }



        //计算时间区间内的订单总数量
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();

        //计算时间区间内的有效订单数量
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0){
            //计算订单完成率
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }

        return  OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .orderCountList(StringUtils.join(orderCountList,","))
                .validOrderCountList(StringUtils.join(validOrderCountList,","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        Integer status = 5; // 已完成状态
        int topN = 10;

        // 创建分页请求对象，第0页，每页10条数据。排序已在@Query中定义。
        Pageable pageable = PageRequest.of(0, topN);

        List<GoodsSalesDTO> salesTop10 = orderRepository.findSalesTopN(beginTime,endTime,OrdersConstant.COMPLETED,pageable);
        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");

        List<Long> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");

        //封装返回结果数据
        return SalesTop10ReportVO
                .builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }
}
