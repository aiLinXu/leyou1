package com.leyou.item.service;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.utils.BeanHelper;
import com.leyou.item.dto.CategoryDTO;
import com.leyou.item.entity.Category;
import com.leyou.item.mapper.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 虎哥
 */
@Service
public class CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    public List<CategoryDTO> queryCategoryByParentId(Long pid) {
        // 条件.
        Category category = new Category();
        category.setParentId(pid);
        // 查询
        List<Category> list = categoryMapper.select(category);
        // 健壮性判断
        if (CollectionUtils.isEmpty(list)) {
            // 没查到返回404
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }
        // 转换
        return BeanHelper.copyWithCollection(list, CategoryDTO.class);
        
    }

    public List<CategoryDTO> queryCategoryByIdList(List<Long> idList){
        List<Category> list = categoryMapper.selectByIdList(idList);
        // 健壮性判断
        if (CollectionUtils.isEmpty(list)) {
            // 没查到返回404
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }
        // 转换
        return BeanHelper.copyWithCollection(list, CategoryDTO.class);
    }
}
