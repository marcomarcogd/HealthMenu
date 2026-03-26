package com.kfd.healthmenu.controller.api.admin;

import com.kfd.healthmenu.common.ContentFormat;
import com.kfd.healthmenu.common.RecordStatus;
import com.kfd.healthmenu.common.SectionType;
import com.kfd.healthmenu.dto.api.AdminCustomerOption;
import com.kfd.healthmenu.dto.api.AdminOptionsResponse;
import com.kfd.healthmenu.dto.api.ApiResponse;
import com.kfd.healthmenu.dto.api.BooleanOption;
import com.kfd.healthmenu.dto.api.LabelValueOption;
import com.kfd.healthmenu.service.CustomerService;
import com.kfd.healthmenu.service.DictService;
import com.kfd.healthmenu.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/options")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('OPTIONS_READ')")
public class AdminOptionsController {

    private final CustomerService customerService;
    private final TemplateService templateService;
    private final DictService dictService;

    @GetMapping
    public ApiResponse<AdminOptionsResponse> getOptions() {
        Map<String, String> genderLabels = loadDictLabelMap("gender");
        AdminOptionsResponse response = new AdminOptionsResponse();
        response.setSectionTypes(toSectionTypeOptions());
        response.setContentFormats(toContentFormatOptions());
        response.setRecordStatuses(toRecordStatusOptions());
        response.setBooleanOptions(List.of(
                new BooleanOption("启用", true),
                new BooleanOption("停用", false)
        ));
        response.setCustomers(customerService.listAll().stream()
                .map(item -> {
                    AdminCustomerOption option = new AdminCustomerOption();
                    option.setLabel(item.getName());
                    option.setValue(String.valueOf(item.getId()));
                    option.setNickname(item.getNickname());
                    option.setGender(item.getGender());
                    option.setGenderLabel(resolveDictLabel(genderLabels, item.getGender()));
                    option.setPhone(item.getPhone());
                    option.setExclusiveTitle(item.getExclusiveTitle());
                    option.setNote(item.getNote());
                    option.setStatus(item.getStatus());
                    return option;
                })
                .toList());
        response.setTemplates(templateService.listSummaries().stream()
                .map(item -> new LabelValueOption(item.getName(), String.valueOf(item.getId())))
                .toList());
        return ApiResponse.success(response);
    }

    private Map<String, String> loadDictLabelMap(String typeCode) {
        Map<String, String> labels = new LinkedHashMap<>();
        dictService.listTypes().stream()
                .filter(item -> typeCode.equalsIgnoreCase(item.getTypeCode()))
                .findFirst()
                .ifPresent(type -> dictService.listItems(type.getId()).stream()
                        .filter(item -> item.getStatus() == null || item.getStatus() == 1)
                        .forEach(item -> {
                            if (item.getItemValue() != null && item.getItemLabel() != null) {
                                labels.put(item.getItemValue(), item.getItemLabel());
                            }
                            if (item.getItemCode() != null && item.getItemLabel() != null) {
                                labels.put(item.getItemCode(), item.getItemLabel());
                            }
                        }));
        applyBuiltinFallbackLabels(typeCode, labels);
        return labels;
    }

    private String resolveDictLabel(Map<String, String> labels, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return rawValue;
        }
        return labels.getOrDefault(rawValue, rawValue);
    }

    private void applyBuiltinFallbackLabels(String typeCode, Map<String, String> labels) {
        if (!"gender".equalsIgnoreCase(typeCode)) {
            return;
        }
        labels.putIfAbsent("female", "女");
        labels.putIfAbsent("male", "男");
        labels.putIfAbsent("FEMALE", "女");
        labels.putIfAbsent("MALE", "男");
        labels.putIfAbsent("女", "女");
        labels.putIfAbsent("男", "男");
    }

    private List<LabelValueOption> toSectionTypeOptions() {
        return Arrays.stream(SectionType.values())
                .map(item -> new LabelValueOption(sectionTypeLabel(item), item.name()))
                .toList();
    }

    private List<LabelValueOption> toContentFormatOptions() {
        return Arrays.stream(ContentFormat.values())
                .map(item -> new LabelValueOption(contentFormatLabel(item), item.name()))
                .toList();
    }

    private List<LabelValueOption> toRecordStatusOptions() {
        return Arrays.stream(RecordStatus.values())
                .map(item -> new LabelValueOption(recordStatusLabel(item), item.name()))
                .toList();
    }

    private String sectionTypeLabel(SectionType type) {
        return switch (type) {
            case EXCLUSIVE_TITLE -> "专属标题";
            case WEEKLY_TIP -> "每周提示";
            case SWAP_GUIDE -> "食材互换指南";
            case DAILY_MENU -> "每日餐单";
            case REMARK -> "备注区块";
            case IMAGE_BLOCK -> "图片区块";
        };
    }

    private String contentFormatLabel(ContentFormat format) {
        return switch (format) {
            case PLAIN_TEXT -> "纯文本";
            case RICH_TEXT -> "富文本";
        };
    }

    private String recordStatusLabel(RecordStatus status) {
        return switch (status) {
            case DRAFT -> "草稿";
            case PUBLISHED -> "已发布";
        };
    }
}
