package com.sky.repository;

import com.sky.entity.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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
}
