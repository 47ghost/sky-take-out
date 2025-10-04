package com.sky.service;

import com.sky.entity.AddressBook;

import java.util.List;

public interface AddressBookService {
    void save(AddressBook addressBook);

    List<AddressBook> listByUserId();

    AddressBook getById(Long id);

    void update(AddressBook addressBook);

    void setDefault(AddressBook addressBook);

    void deleteById(Long id);

    List<AddressBook> listDefultAddress(AddressBook addressBook);
}
