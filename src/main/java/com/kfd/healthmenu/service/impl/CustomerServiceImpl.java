package com.kfd.healthmenu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kfd.healthmenu.common.BizException;
import com.kfd.healthmenu.entity.Customer;
import com.kfd.healthmenu.entity.CustomerMenu;
import com.kfd.healthmenu.mapper.CustomerMapper;
import com.kfd.healthmenu.mapper.CustomerMenuMapper;
import com.kfd.healthmenu.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;
    private final CustomerMenuMapper customerMenuMapper;

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

    @Override
    public void deleteById(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null || customer.getDeleted() != null && customer.getDeleted() == 1) {
            throw new BizException("CUSTOMER_NOT_FOUND", "客户不存在或已删除");
        }

        long menuCount = customerMenuMapper.selectCount(new LambdaQueryWrapper<CustomerMenu>()
                .eq(CustomerMenu::getCustomerId, id)
                .eq(CustomerMenu::getDeleted, 0));
        if (menuCount > 0) {
            throw new BizException(
                    "CUSTOMER_DELETE_HAS_MENUS",
                    "该客户下还有 " + menuCount + " 份餐单，不能直接删除。若不再使用，建议先停用客户；如需彻底删除，请先删除相关餐单。"
            );
        }

        customerMapper.deleteById(id);
    }
}
