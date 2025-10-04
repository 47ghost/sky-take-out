package com.sky.repository;

import com.sky.entity.ShoppingCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ShoppingCartRepository extends JpaRepository<ShoppingCart,Long> {



    @Query("select s from ShoppingCart  s where (:userId is null or :userId=s.userId) and (:dishId is null or :dishId=s.dishId) and(:setmealId is null or :setmealId=s.setmealId) and(:dishFlavor is null  or :dishFlavor=s.dishFlavor)")
    List<ShoppingCart> findByCondition(Long userId, Long dishId, Long setmealId, String dishFlavor);

    List<ShoppingCart> findByUserId(Long userId);


    void deleteByUserId(Long userId);
}
