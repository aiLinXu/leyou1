package com.leyou.cart.service;

import com.leyou.cart.entity.Cart;
import com.leyou.common.auth.entity.UserInfo;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.common.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 虎哥
 */
@Service
public class CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "cart:uid:";

    public void saveCart(Cart cart) {
        // 1.获取用户id
        UserInfo user = UserHolder.getUser();
        // 2.准备key
        String key = KEY_PREFIX + user.getId().toString();
        // 3.准备hash操作对象
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(key);

        // 4.获取hashKey
        String skuId = cart.getSkuId().toString();
        // 5.判断商品是否存在购物车
        Boolean isExists = hashOps.hasKey(skuId);
        if (isExists != null && isExists) {
            // 获取缓存中的购物车
            String json = hashOps.get(skuId);
            Cart cacheCart = JsonUtils.toBean(json, Cart.class);
            // 如果存在，修改商品数量
            cacheCart.setNum(cacheCart.getNum() + cart.getNum());
            // 写回redis
            hashOps.put(skuId, JsonUtils.toString(cacheCart));
        } else {
            // 不存在，则直接添加
            hashOps.put(skuId, JsonUtils.toString(cart));
        }
    }

    public List<Cart> queryCartList() {
        // 1.获取用户id
        UserInfo user = UserHolder.getUser();
        // 2.准备key
        String key = KEY_PREFIX + user.getId().toString();
        Boolean exists = redisTemplate.hasKey(key);
        if(!exists){
            throw new LyException(ExceptionEnum.CARTS_NOT_FOUND);
        }
        // 3.准备hash操作对象
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(key);

        // 4.获取购物车中的数据
        List<String> values = hashOps.values();
        if (CollectionUtils.isEmpty(values)) {
            throw new LyException(ExceptionEnum.CARTS_NOT_FOUND);
        }
        // 5.数据转换
        return values.stream().map(json -> JsonUtils.toBean(json, Cart.class)).collect(Collectors.toList());
    }
}
