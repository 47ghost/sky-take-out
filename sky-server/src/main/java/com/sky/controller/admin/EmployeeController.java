package com.sky.controller.admin;

import com.sky.constant.JwtClaimsConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.BaseException;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import com.sky.utils.JwtUtil;
import com.sky.vo.EmployeeLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 员工管理
 */
@Tag(name = "员工接口")
@RestController
@RequestMapping("/admin/employee")
@Slf4j
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private JwtProperties jwtProperties;



    //登录
    @Operation(summary = "员工登录")
    @PostMapping("/login")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录：{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        claims.put(JwtClaimsConstant.EMP_NAME,employee.getName());

        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .build();

        return Result.success(employeeLoginVO);
    }


    //登出
    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<String> logout() {
        return Result.success();
    }

    //添加员工
    @Operation(summary = "添加员工")
    @PostMapping
    public Result<Void> addEmp(@RequestBody  EmployeeDTO employeeDTO){
        log.info("开始添加员工");

        Employee newEmp=new Employee();
        BeanUtils.copyProperties(employeeDTO,newEmp);
        newEmp.setStatus(StatusConstant.ENABLE);
        newEmp.setCreateUser(BaseContext.getCurrentId());
        newEmp.setUpdateUser(BaseContext.getCurrentId());
        employeeService.add(newEmp);
        log.info("员工添加成功");
        return Result.success();
    }


    //分页查询
    @Operation(summary = "分页查询")
    @GetMapping("/page")
    public Result<PageResult> searchEmpByPage(EmployeePageQueryDTO pageQueryDTO){
        PageResult empsInpage=  employeeService.sarchByPage(pageQueryDTO);
        log.info("分页查询员工，关键词{}，页码{}，页大小{}",pageQueryDTO.getName(),pageQueryDTO.getPage(),pageQueryDTO.getPageSize());
        return Result.success(empsInpage);
    }

    //按id查询
    @Operation(summary = "按id查询")
    @GetMapping("/{id}")
    public Result<Employee> getById(@PathVariable Long id){
        Employee emp=employeeService.getById(id);
        log.info("查询回显id:{}",id);
        return Result.success(emp);
    }

    //启用禁用
    @Operation(summary = "启用禁用")
    @PostMapping("/status/{status}")
    public Result<Void> startOrStopEmp(Long id,@PathVariable Integer status){
        employeeService.startOrStop(id,status);
        log.info("更改员工(id:{})账号状态为{}",id,status);
        return  Result.success();
    }

    //编辑用户信息
    @Operation(summary = "编辑用户信息")
    @PutMapping
    public Result<Void> updateEmp(@RequestBody  EmployeeDTO employeeDTO){
        try{
        employeeService.updateEmp(employeeDTO);
        log.info("更新员工({})信息",employeeDTO.getUsername());
        return Result.success();}
        catch (Exception ex){
            throw  new BaseException("用户更新失败，"+ex.getMessage());
        }

    }

}
