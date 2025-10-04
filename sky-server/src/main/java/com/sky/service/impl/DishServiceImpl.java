package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.BaseException;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.repository.DishFlavorRepository;
import com.sky.repository.DishRepository;
import com.sky.repository.SetmealDishRepository;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishRepository dishRepository;

    @Autowired
    private DishFlavorRepository dishFlavorRepository;

    @Autowired
    private SetmealDishRepository setmealDishRepository;


    @Transactional
    @Override
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish=new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dish.setCreateUser(BaseContext.getCurrentId());
        dish.setUpdateUser(BaseContext.getCurrentId());
        //插入菜品
        dishRepository.save(dish);

        //插入口味
        List<DishFlavor> flavors=dishDTO.getFlavors();
        if(flavors!=null && !flavors.isEmpty()){
            flavors.forEach(flavor->flavor.setDishId(dish.getId()));
            dishFlavorRepository.saveAll(flavors);
        }

    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        int pageInPagebale=dishPageQueryDTO.getPage()-1;
        Pageable pageable= PageRequest.of(pageInPagebale,dishPageQueryDTO.getPageSize(),
                Sort.by(Sort.Direction.DESC,"createTime"));

        String name=dishPageQueryDTO.getName();
        Integer status=dishPageQueryDTO.getStatus();
        Long categoryId=dishPageQueryDTO.getCategoryId();

        Page<Dish> dishes;
/*

        if(name==null||name.isEmpty()){
            dishes=dishRepository.findAllByCategoryId(dishPageQueryDTO.getCategoryId(),pageable);
        }else {
            dishes=dishRepository.findByNameAndCategoryId(dishPageQueryDTO.getName(),dishPageQueryDTO.getCategoryId(),pageable);

        }
*/

        dishes=dishRepository.findByCondition(name,status,categoryId,pageable);

        List<DishVO> dishVOList=dishes.getContent().stream().map(
                dish->{
                    DishVO dishVO=new DishVO();
                    BeanUtils.copyProperties(dish,dishVO);
                    if (dish.getCategory() != null) {
                        dishVO.setCategoryName(dish.getCategory().getName());
                    }
                    return  dishVO;
                }
        ).toList();


        return new PageResult(dishes.getTotalElements(),dishVOList);
    }

    @Transactional
    @Override
    public void deleteBatch(List<Long> ids) {
        //判断当前菜品是否能够删除---是否存在起售中的菜品？？
        List<Dish> dishes=dishRepository.findByIdIn(ids);
        for (Dish dish : dishes){
            if(dish.getStatus()==StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }


        //判断当前菜品是否能够删除---是否被套餐关联了？？
        int count= setmealDishRepository.countByDishIdIn(ids);
        if (count > 0) {
            //当前菜品被套餐关联了，不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        dishFlavorRepository.deleteByDishIdIn(ids);
        dishRepository.deleteByIdIn(ids);


    }

    @Override
    public DishVO getByIdWithFlavor(Long id) {
        Dish dish=dishRepository.findById(id)
                .orElseThrow(()->new BaseException("未查询到菜品"));

        List<DishFlavor> flavors=dishFlavorRepository.findByDishId(id);

        DishVO dishVO=new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(flavors);

        return dishVO;

    }


    @Transactional
    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish=dishRepository.findById(dishDTO.getId())
                .orElseThrow(()->new BaseException("未查询到菜品"));

        if(StringUtils.hasText(dishDTO.getName())){
            dish.setName(dishDTO.getName());
        }
        dish.setCategoryId(dishDTO.getCategoryId());
        dish.setPrice(dishDTO.getPrice());
        dish.setImage(dishDTO.getImage());
        dish.setDescription(dishDTO.getDescription());
        dish.setStatus(dishDTO.getStatus());
        dish.setUpdateUser(BaseContext.getCurrentId());
        dish.setUpdateTime(LocalDateTime.now());

        //口味数据的更改 -先删后加
        //dishDTO可能包含前端的数据，其flavor包含id属性
        dishFlavorRepository.deleteByDishId(dishDTO.getId());
        List<DishFlavor> flavors=dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(flavor -> {
                flavor.setDishId(dish.getId());
                flavor.setId(null); // 清空旧ID，避免Hibernate误以为是更新
            });
            dishFlavorRepository.saveAll(flavors);
        }

        dishRepository.save(dish);

    }

    @Override
    public List<Dish> listByCategoryId(Long categoryId) {
        List<Dish> dishes=  dishRepository.findByCategoryId(categoryId);
        return dishes;

    }

    @Override
    public void startOrStop(Integer status, Long id) {
     Dish dish=dishRepository.findById(id).orElseThrow(()->new BaseException("未找到菜品"));
     dish.setUpdateUser(BaseContext.getCurrentId());
     dish.setStatus(status);
     dishRepository.save(dish);
    }


    //传入的dish包含分类id、状态
    //需要额外附带口味
    @Override
    public List<DishVO> listWithFlavor(Dish dish) {
        Long categoryId=dish.getCategoryId();
        Integer status=dish.getStatus();

        List<Dish> dishes=dishRepository.findByCategoryIdAndStatus(categoryId,status);

        List<DishVO> dishVOS=new ArrayList<>();

        for (Dish d :dishes){
            DishVO  dishVO=new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            List<DishFlavor> flavors=dishFlavorRepository.findByDishId(d.getId());
            dishVO.setFlavors(flavors);
            dishVOS.add(dishVO);
        }


        return dishVOS;
    }
}
