package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.exception.BaseException;
import com.sky.repository.AddressBookRepository;
import com.sky.service.AddressBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;


@Service
public class AddressBookServiceImpl implements AddressBookService {

    @Autowired
    private AddressBookRepository addressBookRepository;

    @Override
    public void save(AddressBook addressBook) {
        addressBook.setId(null);
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBook.setIsDefault(0);
        addressBookRepository.save(addressBook);

    }


    @Override
    public List<AddressBook> listByUserId() {
        List<AddressBook> list=addressBookRepository.findByUserId(BaseContext.getCurrentId());
        return list;

    }


    @Override
    public AddressBook getById(Long id) {
        return addressBookRepository.findById(id).orElseThrow(()->new BaseException("未找到地址"));
    }

    @Override
    public void update(AddressBook addressBook) {

        addressBookRepository.save(addressBook);
    }



    @Override
    public void deleteById(Long id) {
        addressBookRepository.deleteById(id);
    }

    @Transactional
    @Override
    public void setDefault(AddressBook addressBook) {
        //1、将当前用户的所有地址修改为非默认地址 update address_book set is_default = ? where user_id = ?
        Long userId=BaseContext.getCurrentId();
        Integer isDefault=0;
        Long id=addressBook.getId();

        addressBookRepository.updateIsDefaultByUserId(userId,isDefault);

        //2、将当前地址改为默认地址 update address_book set is_default = ? where id = ?
        addressBookRepository.setIsDefult(id,1);

    }

    @Override
    public List<AddressBook> listDefultAddress(AddressBook addressBook) {
        Long userId=BaseContext.getCurrentId();
        Integer isDefault=1;

        List<AddressBook> list=addressBookRepository.findByUserIdAndIsDefault(userId,isDefault);
        return  list;



    }
}
