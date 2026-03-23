package com.kfd.healthmenu.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class IndexController {

    @GetMapping({"/", "/admin"})
    public String index() {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "管理端和餐单展示页已改为独立前端部署，请使用前端地址访问。后端仅提供 /api/admin/** 和 /api/public/**。");
    }
}
