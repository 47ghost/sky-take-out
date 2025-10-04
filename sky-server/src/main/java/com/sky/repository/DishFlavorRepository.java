package com.sky.repository;


import com.sky.entity.DishFlavor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DishFlavorRepository extends JpaRepository<DishFlavor,Long> {

    void deleteByDishIdIn(List<Long> ids);

    List<DishFlavor> findByDishId(Long dishId);

    void deleteByDishId(Long id);
}
