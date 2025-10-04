package com.sky.repository;

import com.sky.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface CategoryRepository extends JpaRepository<Category,Long> {




    List<Category> findAllByType(Integer type);


    @Modifying
    @Query("update Category c set c.name=:name,c.sort=:sort,c.updateUser=:updateUser,c.updateTime=:updateTime where c.id=:id")
    int updateTypeAndSortById(@Param("id")Long  id, @Param("name")String name, @Param("sort") Integer sort, @Param("updateUser") Long updateUser,@Param("updateTime") LocalDateTime updateTime);

    Page<Category> findByNameContainingAndType(String name, Integer type, Pageable pageable);

    Page<Category> findByType(Integer type, Pageable pageable);


    @Modifying
    @Query("update  Category  c set c.status=:status ,c.updateTime=:updateTime,c.updateUser=:updateUser where c.id=:id")
    int updateStatus(@Param("status") Integer status,@Param("id") Long id,@Param("updateUser") Long updateUser,@Param("updateTime") LocalDateTime updateTime);


    @Query("select c from  Category  c where (:name is null or c.name LIKE CONCAT('%', :name, '%')) and (:type is null or c.type=:type)")
    Page<Category> findByCondition(String name, Integer type, Pageable pageable);
}
