package com.leyou.item.mapper;

import com.leyou.common.mappers.BaseMapper;
import com.leyou.item.entity.Brand;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author 虎哥
 */
@Repository
public interface BrandMapper extends BaseMapper<Brand> {

    int insertCategoryBrand(@Param("bid") Long id,@Param("ids") List<Long> ids);

    @Select("SELECT b.id, b.name, b.letter, b.image FROM tb_category_brand cb INNER JOIN tb_brand b ON b.id = cb.brand_id WHERE cb.category_id = #{cid}")
    List<Brand> queryBrandByCid(@Param("cid") Long cid);
}
