package com.kfd.healthmenu;

import com.kfd.healthmenu.entity.Customer;
import com.kfd.healthmenu.entity.MenuTemplateSection;
import com.kfd.healthmenu.mapper.CustomerMapper;
import com.kfd.healthmenu.mapper.MenuTemplateSectionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SeedDataEncodingTest {

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private MenuTemplateSectionMapper menuTemplateSectionMapper;

    @Test
    void chineseSeedDataShouldLoadWithoutMojibake() {
        Customer customer = customerMapper.selectById(2001L);
        MenuTemplateSection section = menuTemplateSectionMapper.selectById(3001L);

        assertThat(customer).isNotNull();
        assertThat(customer.getName()).isEqualTo("张三");
        assertThat(customer.getExclusiveTitle()).isEqualTo("张女士的营养调理餐单");

        assertThat(section).isNotNull();
        assertThat(section.getTitle()).isEqualTo("专属标题");
    }
}
