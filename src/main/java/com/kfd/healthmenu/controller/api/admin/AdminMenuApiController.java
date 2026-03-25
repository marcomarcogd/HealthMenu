package com.kfd.healthmenu.controller.api.admin;

import com.kfd.healthmenu.dto.AiImportResultDto;
import com.kfd.healthmenu.dto.CustomerMenuForm;
import com.kfd.healthmenu.dto.api.AiImageResponse;
import com.kfd.healthmenu.dto.api.ApiResponse;
import com.kfd.healthmenu.dto.api.IdResponse;
import com.kfd.healthmenu.dto.api.IdsRequest;
import com.kfd.healthmenu.dto.api.PageResult;
import com.kfd.healthmenu.dto.menu.AiImageGenerateRequest;
import com.kfd.healthmenu.dto.menu.AiMenuParseRequest;
import com.kfd.healthmenu.dto.menu.CustomerMenuSummaryDto;
import com.kfd.healthmenu.dto.menu.MenuInitRequest;
import com.kfd.healthmenu.dto.menu.MenuSummaryQuery;
import com.kfd.healthmenu.service.CustomerMenuService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/admin/menus")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MENU_MANAGE')")
public class AdminMenuApiController {

    private final CustomerMenuService customerMenuService;

    @GetMapping
    public ApiResponse<PageResult<CustomerMenuSummaryDto>> list(MenuSummaryQuery query) {
        return ApiResponse.success(customerMenuService.listSummaries(query));
    }

    @PostMapping("/init")
    public ApiResponse<CustomerMenuForm> init(@RequestBody MenuInitRequest request) {
        if (Boolean.TRUE.equals(request.getUseAi()) && StringUtils.hasText(request.getSourceText())) {
            AiImportResultDto aiResult = customerMenuService.parseAiMenuText(request.getSourceText());
            return ApiResponse.success(customerMenuService.buildCreateFormFromAi(request.getCustomerId(), request.getTemplateId(), aiResult));
        }
        return ApiResponse.success(customerMenuService.buildCreateForm(request.getCustomerId(), request.getTemplateId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerMenuForm> detail(@PathVariable Long id) {
        return ApiResponse.success(customerMenuService.getFormById(id));
    }

    @PostMapping
    public ApiResponse<IdResponse> save(@Valid @RequestBody CustomerMenuForm form) {
        Long id = customerMenuService.saveMenu(form);
        return ApiResponse.success("餐单保存成功", new IdResponse(id));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<Void> publish(@PathVariable Long id) {
        customerMenuService.publishMenu(id);
        return ApiResponse.success("餐单已发布", null);
    }

    @PostMapping("/batch/publish")
    public ApiResponse<Void> batchPublish(@Valid @RequestBody IdsRequest request) {
        customerMenuService.publishMenus(request.getIds());
        return ApiResponse.success("已批量发布餐单", null);
    }

    @GetMapping("/{id}/export/excel")
    public void exportExcel(@PathVariable Long id, HttpServletResponse response) {
        customerMenuService.exportMenuExcel(id, response);
    }

    @PostMapping("/batch/export/excel")
    public void batchExportExcel(@Valid @RequestBody IdsRequest request, HttpServletResponse response) {
        customerMenuService.exportMenusExcel(request.getIds(), response);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        customerMenuService.deleteById(id);
        return ApiResponse.success("餐单已删除", null);
    }

    @PostMapping("/ai/parse")
    public ApiResponse<AiImportResultDto> parse(@Valid @RequestBody AiMenuParseRequest request) {
        return ApiResponse.success(customerMenuService.parseAiMenuText(request.getSourceText()));
    }

    @PostMapping("/ai/generate-image")
    public ApiResponse<AiImageResponse> generateImage(@Valid @RequestBody AiImageGenerateRequest request) {
        String prompt = request.getPrompt().trim();
        String path = customerMenuService.generateAiImage(prompt);
        return ApiResponse.success("AI 图片生成成功", new AiImageResponse(path, prompt));
    }
}
