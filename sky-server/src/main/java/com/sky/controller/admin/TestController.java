package com.sky.controller.admin;


import com.sky.entity.Dish;
import com.sky.repository.DishRepository;
import com.sky.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "测试接口")
@RestController
@Slf4j
public class TestController {

    @Autowired
    private DishRepository dishRepository;

    @GetMapping("/test")
    public Result<String> loginTest(){
        return Result.success("hello,world");
    }


    @Operation(summary = "菜品查询测试")
    @GetMapping("/dishtest")
    public Result<List<Dish>> listDish(){
        return Result.success(dishRepository.findAll());
    }
}
