package com.leyou.item.mapper;

import com.leyou.common.mappers.BaseMapper;
import com.leyou.item.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

/**
 * @author 虎哥
 */
@Repository
public interface CategoryMapper extends BaseMapper<Category> {
}
