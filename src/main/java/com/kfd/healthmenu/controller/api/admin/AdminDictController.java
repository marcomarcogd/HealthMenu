package com.kfd.healthmenu.controller.api.admin;

import com.kfd.healthmenu.dto.api.ApiResponse;
import com.kfd.healthmenu.dto.dict.DictItemDto;
import com.kfd.healthmenu.dto.dict.DictItemSaveRequest;
import com.kfd.healthmenu.dto.dict.DictTypeDto;
import com.kfd.healthmenu.dto.dict.DictTypeSaveRequest;
import com.kfd.healthmenu.service.DictService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dicts")
@RequiredArgsConstructor
public class AdminDictController {

    private final DictService dictService;

    @GetMapping("/types")
    public ApiResponse<List<DictTypeDto>> listTypes() {
        return ApiResponse.success(dictService.listTypes());
    }

    @PostMapping("/types")
    public ApiResponse<DictTypeDto> saveType(@Valid @RequestBody DictTypeSaveRequest request) {
        return ApiResponse.success("字典类型保存成功", dictService.saveType(request));
    }

    @DeleteMapping("/types/{id}")
    public ApiResponse<Void> deleteType(@PathVariable Long id) {
        dictService.deleteType(id);
        return ApiResponse.success("字典类型删除成功", null);
    }

    @GetMapping("/items")
    public ApiResponse<List<DictItemDto>> listItems(@RequestParam Long dictTypeId) {
        return ApiResponse.success(dictService.listItems(dictTypeId));
    }

    @PostMapping("/items")
    public ApiResponse<DictItemDto> saveItem(@Valid @RequestBody DictItemSaveRequest request) {
        return ApiResponse.success("字典项保存成功", dictService.saveItem(request));
    }

    @DeleteMapping("/items/{id}")
    public ApiResponse<Void> deleteItem(@PathVariable Long id) {
        dictService.deleteItem(id);
        return ApiResponse.success("字典项删除成功", null);
    }
}
