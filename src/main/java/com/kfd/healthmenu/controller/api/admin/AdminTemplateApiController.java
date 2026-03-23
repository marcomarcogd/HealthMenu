package com.kfd.healthmenu.controller.api.admin;

import com.kfd.healthmenu.dto.api.ApiResponse;
import com.kfd.healthmenu.dto.template.MenuTemplateDesignDto;
import com.kfd.healthmenu.dto.template.TemplateCopyRequest;
import com.kfd.healthmenu.dto.template.TemplateDesignSaveRequest;
import com.kfd.healthmenu.dto.template.TemplatePreviewDto;
import com.kfd.healthmenu.dto.template.TemplateSaveRequest;
import com.kfd.healthmenu.dto.template.TemplateSummaryDto;
import com.kfd.healthmenu.entity.MenuTemplate;
import com.kfd.healthmenu.service.TemplateService;
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
@RequestMapping("/api/admin/templates")
@RequiredArgsConstructor
public class AdminTemplateApiController {

    private final TemplateService templateService;

    @GetMapping
    public ApiResponse<List<TemplateSummaryDto>> list() {
        return ApiResponse.success(templateService.listSummaries());
    }

    @GetMapping("/{id}")
    public ApiResponse<MenuTemplateDesignDto> detail(@PathVariable Long id) {
        return ApiResponse.success(templateService.getDesignDetail(id));
    }

    @PostMapping
    public ApiResponse<TemplateSummaryDto> save(@Valid @RequestBody TemplateSaveRequest request) {
        MenuTemplate template = templateService.saveTemplate(request);
        return ApiResponse.success("模板保存成功", templateService.listSummaries().stream()
                .filter(item -> item.getId().equals(template.getId()))
                .findFirst()
                .orElseGet(() -> {
                    TemplateSummaryDto dto = new TemplateSummaryDto();
                    dto.setId(template.getId());
                    dto.setName(template.getName());
                    dto.setDescription(template.getDescription());
                    dto.setThemeCode(template.getThemeCode());
                    dto.setStatus(template.getStatus());
                    dto.setIsDefault(template.getIsDefault());
                    return dto;
                }));
    }

    @PostMapping("/{id}/design")
    public ApiResponse<MenuTemplateDesignDto> saveDesign(@PathVariable Long id,
                                                         @Valid @RequestBody TemplateDesignSaveRequest request) {
        request.setId(id);
        return ApiResponse.success("模板结构保存成功", templateService.saveDesign(request));
    }

    @PostMapping("/{id}/copy")
    public ApiResponse<TemplateSummaryDto> copy(@PathVariable Long id, @RequestBody(required = false) TemplateCopyRequest request) {
        MenuTemplate template = templateService.copyTemplate(id, request == null ? null : request.getName());
        TemplateSummaryDto dto = new TemplateSummaryDto();
        dto.setId(template.getId());
        dto.setName(template.getName());
        dto.setDescription(template.getDescription());
        dto.setThemeCode(template.getThemeCode());
        dto.setStatus(template.getStatus());
        dto.setIsDefault(template.getIsDefault());
        return ApiResponse.success("模板复制成功", dto);
    }

    @PostMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        templateService.updateTemplateStatus(id, status);
        return ApiResponse.success("模板状态更新成功", null);
    }

    @GetMapping("/{id}/preview")
    public ApiResponse<TemplatePreviewDto> preview(@PathVariable Long id) {
        return ApiResponse.success(templateService.preview(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        templateService.deleteById(id);
        return ApiResponse.success("模板删除成功", null);
    }
}
