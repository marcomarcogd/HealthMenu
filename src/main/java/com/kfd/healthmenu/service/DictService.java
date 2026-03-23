package com.kfd.healthmenu.service;

import com.kfd.healthmenu.dto.dict.DictItemDto;
import com.kfd.healthmenu.dto.dict.DictItemSaveRequest;
import com.kfd.healthmenu.dto.dict.DictTypeDto;
import com.kfd.healthmenu.dto.dict.DictTypeSaveRequest;

import java.util.List;

public interface DictService {
    List<DictTypeDto> listTypes();

    DictTypeDto saveType(DictTypeSaveRequest request);

    void deleteType(Long id);

    List<DictItemDto> listItems(Long dictTypeId);

    DictItemDto saveItem(DictItemSaveRequest request);

    void deleteItem(Long id);
}
