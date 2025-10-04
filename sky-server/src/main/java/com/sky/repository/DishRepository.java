package com.sky.repository;


import com.sky.entity.Dish;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface DishRepository extends JpaRepository<Dish,Long> {
    Long countByCategoryId(Long categoryId);


    @Query("select d from Dish  d JOIN FETCH  d.category where  d.categoryId=:categoryId")
    Page<Dish> findAllByCategoryId(Long categoryId, Pageable pageable);


    @Query("select d from Dish  d JOIN FETCH  d.category where  d.categoryId=:categoryId and d.name LIKE CONCAT('%', :name, '%')")
    Page<Dish> findByNameAndCategoryId(String name, Long categoryId, Pageable pageable);


    @Query("select d from Dish  d JOIN FETCH d.category where (:name is null or d.name LIKE CONCAT('%', :name, '%')) and (:status is null or d.status=:status) and (:categoryId is null or d.categoryId=:categoryId) ")
    Page<Dish> findByCondition(String name, Integer status, Long categoryId, Pageable pageable);

    List<Dish> findByIdIn(List<Long> ids);

    void deleteByIdIn(List<Long> ids);

    List<Dish> findByCategoryId(Long categoryId);

    List<Dish> findByCategoryIdAndStatus(Long categoryId, Integer status);
}
