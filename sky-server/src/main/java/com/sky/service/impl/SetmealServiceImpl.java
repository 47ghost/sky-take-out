package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.BaseException;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.repository.DishRepository;
import com.sky.repository.SetmealDishRepository;
import com.sky.repository.SetmealRepository;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;



@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealRepository setmealRepository;

    @Autowired
    private SetmealDishRepository setmealDishRepository;

    @Autowired
    private DishRepository dishRepository;


    @Override
    public void saveWithDish(SetmealDTO setmealDTO) {
        //套餐表插入数据
        Setmeal setmeal=new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmeal.setUpdateUser(BaseContext.getCurrentId());
        setmeal.setCreateUser(BaseContext.getCurrentId());
        setmealRepository.save(setmeal);

        //维护套餐-标签表
        List<SetmealDish> setmealDishes=setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmeal.getId()));
        setmealDishRepository.saveAll(setmealDishes);





    }

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        int pageInPageable=setmealPageQueryDTO.getPage()-1;
        Pageable pageable= PageRequest.of(pageInPageable,setmealPageQueryDTO.getPageSize(),
                Sort.by(Sort.Direction.DESC,"createTime") );

        String name=setmealPageQueryDTO.getName();
        Long categoryId=setmealPageQueryDTO.getCategoryId();
        Integer status=setmealPageQueryDTO.getStatus();

        //获取Setmeal表数据
        Page<Setmeal> setmealsPage=setmealRepository.findByCondition(name,categoryId,status,pageable);
        List<Setmeal> setmeals=setmealsPage.getContent();

        List<SetmealVO> setmealVOs=setmeals.stream().map(setmeal -> {
            SetmealVO setmealVO=new SetmealVO();
            BeanUtils.copyProperties(setmeal,setmealVO);
            setmealVO.setCategoryName(setmeal.getCategory().getName());
            return setmealVO;
        }).toList();
        return new PageResult(setmealsPage.getTotalElements(),setmealVOs);


    }


    @Transactional
    @Override
    public void deleteBatch(List<Long> ids) {
        ids.forEach(id -> {
            Setmeal setmeal = setmealRepository.findById(id).orElseThrow(()->new BaseException("未找到套餐"));
            if(StatusConstant.ENABLE == setmeal.getStatus()){
                //起售中的套餐不能删除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        });

        //删除setmeal表中数据
        setmealRepository.deleteByIdIn(ids);
        //删除setmeal_dish表中数据
        setmealDishRepository.deleteBySetmealIdIn(ids);

    }


    @Transactional
    //与 Category 的关联关系被配置为懒加载,需要确保在访问懒加载属性时，数据库会话是打开的
    @Override
    public SetmealVO getByIdWithDish(Long id) {
        Setmeal setmeal=setmealRepository.findById(id).orElseThrow(()->new BaseException("未找到菜品"));
        Category category=setmeal.getCategory();

        //补全其它属性
        SetmealVO setmealVO=new SetmealVO();
        BeanUtils.copyProperties(setmeal,setmealVO);
        setmealVO.setCategoryName(category.getName());
        List<Long> ids=new ArrayList<>();
        ids.add(id);
        List<SetmealDish> setmealDishes=setmealDishRepository.findBySetmealIdIn(ids);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;



    }


    @Transactional
    @Override
    public void update(SetmealDTO setmealDTO) {
        //赋值属性(服务层控制可供修改的属性)
        Setmeal setmeal=setmealRepository.findById(setmealDTO.getId()).orElseThrow(()->new BaseException("未找到菜品"));
        setmeal.setUpdateUser(BaseContext.getCurrentId());
        setmeal.setCategoryId(setmealDTO.getCategoryId());
        setmeal.setPrice(setmealDTO.getPrice());
        setmeal.setDescription(setmealDTO.getDescription());
        setmeal.setImage(setmealDTO.getImage());
        setmealRepository.save(setmeal);

        //维护中间表，先删后加
        setmealDishRepository.deleteBySetmealId(setmealDTO.getId());
        List<SetmealDish> setmealDishes=setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmeal.getId()));
        setmealDishRepository.saveAll(setmealDishes);


    }

    @Override
    public void startOrStop(Integer status, Long id) {
        Setmeal setmeal=setmealRepository.findById(id).orElseThrow(()->new BaseException("未找到菜品"));
        setmeal.setUpdateUser(BaseContext.getCurrentId());
        setmeal.setStatus(status);
        setmealRepository.save(setmeal);

    }


    //通过分类id和状态查询
    @Override
    public List<Setmeal> list(Setmeal setmeal) {
        Long categoryId=setmeal.getCategoryId();
        Integer status=setmeal.getStatus();

        List<Setmeal> setmeals=setmealRepository.findByCategoryIdAndStatus(categoryId,status);
        return  setmeals;
    }


    //用于客户端，传入参数为套餐id
    @Override
    public List<DishItemVO> getDishItemById(Long id) {

        List<SetmealDish> setmealDishes = setmealDishRepository.findBySetmealIdWithDish(id);

        //旧方法。缺点，需要多次查询Dish
        //List<SetmealDish> setmealDishes=setmealDishRepository.findBySetmealId(id);

        return setmealDishes.stream().map(setmealDish -> {
            DishItemVO dishItemVO = new DishItemVO();
            Dish dish = setmealDish.getDish(); // 直接获取已加载的Dish对象

            dishItemVO.setImage(dish.getImage());
            dishItemVO.setCopies(setmealDish.getCopies());
            dishItemVO.setDescription(dish.getDescription());
            dishItemVO.setName(dish.getName());
            return dishItemVO;
        }).toList();



    }
}
