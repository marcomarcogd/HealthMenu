package com.kfd.healthmenu.dto;

import com.kfd.healthmenu.entity.Customer;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CustomerOptionDto {
    private Long id;
    private String name;

    public static CustomerOptionDto from(Customer customer) {
        return new CustomerOptionDto(customer.getId(), customer.getName());
    }
}
