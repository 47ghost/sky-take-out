package com.sky.repository;


import com.sky.entity.AddressBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface AddressBookRepository extends JpaRepository<AddressBook,Long> {
    List<AddressBook> findByUserId(Long userId);

    List<AddressBook> findByUserIdAndIsDefault(Long userId, Integer isDefault);

    @Modifying
    @Query("update AddressBook a set a.isDefault=:isDefault where  a.userId=:userId")
    void updateIsDefaultByUserId(Long userId, Integer isDefault);

    @Modifying
    @Query("update AddressBook a set a.isDefault=:isDefault where  a.id=:id")
    void setIsDefult(Long id, Integer isDefault);
}
