package com.kfd.healthmenu.controller.api.admin;

import com.kfd.healthmenu.dto.api.ApiResponse;
import com.kfd.healthmenu.dto.customer.CustomerSaveRequest;
import com.kfd.healthmenu.entity.Customer;
import com.kfd.healthmenu.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/customers")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('CUSTOMER_MANAGE')")
public class AdminCustomerApiController {

    private final CustomerService customerService;

    @PostMapping
    public ApiResponse<Void> save(@Valid @RequestBody CustomerSaveRequest request) {
        Customer customer = new Customer();
        customer.setId(request.getId());
        customer.setName(request.getName());
        customer.setNickname(request.getNickname());
        customer.setGender(request.getGender());
        customer.setPhone(request.getPhone());
        customer.setExclusiveTitle(request.getExclusiveTitle());
        customer.setNote(request.getNote());
        customer.setStatus(request.getStatus());
        customerService.save(customer);
        return ApiResponse.success("客户保存成功", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        customerService.deleteById(id);
        return ApiResponse.success("客户删除成功", null);
    }
}
