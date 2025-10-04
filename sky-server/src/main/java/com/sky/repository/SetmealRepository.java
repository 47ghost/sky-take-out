package com.sky.repository;


import com.sky.entity.Setmeal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SetmealRepository extends JpaRepository<Setmeal,Long> {
    Long countByCategoryId(Long categoryId);


    @Query("select s from Setmeal s join fetch  s.category where (:name is null or :name = '' or s.name like concat('%', :name, '%')) and (:categoryId is null or s.categoryId = :categoryId) and (:status is null or s.status = :status)")
    Page<Setmeal> findByCondition(@Param("name")String name,@Param("categoryId") Long categoryId,@Param("status") Integer status, Pageable pageable);


    void deleteByIdIn(List<Long> ids);

    List<Setmeal> findByCategoryIdAndStatus(Long categoryId, Integer status);

    long countByStatus(Integer status);
}
