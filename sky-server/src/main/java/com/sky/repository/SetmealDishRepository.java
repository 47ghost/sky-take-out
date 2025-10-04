package com.sky.repository;


import com.sky.entity.SetmealDish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SetmealDishRepository extends JpaRepository<SetmealDish,Long> {

    int countByDishIdIn(List<Long> ids);

    List<SetmealDish> findBySetmealIdIn(Collection<Long> setmealIds);


    void deleteBySetmealIdIn(List<Long> ids);

    void deleteBySetmealId(Long id);


    //联查实现前的旧方法
    List<SetmealDish> findBySetmealId(Long id);


    @Query("SELECT sd FROM SetmealDish sd JOIN FETCH sd.dish WHERE sd.setmealId = :setmealId")
    List<SetmealDish> findBySetmealIdWithDish(@Param("setmealId")Long setmealId);
}
