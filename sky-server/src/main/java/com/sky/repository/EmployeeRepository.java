package com.sky.repository;


import com.sky.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee,Long> {



    Optional<Employee> findByUsername(String username);


    Page<Employee> findByNameContaining(String name, Pageable pageable);
}
