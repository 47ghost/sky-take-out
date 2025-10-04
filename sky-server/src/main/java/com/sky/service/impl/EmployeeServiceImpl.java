package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.BaseException;
import com.sky.exception.PasswordErrorException;
import com.sky.repository.EmployeeRepository;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.util.Optional;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;



    @Override
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据

        Optional<Employee> emps=employeeRepository.findByUsername(username);



        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (emps.isEmpty()) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        Employee emp=emps.get();

        //密码比对
        if (!passwordEncoder.matches(password,emp.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (emp.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return emp;
    }

    @Override
    public PageResult sarchByPage(EmployeePageQueryDTO pageQueryDTO) {

        int pageInPageHelper=pageQueryDTO.getPage();
        int pageInPageable=pageInPageHelper-1;
        Pageable pageable= PageRequest.of(pageInPageable,pageQueryDTO.getPageSize()
                , Sort.by(Sort.Direction.DESC,"createTime"));
        Page<Employee> emps;

        if(pageQueryDTO.getName()==null|| pageQueryDTO.getName().isEmpty()){
            emps=employeeRepository.findAll(pageable);
        }else {
           emps = employeeRepository.findByNameContaining(pageQueryDTO.getName(), pageable);
        }


        PageResult pageResult=new PageResult();
        pageResult.setTotal(emps.getTotalElements());
        pageResult.setRecords(emps.getContent());
        return pageResult;


    }

    @Override
    public void add(Employee newEmp) {
        newEmp.setPassword(passwordEncoder.encode("123456"));
         employeeRepository.save(newEmp);

    }

    @Override
    public Employee getById(Long id) {
        Optional<Employee> emp=employeeRepository.findById(id);
        if(emp.isEmpty()){
            throw new BaseException("未查询到用户");
        }
        Employee employee=emp.get();
        employee.setPassword("***");
        return  employee;



    }

    @Override
    public void startOrStop(Long id, Integer status) {
        Employee emp= getById(id);
        Integer originStatus=emp.getStatus();
        if(originStatus.compareTo(status)==0){
            throw new BaseException("无法更改员工账号状态");
        }

        emp.setStatus(status);
        employeeRepository.save(emp);

    }

    @Override
    public void updateEmp(EmployeeDTO employeeDTO) {
        Optional<Employee> optionalEmp = employeeRepository.findById(employeeDTO.getId());
        if (optionalEmp.isEmpty()) {
            throw new BaseException("未查询到用户");
        }
        Employee emp = optionalEmp.get();
        String password=emp.getPassword();
        BeanUtils.copyProperties(employeeDTO,emp);
        emp.setUpdateUser(BaseContext.getCurrentId());
        emp.setPassword(password);
        employeeRepository.save(emp);
    }
}
