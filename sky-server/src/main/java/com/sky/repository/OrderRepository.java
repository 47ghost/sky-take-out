package com.sky.repository;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface OrderRepository extends JpaRepository<Orders,Long> {


    @Modifying
    @Query("update  Orders  o set o.payStatus=:payStatus,o.status=:status,o.checkoutTime=:checkoutTime  where  o.number=:number")
    void updatePayment(Integer payStatus, Integer status, LocalDateTime checkoutTime, String number);


    Page<Orders> findAll(Specification<Orders> spec, Pageable pageable);

    Integer countByStatus(Integer status);

    @Modifying
    @Query("update Orders o set o.status=:status where o.id=:id")
    void updateStatus(Long id, Integer status);

    List<Orders> findByStatusAndOrderTimeBefore(Integer status, LocalDateTime orderTimeBefore);


    Optional<Orders> findByNumber(String number);




    @Query("SELECT SUM(o.amount) FROM Orders o " +
            "WHERE o.orderTime > :beginTime " +
            "AND o.orderTime < :endTime " +
            "AND o.status = :status")
    BigDecimal sumAmountByOrderTimeBetweenAndStatus(@Param("beginTime")LocalDateTime beginTime, @Param("endTime")LocalDateTime endTime,@Param("status") Integer status);

    @Query("select count(o) from Orders o where (:beginTime is null or o.orderTime> :beginTime) and(:endTime is null  or o.orderTime < :endTime ) and(:status is null or o.status = :status)")
    long countByCondition(@Param("beginTime")LocalDateTime beginTime, @Param("endTime")LocalDateTime endTime,@Param("status") Integer status);



    @Query("select new com.sky.dto.GoodsSalesDTO(od.name,sum(od.number)) from Orders o,OrderDetail od where o.id=od.orderId and o.status=:status and o.orderTime >:begin and o.orderTime<:end group by od.name order by sum(od.number) desc ")
    List<GoodsSalesDTO> findSalesTopN(
            @Param("begin") LocalDateTime begin,
            @Param("end") LocalDateTime end,
            @Param("status") Integer status,
            Pageable pageable);
}
