package com.sky.security;


import com.sky.entity.Employee;
import com.sky.exception.BaseException;
import com.sky.repository.EmployeeRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.Optional;


@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private HandlerExceptionResolver handlerExceptionResolver;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("UserDetailsServiceImpl进行验证");

        Optional<Employee> employee=employeeRepository.findByUsername(username);
        if(employee.isEmpty()){
            log.error("security未找到用户");
            BaseException exception=new BaseException("security未找到用户");
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
            HttpServletResponse response = attributes != null ? attributes.getResponse() : null;
            handlerExceptionResolver.resolveException(request,response,null,exception);
        }

        UserDetails userDetails=new User(employee.get().getUsername(),employee.get().getPassword(),AuthorityUtils.createAuthorityList("ROLE_USER"));
        return  userDetails;
    }
}
