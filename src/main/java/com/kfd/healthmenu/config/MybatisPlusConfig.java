package com.kfd.healthmenu.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.kfd.healthmenu.mapper")
public class MybatisPlusConfig {
}
