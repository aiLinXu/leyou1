package com.leyou.item.service;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.utils.BeanHelper;
import com.leyou.item.dto.SpecGroupDTO;
import com.leyou.item.dto.SpecParamDTO;
import com.leyou.item.entity.SpecGroup;
import com.leyou.item.entity.SpecParam;
import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecParamMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 虎哥
 */
@Service
public class SpecService {

    @Autowired
    private SpecGroupMapper groupMapper;

    @Autowired
    private SpecParamMapper paramMapper;

    public List<SpecGroupDTO> querySpecs(Long id) {
        // 根据分类查询规格组
        List<SpecGroupDTO> groupList = queryGroupByCid(id);

        // 查询出了当前分类下的所有规格参数
        List<SpecParamDTO> params = querySpecParams(null, id, null);
        // 尝试对params分组，根据groupId。结果应该是Map<Long, List<SpecParamDTO>>
        Map<Long, List<SpecParamDTO>> map = params.stream()
                .collect(Collectors.groupingBy(SpecParamDTO::getGroupId));
        /*Map<Long, List<SpecParamDTO>> map = new HashMap<>();
        for (SpecParamDTO param : params) {
            if(!map.containsKey(param.getGroupId())){
                // 我是本组的第一个，我来创建组
                map.put(param.getGroupId(), new ArrayList<>());
            }
            // 我不是第一个，我直接进入组内
            map.get(param.getGroupId()).add(param);
        }*/

        for (SpecGroupDTO group : groupList) {
            // 遍历params
            group.setParams(map.get(group.getId()));
        }

/*        // 遍历
        for (SpecGroupDTO group : groupList) {
            // 根据groupId查询param
             List<SpecParamDTO> params = querySpecParams(group.getId(), null, null);
            // 存入param集合
            group.setParams(params);
        }*/
        return groupList;
    }

    public List<SpecGroupDTO> queryGroupByCid(Long cid) {
        // 根据分类查询规格组
        SpecGroup group = new SpecGroup();
        group.setCid(cid);
        List<SpecGroup> list = groupMapper.select(group);
        // 健壮性判断
        if (CollectionUtils.isEmpty(list)) {
            throw new LyException(ExceptionEnum.SPEC_NOT_FOUND);
        }
        return BeanHelper.copyWithCollection(list, SpecGroupDTO.class);
    }

    public List<SpecParamDTO> querySpecParams(Long gid, Long cid, Boolean searching) {
        // 健壮性
        if (gid == null && cid == null && searching == null) {
            throw new LyException(ExceptionEnum.INVALID_PARAM_ERROR);
        }
        // 查询条件，是根据当前对象的非空字段
        SpecParam param = new SpecParam();
        param.setGroupId(gid);
        param.setCid(cid);
        param.setSearching(searching);
        List<SpecParam> list = paramMapper.select(param);
        // 健壮性判断
        if (CollectionUtils.isEmpty(list)) {
            throw new LyException(ExceptionEnum.SPEC_NOT_FOUND);
        }
        return BeanHelper.copyWithCollection(list, SpecParamDTO.class);
    }


}
