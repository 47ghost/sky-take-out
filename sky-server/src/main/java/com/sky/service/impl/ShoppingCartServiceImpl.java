package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.exception.BaseException;
import com.sky.repository.DishRepository;
import com.sky.repository.SetmealRepository;
import com.sky.repository.ShoppingCartRepository;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartRepository shoppingCartRepository;

    @Autowired
    private DishRepository dishRepository;

    @Autowired
    private SetmealRepository setmealRepository;

    @Transactional
    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        //判断当前购物车的添加是否为重复，重复则数量+1
        Long dishId=shoppingCartDTO.getDishId();
        Long setmealId =shoppingCartDTO.getSetmealId();
        String dishFlavor=shoppingCartDTO.getDishFlavor();
        Long  userId= BaseContext.getCurrentId();
        //拷贝属性
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);
        shoppingCart.setUserId(userId);


        List<ShoppingCart> list=shoppingCartRepository.findByCondition(userId,dishId,setmealId,dishFlavor);

        //如果已经存在了，只需要将数量加一
        if(list != null && !list.isEmpty()){
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartRepository.save(cart);
            return;

        }else {
            if(dishId != null) {
                //本次添加到购物车的是菜品
                Dish dish=dishRepository.findById(dishId).orElseThrow(()->new BaseException("未找到餐品"));
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());

            }
            else {
                //本次添加到购物车的是套餐
                Setmeal setmeal=setmealRepository.findById(setmealId).orElseThrow(()->new BaseException("未找到套餐"));
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());

            }
            shoppingCart.setNumber(1);
            shoppingCartRepository.save(shoppingCart);
            return;


        }

    }

    @Transactional
    @Override
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        Long dishId=shoppingCartDTO.getDishId();
        Long setmealId =shoppingCartDTO.getSetmealId();
        String dishFlavor=shoppingCartDTO.getDishFlavor();
        Long  userId= BaseContext.getCurrentId();

        List<ShoppingCart> list=shoppingCartRepository.findByCondition(userId,dishId,setmealId,dishFlavor);
        ShoppingCart shoppingCart;

        if(list != null && !list.isEmpty()){
            shoppingCart = list.get(0);

            Integer number = shoppingCart.getNumber();
            if(number == 1){
                //当前商品在购物车中的份数为1，直接删除当前记录
                shoppingCartRepository.deleteById(shoppingCart.getId());
            }else {
                //当前商品在购物车中的份数不为1，修改份数即可
                shoppingCart.setNumber(shoppingCart.getNumber() - 1);
                shoppingCartRepository.save(shoppingCart);
            }
        }


    }

    @Override
    @Transactional
    public void cleanShoppingCart() {
        Long userId=BaseContext.getCurrentId();
        shoppingCartRepository.deleteByUserId(userId);

    }

    @Override
    public List<ShoppingCart> showShoppingCart() {
        List<ShoppingCart> list=shoppingCartRepository.findByUserId(BaseContext.getCurrentId());
        return list;
    }


}
