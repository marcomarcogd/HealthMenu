package com.kfd.healthmenu.service;

import com.kfd.healthmenu.entity.Customer;

import java.util.List;

public interface CustomerService {
    List<Customer> listAll();

    Customer getById(Long id);

    void save(Customer customer);

    void deleteById(Long id);
}
