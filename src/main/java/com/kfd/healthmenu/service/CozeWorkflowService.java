package com.kfd.healthmenu.service;

import com.kfd.healthmenu.dto.CozeWorkflowRequest;
import com.kfd.healthmenu.dto.CozeWorkflowResponse;

public interface CozeWorkflowService {
    CozeWorkflowResponse execute(CozeWorkflowRequest request);
}
