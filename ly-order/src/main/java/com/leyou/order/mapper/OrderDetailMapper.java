package com.leyou.order.mapper;

import com.leyou.common.mappers.BaseMapper;
import com.leyou.order.entity.OrderDetail;
import tk.mybatis.mapper.common.special.InsertListMapper;

public interface OrderDetailMapper extends BaseMapper<OrderDetail>, InsertListMapper<OrderDetail> {
}