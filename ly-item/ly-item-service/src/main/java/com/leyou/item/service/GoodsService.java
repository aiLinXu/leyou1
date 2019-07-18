package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.constants.MQConstants;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.utils.BeanHelper;
import com.leyou.common.vo.PageResult;
import com.leyou.item.dto.*;
import com.leyou.item.entity.Sku;
import com.leyou.item.entity.Spu;
import com.leyou.item.entity.SpuDetail;
import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.mapper.SpuDetailMapper;
import com.leyou.item.mapper.SpuMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.leyou.common.constants.MQConstants.RoutingKey;

/**
 * @author 虎哥
 */
@Service
public class GoodsService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private SpuDetailMapper detailMapper;

    /*
    SELECT *
FROM tb_spu
WHERE `name` LIKE "%x%" AND saleable = 1
ORDER BY update_time DESC
LIMIT 0, 20
     */
    public PageResult<SpuDTO> querySpuByPage(String key, Integer page, Integer rows, Boolean saleable) {
        // 1.分页
        PageHelper.startPage(page, rows);
        // 2.条件过滤
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        // 2.1.关键字过滤
        if (StringUtils.isNotBlank(key)) {
            criteria.andLike("name", "%" + key + "%");
        }
        // 2.2.上下架过滤
        if (saleable != null) {
            criteria.andEqualTo("saleable", saleable);
        }
        // 3.排序
        example.setOrderByClause("update_time DESC");
        // 4.搜索
        List<Spu> list = spuMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(list)) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        // 5.解析数据
        // 5.1.分页数据
        PageInfo<Spu> info = new PageInfo<>(list);
        // 5.2.转换DTO
        List<SpuDTO> spuDTOList = BeanHelper.copyWithCollection(list, SpuDTO.class);
        // 5.3.处理分类和品牌的名称
        handleCategoryAndBrandName(spuDTOList);
        return new PageResult<>(info.getTotal(), info.getPages(), spuDTOList);
    }

    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    private void handleCategoryAndBrandName(List<SpuDTO> spuDTOList) {
        for (SpuDTO spuDTO : spuDTOList) {
            // 处理分类名称
            List<Long> idList = spuDTO.getCategoryIds();
            // 根据id批量查询分类
            /*List<CategoryDTO> list = categoryService.queryCategoryByIdList(idList);
            StringBuilder sb = new StringBuilder();
            for (CategoryDTO c : list) {
                sb.append(c.getName()).append("/");
            }
            sb.deleteCharAt(sb.length() - 1);*/
            String name = categoryService.queryCategoryByIdList(idList)
                    .stream()
                    .map(CategoryDTO::getName)
                    .collect(Collectors.joining("/"));

            spuDTO.setCategoryName(name);
            // 处理品牌名称
            Long brandId = spuDTO.getBrandId();
            BrandDTO brandDTO = brandService.queryById(brandId);
            spuDTO.setBrandName(brandDTO.getName());
        }
    }

    @Autowired
    private SkuMapper skuMapper;

    @Transactional
    public void saveGoods(SpuDTO spuDTO) {
        // 新增SPU
        Spu spu = BeanHelper.copyProperties(spuDTO, Spu.class);
        // 默认下架
        spu.setSaleable(false);
        int count = spuMapper.insertSelective(spu);
        if (count != 1) {
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }

        //新增SPUDetail
        SpuDetail spuDetail = BeanHelper.copyProperties(spuDTO.getSpuDetail(), SpuDetail.class);
        spuDetail.setSpuId(spu.getId());
        count = detailMapper.insertSelective(spuDetail);
        if (count != 1) {
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }

        // 新增sku
        insertSkuList(spuDTO.getSkus(), spu.getId());
    }

    private void insertSkuList(List<SkuDTO> skuDTOList, Long spuId) {
        int count;// 新增Sku
        List<Sku> skuList = BeanHelper.copyWithCollection(skuDTOList, Sku.class);
        for (Sku sku : skuList) {
            sku.setSpuId(spuId);
            // 下架状态
            sku.setEnable(false);
        }
        count = skuMapper.insertList(skuList);
        if (count != skuList.size()) {
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }
    }

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Transactional
    public void updateSaleable(Long id, Boolean saleable) {
        // 1.更新spu上下架
        Spu spu = new Spu();
        spu.setId(id);
        spu.setSaleable(saleable);
        int count = spuMapper.updateByPrimaryKeySelective(spu);
        if (count != 1) {
            throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
        }
        // 2.更新sku的上下架
        // 2.1.设置更新字段
        Sku record = new Sku();
        record.setEnable(saleable);
        // 2.2.更新的匹配条件
        Example example = new Example(Sku.class);
        example.createCriteria().andEqualTo("spuId", id);
        count = skuMapper.updateByExampleSelective(record, example);
        if (count <= 0) {
            throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
        }

        // 发送消息
        String routingKey = saleable ? RoutingKey.ITEM_UP_KEY : RoutingKey.ITEM_DOWN_KEY;
        amqpTemplate.convertAndSend(
                MQConstants.Exchange.ITEM_EXCHANGE_NAME, routingKey, id);
    }

    public SpuDetailDTO querySpuDetailBySpuId(Long spuId) {
        SpuDetail spuDetail = detailMapper.selectByPrimaryKey(spuId);
        if (spuDetail == null) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        return BeanHelper.copyProperties(spuDetail, SpuDetailDTO.class);
    }

    public List<SkuDTO> querySkuBySpuId(Long spuId) {
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        List<Sku> list = skuMapper.select(sku);
        if (CollectionUtils.isEmpty(list)) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        return BeanHelper.copyWithCollection(list, SkuDTO.class);
    }

    @Transactional
    public void updateGoods(SpuDTO spuDTO) {
        // 0.获取spu的id
        Long spuId = spuDTO.getId();
        if (spuId == null) {
            throw new LyException(ExceptionEnum.INVALID_PARAM_ERROR);
        }
        // 1.删除SKU
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        // 1.1.查询以前有几个SKU
        int size = skuMapper.selectCount(sku);
        if (size > 0) {
            // 1.2.删除
            int count = skuMapper.delete(sku);
            if (count != size) {
                throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
            }
        }
        // 2.更新SPU
        Spu spu = BeanHelper.copyProperties(spuDTO, Spu.class);
        spu.setSaleable(null);
        spu.setUpdateTime(null);
        spu.setCreateTime(null);
        int count = spuMapper.updateByPrimaryKeySelective(spu);
        if (count != 1) {
            throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
        }

        // 3.更新SpuDetail
        SpuDetail spuDetail = BeanHelper.copyProperties(spuDTO.getSpuDetail(), SpuDetail.class);
        count = detailMapper.updateByPrimaryKey(spuDetail);
        if (count != 1) {
            throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
        }

        // 4.新增SKU
        insertSkuList(spuDTO.getSkus(), spu.getId());
    }

    public SpuDTO querySpuById(Long id) {
        // 查询spu
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if (spu == null) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        SpuDTO spuDTO = BeanHelper.copyProperties(spu, SpuDTO.class);
        // 查询detail
        spuDTO.setSpuDetail(querySpuDetailBySpuId(id));
        // 查询skus
        spuDTO.setSkus(querySkuBySpuId(id));
        return spuDTO;
    }

    public List<SkuDTO> querySkuByIds(List<Long> ids) {
        List<Sku> list = skuMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(list)) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        return BeanHelper.copyWithCollection(list, SkuDTO.class);
    }

    @Transactional
    public void minusStock(Map<Long, Integer> skuMap) {
        for (Map.Entry<Long, Integer> entry : skuMap.entrySet()) {
            Long skuId = entry.getKey();
            Integer num = entry.getValue();
            // 减库存
            try {
                skuMapper.minusStock(skuId, num);
            } catch (Exception e) {
                throw new LyException(ExceptionEnum.STOCK_NOT_ENOUGH);
            }
        }
    }
}
