package com.kfd.healthmenu.dto.api;

import lombok.Data;

import java.util.List;

@Data
public class AdminOptionsResponse {
    private List<LabelValueOption> sectionTypes;
    private List<LabelValueOption> contentFormats;
    private List<LabelValueOption> recordStatuses;
    private List<BooleanOption> booleanOptions;
    private List<AdminCustomerOption> customers;
    private List<LabelValueOption> templates;
}
