package com.leyou.item.mapper;

import com.leyou.common.mappers.BaseMapper;
import com.leyou.item.entity.Sku;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;
import tk.mybatis.mapper.common.special.InsertListMapper;

/**
 * @author 虎哥
 */

public interface SkuMapper extends BaseMapper<Sku>, InsertListMapper<Sku> {
    @Update("UPDATE tb_sku SET stock = stock - #{num} WHERE id = #{id}")
    int minusStock(@Param("id") Long id, @Param("num") Integer num);
}
