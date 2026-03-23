package com.kfd.healthmenu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kfd.healthmenu.entity.Customer;
import com.kfd.healthmenu.mapper.CustomerMapper;
import com.kfd.healthmenu.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;

    @Override
    public List<Customer> listAll() {
        return customerMapper.selectList(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getDeleted, 0)
                .orderByDesc(Customer::getUpdateTime));
    }

    @Override
    public Customer getById(Long id) {
        return customerMapper.selectById(id);
    }

    @Override
    public void save(Customer customer) {
        if (customer.getStatus() == null) {
            customer.setStatus(1);
        }
        if (customer.getId() == null) {
            customerMapper.insert(customer);
            return;
        }
        customerMapper.updateById(customer);
    }
}
