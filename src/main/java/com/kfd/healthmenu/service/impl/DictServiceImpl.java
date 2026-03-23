package com.kfd.healthmenu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kfd.healthmenu.common.BizException;
import com.kfd.healthmenu.dto.dict.DictItemDto;
import com.kfd.healthmenu.dto.dict.DictItemSaveRequest;
import com.kfd.healthmenu.dto.dict.DictTypeDto;
import com.kfd.healthmenu.dto.dict.DictTypeSaveRequest;
import com.kfd.healthmenu.entity.SysDictItem;
import com.kfd.healthmenu.entity.SysDictType;
import com.kfd.healthmenu.mapper.SysDictItemMapper;
import com.kfd.healthmenu.mapper.SysDictTypeMapper;
import com.kfd.healthmenu.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DictServiceImpl implements DictService {

    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictItemMapper dictItemMapper;

    @Override
    public List<DictTypeDto> listTypes() {
        return dictTypeMapper.selectList(new LambdaQueryWrapper<SysDictType>()
                        .eq(SysDictType::getDeleted, 0)
                        .orderByAsc(SysDictType::getTypeCode))
                .stream()
                .map(this::toTypeDto)
                .toList();
    }

    @Override
    @Transactional
    public DictTypeDto saveType(DictTypeSaveRequest request) {
        validateTypeCode(request.getId(), request.getTypeCode());
        SysDictType entity = request.getId() == null ? new SysDictType() : requireType(request.getId());
        entity.setTypeCode(request.getTypeCode().trim());
        entity.setTypeName(request.getTypeName().trim());
        entity.setDescription(request.getDescription());
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        if (entity.getId() == null) {
            dictTypeMapper.insert(entity);
        } else {
            dictTypeMapper.updateById(entity);
        }
        return toTypeDto(entity);
    }

    @Override
    @Transactional
    public void deleteType(Long id) {
        SysDictType entity = requireType(id);
        long itemCount = dictItemMapper.selectCount(new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getDictTypeId, id)
                .eq(SysDictItem::getDeleted, 0));
        if (itemCount > 0) {
            throw new BizException("DICT_TYPE_NOT_EMPTY", "请先删除该类型下的字典项");
        }
        dictTypeMapper.deleteById(entity.getId());
    }

    @Override
    public List<DictItemDto> listItems(Long dictTypeId) {
        requireType(dictTypeId);
        return dictItemMapper.selectList(new LambdaQueryWrapper<SysDictItem>()
                        .eq(SysDictItem::getDictTypeId, dictTypeId)
                        .eq(SysDictItem::getDeleted, 0)
                        .orderByAsc(SysDictItem::getSortOrder)
                        .orderByAsc(SysDictItem::getId))
                .stream()
                .map(this::toItemDto)
                .toList();
    }

    @Override
    @Transactional
    public DictItemDto saveItem(DictItemSaveRequest request) {
        requireType(request.getDictTypeId());
        validateItemCode(request.getId(), request.getDictTypeId(), request.getItemCode());
        SysDictItem entity = request.getId() == null ? new SysDictItem() : requireItem(request.getId());
        entity.setDictTypeId(request.getDictTypeId());
        entity.setItemCode(request.getItemCode().trim());
        entity.setItemLabel(request.getItemLabel().trim());
        entity.setItemValue(request.getItemValue().trim());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setIsSystem(request.getIsSystem() == null ? 0 : request.getIsSystem());
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        if (entity.getId() == null) {
            dictItemMapper.insert(entity);
        } else {
            dictItemMapper.updateById(entity);
        }
        return toItemDto(entity);
    }

    @Override
    @Transactional
    public void deleteItem(Long id) {
        SysDictItem entity = requireItem(id);
        if (entity.getIsSystem() != null && entity.getIsSystem() == 1) {
            throw new BizException("DICT_ITEM_SYSTEM_LOCKED", "系统内置字典项不允许删除");
        }
        dictItemMapper.deleteById(entity.getId());
    }

    private void validateTypeCode(Long id, String typeCode) {
        if (!StringUtils.hasText(typeCode)) {
            throw new BizException("DICT_TYPE_CODE_EMPTY", "字典类型编码不能为空");
        }
        Long count = dictTypeMapper.selectCount(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getTypeCode, typeCode.trim())
                .eq(SysDictType::getDeleted, 0)
                .ne(id != null, SysDictType::getId, id));
        if (count != null && count > 0) {
            throw new BizException("DICT_TYPE_CODE_DUPLICATE", "字典类型编码已存在");
        }
    }

    private void validateItemCode(Long id, Long dictTypeId, String itemCode) {
        if (!StringUtils.hasText(itemCode)) {
            throw new BizException("DICT_ITEM_CODE_EMPTY", "字典项编码不能为空");
        }
        Long count = dictItemMapper.selectCount(new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getDictTypeId, dictTypeId)
                .eq(SysDictItem::getItemCode, itemCode.trim())
                .eq(SysDictItem::getDeleted, 0)
                .ne(id != null, SysDictItem::getId, id));
        if (count != null && count > 0) {
            throw new BizException("DICT_ITEM_CODE_DUPLICATE", "字典项编码已存在");
        }
    }

    private SysDictType requireType(Long id) {
        SysDictType entity = dictTypeMapper.selectById(id);
        if (entity == null || entity.getDeleted() != null && entity.getDeleted() == 1) {
            throw new BizException("DICT_TYPE_NOT_FOUND", "未找到对应的字典类型");
        }
        return entity;
    }

    private SysDictItem requireItem(Long id) {
        SysDictItem entity = dictItemMapper.selectById(id);
        if (entity == null || entity.getDeleted() != null && entity.getDeleted() == 1) {
            throw new BizException("DICT_ITEM_NOT_FOUND", "未找到对应的字典项");
        }
        return entity;
    }

    private DictTypeDto toTypeDto(SysDictType entity) {
        DictTypeDto dto = new DictTypeDto();
        dto.setId(entity.getId());
        dto.setTypeCode(entity.getTypeCode());
        dto.setTypeName(entity.getTypeName());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus());
        return dto;
    }

    private DictItemDto toItemDto(SysDictItem entity) {
        DictItemDto dto = new DictItemDto();
        dto.setId(entity.getId());
        dto.setDictTypeId(entity.getDictTypeId());
        dto.setItemCode(entity.getItemCode());
        dto.setItemLabel(entity.getItemLabel());
        dto.setItemValue(entity.getItemValue());
        dto.setSortOrder(entity.getSortOrder());
        dto.setIsSystem(entity.getIsSystem());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
