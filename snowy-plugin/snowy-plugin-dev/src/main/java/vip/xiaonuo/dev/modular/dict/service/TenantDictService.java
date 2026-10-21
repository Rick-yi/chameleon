/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 *
 * Snowy采用APACHE LICENSE 2.0开源协议，您在使用过程中，需要注意以下几点：
 *
 * 1.请不要删除和修改根目录下的LICENSE文件。
 * 2.请不要删除和修改Snowy源码头部的版权声明。
 * 3.本项目代码可免费商业使用，商业使用请保留源码和相关描述文件的项目出处，作者声明等。
 * 4.分发源码时候，请注明软件出处 https://www.xiaonuo.vip
 * 5.不可二次分发开源参与同类竞品，如有想法可联系团队xiaonuobase@qq.com商议合作。
 * 6.若您的项目无法满足以上几点，需要更多功能代码，获取Snowy商业授权许可，请在官网购买授权，地址为 https://www.xiaonuo.vip
 */
package vip.xiaonuo.dev.modular.dict.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import vip.xiaonuo.common.cache.CommonCacheOperator;
import vip.xiaonuo.common.context.TenantContext;
import vip.xiaonuo.dev.modular.dict.entity.DevDict;
import vip.xiaonuo.dev.modular.dict.mapper.DevDictMapper;

import java.util.Map;

/**
 * 租户感知字典翻译服务
 * <p>
 * easy-trans 的字典缓存是全局的（按字典编码分组），多租户下同名字典会互相覆盖，
 * 导致 A 租户页面显示 B 租户的字典标签。这里按「租户 + 字典编码」在 Redis 里维护独立缓存，
 * 未命中时回落到数据库查询（DEV_DICT 已是租户表，查询自动带租户条件）。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
@Service
public class TenantDictService {

    /** 根字典的父级id */
    private static final String ROOT_PARENT_ID = "0";

    /** 租户字典缓存有效期（秒） */
    private static final long CACHE_TTL_SECONDS = 3600L;

    @Resource
    private DevDictMapper devDictMapper;

    @Resource
    private CommonCacheOperator cacheOperator;

    /**
     * 租户维度的缓存键
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    public String tenantCacheKey(String typeCode) {
        return "dict:" + StrUtil.blankToDefault(TenantContext.getTenantId(), "0") + ":" + typeCode;
    }

    /**
     * 写入租户维度字典缓存
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    public void putCache(String typeCode, Map<String, String> itemMap) {
        cacheOperator.put(tenantCacheKey(typeCode), JSONUtil.toJsonStr(itemMap), CACHE_TTL_SECONDS);
    }

    /**
     * 字典翻译（租户维度）
     *
     * @param typeCode 字典编码
     * @param value    字典值
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    public String transDictValue(String typeCode, String value) {
        if (StrUtil.hasBlank(typeCode, value)) {
            return value;
        }
        Object cached = cacheOperator.get(tenantCacheKey(typeCode));
        if (cached instanceof String json) {
            try {
                Map<String, String> itemMap = JSONUtil.toBean(json, new cn.hutool.core.lang.TypeReference<Map<String, String>>() {
                }, true);
                if (itemMap != null && itemMap.containsKey(value)) {
                    return itemMap.get(value);
                }
            } catch (Exception ignored) {
                // 缓存解析失败时回落到数据库查询
            }
        }
        return queryLabel(typeCode, value);
    }

    /**
     * 数据库回落查询（带租户隔离）
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    private String queryLabel(String typeCode, String value) {
        DevDict parent = devDictMapper.selectOne(new LambdaQueryWrapper<DevDict>()
                .eq(DevDict::getDictValue, typeCode)
                .eq(DevDict::getParentId, ROOT_PARENT_ID)
                .last("limit 1"));
        if (parent == null) {
            return value;
        }
        DevDict item = devDictMapper.selectOne(new LambdaQueryWrapper<DevDict>()
                .eq(DevDict::getParentId, parent.getId())
                .eq(DevDict::getDictValue, value)
                .last("limit 1"));
        return item == null ? value : item.getDictLabel();
    }
}
