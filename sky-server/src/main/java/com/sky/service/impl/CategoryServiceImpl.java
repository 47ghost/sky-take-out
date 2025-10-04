package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.exception.BaseException;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.repository.CategoryRepository;
import com.sky.repository.DishRepository;
import com.sky.repository.SetmealRepository;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.List;



@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private DishRepository dishRepository;

    @Autowired
    private SetmealRepository setmealRepository;


    @Transactional
    @Override
    public void deleteById(Long id) {
        Long dishCount=dishRepository.countByCategoryId(id);
        if(dishCount>0){
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        }

        Long setmealCount=setmealRepository.countByCategoryId(id);
        if(setmealCount>0){
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }

        categoryRepository.deleteById(id);



    }

    @Override
    public void save(CategoryDTO categoryDTO) {
        Category category=new Category();
        BeanUtils.copyProperties(categoryDTO,category);

        //放置出现分类下的菜品为空
        category.setStatus(StatusConstant.DISABLE);

        category.setUpdateUser(BaseContext.getCurrentId());
        category.setCreateUser(BaseContext.getCurrentId());

        categoryRepository.save(category);

    }

    @Override
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        int pageInPageable = categoryPageQueryDTO.getPage() - 1;
        Pageable pageable = PageRequest.of(pageInPageable, categoryPageQueryDTO.getPageSize(),
                Sort.by(Sort.Direction.DESC,"createTime"));

        Page<Category> page;
        String name = categoryPageQueryDTO.getName();
        Integer type = categoryPageQueryDTO.getType();

/*
        if (name==null||name.isEmpty()) {
            page = categoryRepository.findByType(type, pageable);

        } else {
            page = categoryRepository.findByNameContainingAndType(name, type, pageable);
        }
*/
        page=categoryRepository.findByCondition(name,type,pageable);

        return new PageResult(page.getTotalElements(), page.getContent());
    }


    @Transactional
    @Override
    public void update(CategoryDTO categoryDTO) {
        Category category=new Category();
        BeanUtils.copyProperties(categoryDTO,category);
        category.setUpdateUser(BaseContext.getCurrentId());
        category.setUpdateTime(LocalDateTime.now());
        int updated = categoryRepository.updateTypeAndSortById(category.getId(),category.getName(),
                category.getSort(),category.getUpdateUser(),category.getUpdateTime());
        if (updated == 0) {
            throw new BaseException("分类的更新操作失败");
        }

    }


    @Transactional
    @Override
    public void startOrStop(Integer status, Long id) {
        LocalDateTime updateTime=LocalDateTime.now();
        Long updateUser=BaseContext.getCurrentId();

        categoryRepository.updateStatus(status,id,updateUser,updateTime);
    }

    @Override
    public List<Category> list(Integer type) {
        List<Category> categories;
        if (type==null){
            categories=  categoryRepository.findAll();
        }
        else {
           categories=  categoryRepository.findAllByType(type);

        }
        return categories.stream().filter(category -> category.getStatus()==StatusConstant.ENABLE).toList();


    }
}
