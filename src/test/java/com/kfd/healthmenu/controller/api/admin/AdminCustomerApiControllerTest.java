package com.kfd.healthmenu.controller.api.admin;

import com.kfd.healthmenu.entity.Customer;
import com.kfd.healthmenu.entity.CustomerMenu;
import com.kfd.healthmenu.mapper.CustomerMapper;
import com.kfd.healthmenu.mapper.CustomerMenuMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "admin", authorities = "CUSTOMER_MANAGE")
class AdminCustomerApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private CustomerMenuMapper customerMenuMapper;

    @Test
    void delete_shouldRemoveCustomerWithoutMenus() throws Exception {
        Customer customer = createCustomer("待删除客户", "female");

        mockMvc.perform(delete("/api/admin/customers/{id}", customer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("客户删除成功"));
    }

    @Test
    void delete_shouldRejectCustomerWithMenus() throws Exception {
        Customer customer = createCustomer("有关联餐单客户", "female");
        CustomerMenu menu = new CustomerMenu();
        menu.setCustomerId(customer.getId());
        menu.setTemplateId(1001L);
        menu.setTitle("演示餐单");
        menu.setMenuDate(LocalDate.of(2026, 3, 26));
        menu.setWeekIndex(1);
        menu.setStatus("DRAFT");
        menu.setShowWeeklyTip(1);
        menu.setShowSwapGuide(0);
        customerMenuMapper.insert(menu);

        mockMvc.perform(delete("/api/admin/customers/{id}", customer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CUSTOMER_DELETE_HAS_MENUS"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("建议先停用客户")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("请先删除相关餐单")));
    }

    private Customer createCustomer(String name, String gender) {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setGender(gender);
        customer.setStatus(1);
        customerMapper.insert(customer);
        return customer;
    }
}
