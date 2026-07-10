package com.legalai.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legalai.model.LawCategory;
import com.legalai.model.LawCategoryType;
import com.legalai.model.LawDocumentCategory;
import com.legalai.repository.LawCategoryMapper;
import com.legalai.repository.LawCategoryTypeMapper;
import com.legalai.repository.LawDocumentCategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LawCategoryService {
    @Autowired
    private LawCategoryTypeMapper lawCategoryTypeMapper;

    @Autowired
    private LawCategoryMapper lawCategoryMapper;

    @Autowired
    private LawDocumentCategoryMapper lawDocumentCategoryMapper;

    public List<Map<String, Object>> listTypes() {
        LambdaQueryWrapper<LawCategoryType> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(LawCategoryType::getSortOrder);
        List<LawCategoryType> types = lawCategoryTypeMapper.selectList(wrapper);
        return types.stream().map(this::toMap).collect(Collectors.toList());
    }

    public LawCategoryType getType(Long id) {
        return lawCategoryTypeMapper.selectById(id);
    }

    public void createType(LawCategoryType type) {
        lawCategoryTypeMapper.insert(type);
    }

    public void updateType(Long id, LawCategoryType type) {
        type.setId(id);
        lawCategoryTypeMapper.updateById(type);
    }

    public void deleteType(Long id) {
        lawCategoryTypeMapper.deleteById(id);
    }

    public List<Map<String, Object>> listCategories(Long typeId) {
        LambdaQueryWrapper<LawCategory> wrapper = new LambdaQueryWrapper<>();
        if (typeId != null) {
            wrapper.eq(LawCategory::getCategoryTypeId, typeId);
        }
        wrapper.orderByAsc(LawCategory::getSortOrder);
        List<LawCategory> categories = lawCategoryMapper.selectList(wrapper);

        Map<Long, List<Map<String, Object>>> childrenMap = new LinkedHashMap<>();
        List<Map<String, Object>> roots = new ArrayList<>();

        for (LawCategory c : categories) {
            Map<String, Object> map = toMap(c);
            map.put("children", new ArrayList<Map<String, Object>>());
            if (c.getParentId() == null || c.getParentId() == 0) {
                roots.add(map);
            } else {
                childrenMap.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(map);
            }
        }

        for (Map<String, Object> root : roots) {
            Long pid = (Long) root.get("id");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children = childrenMap.getOrDefault(pid, new ArrayList<>());
            root.put("children", children);
        }

        return roots;
    }

    public Map<String, Object> getCategory(Long id) {
        LawCategory category = lawCategoryMapper.selectById(id);
        if (category == null) {
            return null;
        }
        Map<String, Object> map = toMap(category);
        if (category.getParentId() != null && category.getParentId() > 0) {
            LawCategory parent = lawCategoryMapper.selectById(category.getParentId());
            if (parent != null) {
                map.put("parent", toMap(parent));
            }
        }
        return map;
    }

    public void createCategory(LawCategory category) {
        category.setStatus(1);
        lawCategoryMapper.insert(category);
    }

    public void updateCategory(Long id, LawCategory category) {
        category.setId(id);
        lawCategoryMapper.updateById(category);
    }

    public void deleteCategory(Long id) {
        lawCategoryMapper.delete(new LambdaQueryWrapper<LawCategory>()
                .eq(LawCategory::getId, id)
                .or()
                .eq(LawCategory::getParentId, id));
    }

    public List<Map<String, Object>> getDocumentCategories(Long lawId) {
        List<LawDocumentCategory> docCategories = lawDocumentCategoryMapper.selectList(
                new LambdaQueryWrapper<LawDocumentCategory>().eq(LawDocumentCategory::getLawId, lawId));
        List<Long> categoryIds = docCategories.stream()
                .map(LawDocumentCategory::getCategoryId)
                .collect(Collectors.toList());
        if (categoryIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<LawCategory> categories = lawCategoryMapper.selectList(
                new LambdaQueryWrapper<LawCategory>().in(LawCategory::getId, categoryIds));
        return categories.stream().map(this::toMap).collect(Collectors.toList());
    }

    public void setDocumentCategories(Long lawId, List<Long> categoryIds) {
        lawDocumentCategoryMapper.delete(new LambdaQueryWrapper<LawDocumentCategory>()
                .eq(LawDocumentCategory::getLawId, lawId));
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }
        List<LawDocumentCategory> docCategories = new ArrayList<>(categoryIds.size());
        for (Long catId : categoryIds) {
            LawDocumentCategory docCat = new LawDocumentCategory();
            docCat.setLawId(lawId);
            docCat.setCategoryId(catId);
            docCategories.add(docCat);
        }
        lawDocumentCategoryMapper.batchInsert(docCategories);
    }

    private Map<String, Object> toMap(LawCategoryType type) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", type.getId());
        map.put("typeCode", type.getTypeCode());
        map.put("typeName", type.getTypeName());
        map.put("description", type.getDescription());
        map.put("sortOrder", type.getSortOrder());
        map.put("createdAt", type.getCreatedAt());
        return map;
    }

    private Map<String, Object> toMap(LawCategory category) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", category.getId());
        map.put("categoryTypeId", category.getCategoryTypeId());
        map.put("parentId", category.getParentId());
        map.put("categoryCode", category.getCategoryCode());
        map.put("categoryName", category.getCategoryName());
        map.put("color", category.getColor());
        map.put("sortOrder", category.getSortOrder());
        map.put("status", category.getStatus());
        map.put("createdAt", category.getCreatedAt());
        return map;
    }
}
