package com.leyou.order.service;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.utils.BeanHelper;
import com.leyou.common.utils.IdWorker;
import com.leyou.common.utils.UserHolder;
import com.leyou.item.client.ItemClient;
import com.leyou.item.dto.SkuDTO;
import com.leyou.order.dto.*;
import com.leyou.order.entity.Order;
import com.leyou.order.entity.OrderDetail;
import com.leyou.order.entity.OrderLogistics;
import com.leyou.order.enums.OrderStatusEnum;
import com.leyou.order.mapper.OrderDetailMapper;
import com.leyou.order.mapper.OrderLogisticsMapper;
import com.leyou.order.mapper.OrderMapper;
import com.leyou.order.utils.WxPayHelper;
import com.leyou.user.client.UserClient;
import com.leyou.user.dto.AddressDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 虎哥
 */
@Slf4j
@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper detailMapper;

    @Autowired
    private OrderLogisticsMapper logisticsMapper;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private ItemClient itemClient;

    @Autowired
    private UserClient userClient;

    @Transactional
    public long createOrder(OrderDTO orderDTO) {
        // 1.新增订单
        Order order = new Order();
        // 1.1.订单编号
        long orderId = idWorker.nextId();
        order.setOrderId(orderId);
        // 1.2.订单金额相关
        List<CartDTO> carts = orderDTO.getCarts();
        Map<Long, Integer> numMap = carts.stream().collect(Collectors.toMap(CartDTO::getSkuId, CartDTO::getNum));
        List<Long> idList = carts.stream().map(CartDTO::getSkuId).collect(Collectors.toList());
        // 1.2.1.查询sku
        List<SkuDTO> skuList = itemClient.querySkuByIds(idList);
        // 1.2.2.计算总金额
        List<OrderDetail> orderDetailList = new ArrayList<>();
        long total = 0;
        for (SkuDTO sku : skuList) {
            // 计算金额
            int num = numMap.get(sku.getId());
            total += (sku.getPrice() * num);
            // 封装OrderDetail
            OrderDetail detail = new OrderDetail();
            detail.setTitle(sku.getTitle());
            detail.setSkuId(sku.getId());
            detail.setPrice(sku.getPrice());
            detail.setOwnSpec(sku.getOwnSpec());
            detail.setNum(num);
            detail.setImage(StringUtils.substringBefore(sku.getImages(), ","));
            detail.setOrderId(orderId);
            orderDetailList.add(detail);
        }
        // 1.2.3.赋值
        order.setTotalFee(total);
        order.setPaymentType(orderDTO.getPaymentType());
        order.setPostFee(0L);
        order.setActualFee(total + order.getPostFee()/* - 优惠金额*/);

        // 1.3.订单用户信息
        Long userId = UserHolder.getUser().getId();
        order.setUserId(userId);

        // 1.4.订单状态信息
        order.setStatus(OrderStatusEnum.INIT.value());

        // 1.5.写入order表
        int count = orderMapper.insertSelective(order);
        if (count != 1) {
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }

        // 2.新增订单详情
        count = detailMapper.insertList(orderDetailList);
        if (count != orderDetailList.size()) {
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }

        // 3.新增订单物流
        Long addressId = orderDTO.getAddressId();
        // 3.1.根据addressId查询物流信息
        AddressDTO address = userClient.queryAddressById(userId, addressId);
        // 3.2.封装OrderLogistics
        OrderLogistics orderLogistics = BeanHelper.copyProperties(address, OrderLogistics.class);
        orderLogistics.setOrderId(orderId);
        // 3.3.写入表
        count = logisticsMapper.insertSelective(orderLogistics);
        if (count != 1) {
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }

        // 4.减库存
        try {
            itemClient.minusStock(numMap);
        } catch (Exception e) {
            throw new LyException(ExceptionEnum.STOCK_NOT_ENOUGH);
        }
        return orderId;
    }

    public OrderVO queryOrderById(Long id) {
        // 查询订单
        Order order = orderMapper.selectByPrimaryKey(id);
        if (order == null) {
            throw new LyException(ExceptionEnum.ORDER_NOT_FOUND);
        }

        // 判断当前订单是否属于当前用户
        Long userId = UserHolder.getUser().getId();
        if (!order.getUserId().equals(userId)) {
            // 当前订单不属于这个用户，不能查询
            throw new LyException(ExceptionEnum.ORDER_NOT_FOUND);
        }

        OrderVO orderVO = BeanHelper.copyProperties(order, OrderVO.class);
        // 查询OrderDetail
        orderVO.setDetailList(queryDetailListByOrderId(id));
        // 查询OrderLogistics
        orderVO.setLogistics(queryLogisticsByOrderId(id));

        return orderVO;
    }

    private OrderLogisticsVO queryLogisticsByOrderId(Long id) {
        OrderLogistics logistics = logisticsMapper.selectByPrimaryKey(id);
        if (logistics == null) {
            throw new LyException(ExceptionEnum.ORDER_NOT_FOUND);
        }
        return BeanHelper.copyProperties(logistics, OrderLogisticsVO.class);
    }

    private List<OrderDetailVO> queryDetailListByOrderId(Long id) {
        OrderDetail record = new OrderDetail();
        record.setOrderId(id);
        List<OrderDetail> list = detailMapper.select(record);
        if (CollectionUtils.isEmpty(list)) {
            throw new LyException(ExceptionEnum.ORDER_DETAIL_NOT_FOUND);
        }
        return BeanHelper.copyWithCollection(list, OrderDetailVO.class);
    }

    @Autowired
    private WxPayHelper payHelper;

    public String queryPayUrl(Long id) {
        // TODO 先检查Redis，如果有，直接返回
        // 查询订单
        Order order = orderMapper.selectByPrimaryKey(id);
        // 判断订单是否是未支付
        if (!order.getStatus().equals(OrderStatusEnum.INIT.value())) {
            throw new LyException(ExceptionEnum.INVALID_ORDER_STATUS);
        }
        // 获取总金额
        Long totalFee = order.getActualFee();
        // 商品描述
        String desc = "乐优商城产品";
        // 获取支付地址
        // TODO 把得到的URL写入Redis，保存2小时
        return payHelper.getPayUrl(id, desc, totalFee);
    }

    public void handleNotify(Map<String, String> data) {
        try {
            // 业务校验
            payHelper.checkResultCode(data);
            // 签名校验
            payHelper.checkSignature(data);
            // 订单状态校验（保证幂等，防止重复通知）
            String outTradeNo = data.get("out_trade_no");
            String totalFee = data.get("total_fee");
            if (StringUtils.isBlank(outTradeNo) || StringUtils.isBlank(totalFee)) {
                // 数据有误
                throw new RuntimeException("订单编号或订单金额为空！");
            }
            Long orderId = Long.valueOf(outTradeNo);
            Order order = orderMapper.selectByPrimaryKey(orderId);
            if (!order.getStatus().equals(OrderStatusEnum.INIT.value())) {
                // 说明订单已经支付过了，属于重复通知，直接返回
                return;
            }
            // 订单金额校验
            Long total = Long.valueOf(totalFee);
            if (!total.equals(order.getActualFee())) {
                throw new RuntimeException("订单金额有误，我要报警了！");
            }
            // 修改订单状态
            Order record = new Order();
            record.setOrderId(orderId);
            record.setStatus(OrderStatusEnum.PAY_UP.value());
            record.setPayTime(new Date());
            int count = orderMapper.updateByPrimaryKeySelective(record);
            if (count != 1) {
                throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
            }
            log.info("处理微信支付通知成功！{}", data);
            // TODO 删除Redis中的支付URL
        } catch (Exception e) {
            // 出现异常，通知处理失败
            log.error("处理微信支付通知失败，原因：{}", e.getMessage());
            throw new LyException(ExceptionEnum.INVALID_NOTIFY_PARAM);
        }
    }

    public Integer queryOrderState(Long id) {
        Order order = orderMapper.selectByPrimaryKey(id);
        if (order == null) {
            throw new LyException(ExceptionEnum.ORDER_NOT_FOUND);
        }
        return order.getStatus();
    }
}
