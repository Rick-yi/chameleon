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
package vip.xiaonuo.student.modular.student.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vip.xiaonuo.common.context.TenantContext;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.student.modular.student.param.StudentAddParam;
import vip.xiaonuo.student.modular.student.param.StudentQuickEnrollParam;
import vip.xiaonuo.student.modular.student.service.StudentEnrollService;
import vip.xiaonuo.student.modular.student.service.StudentService;
import vip.xiaonuo.student.modular.studentorder.param.StudentOrderAddParam;
import vip.xiaonuo.student.modular.studentorder.param.StudentOrderOfflinePayParam;
import vip.xiaonuo.student.modular.studentorder.service.StudentOrderService;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 学员快捷录入实现
 *
 * @author 会飞的猪
 * @date 2026/10/23 19:00
 **/
@Slf4j
@Service
public class StudentEnrollServiceImpl implements StudentEnrollService {

    /** 支持手工登记的收款渠道 */
    private static final List<String> PAY_CHANNEL_LIST = List.of("CASH", "OFFLINE", "WECHAT", "ALIPAY");

    /** 幂等键前缀；60 秒内同一请求号只允许处理一次 */
    private static final String IDEMPOTENT_KEY_PREFIX = "QuickEnroll:req:";

    private static final Duration PROCESSING_TTL = Duration.ofSeconds(60);

    private static final Duration RESULT_TTL = Duration.ofMinutes(10);

    private static final int MAX_REQUEST_ID_LENGTH = 64;

    @Resource
    private StudentService studentService;

    @Resource
    private StudentOrderService studentOrderService;

    @Resource
    private RedissonClient redissonClient;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Map<String, Object> quickEnroll(StudentQuickEnrollParam param) {
        RBucket<Object> bucket = idempotentBucket(param.getRequestId());
        if (bucket != null) {
            // 已处理完的重复请求直接返回上次结果，避免双击产生两条学员
            Object cached = bucket.get();
            if (cached instanceof Map<?, ?> cachedResult) {
                return castResult(cachedResult);
            }
            if (!bucket.trySet("PROCESSING", PROCESSING_TTL.getSeconds(), TimeUnit.SECONDS)) {
                throw new CommonException("报名正在处理中，请勿重复提交");
            }
        }
        try {
            Map<String, Object> result = doQuickEnroll(param);
            afterCommit(bucket, result, param);
            return result;
        } catch (RuntimeException e) {
            // 失败释放幂等键，允许前台修正后重试；释放失败不能掩盖原始异常
            if (bucket != null) {
                try {
                    bucket.delete();
                } catch (Exception redisException) {
                    log.warn("quickEnroll idempotent key release failed, requestId={}", param.getRequestId(), redisException);
                }
            }
            throw e;
        }
    }

    private Map<String, Object> doQuickEnroll(StudentQuickEnrollParam param) {
        if (Boolean.TRUE.equals(param.getPayNow()) && StrUtil.isBlank(param.getCoursePackageId())) {
            throw new CommonException("请选择课包后再登记收款");
        }
        String payChannel = StrUtil.blankToDefault(param.getPayChannel(), "CASH").toUpperCase();
        if (Boolean.TRUE.equals(param.getPayNow()) && !PAY_CHANNEL_LIST.contains(payChannel)) {
            throw new CommonException("不支持的收款渠道：{}", param.getPayChannel());
        }

        // 1. 建档
        StudentAddParam studentAddParam = BeanUtil.toBean(param, StudentAddParam.class);
        String studentId = studentService.add(studentAddParam);
        if (StrUtil.isBlank(studentId)) {
            throw new CommonException("学员创建失败");
        }

        // 2. 选课包则报名，按需收款
        String orderId = null;
        if (StrUtil.isNotBlank(param.getCoursePackageId())) {
            StudentOrderAddParam orderAddParam = new StudentOrderAddParam();
            orderAddParam.setStudentId(studentId);
            orderAddParam.setCoursePackageId(param.getCoursePackageId());
            orderAddParam.setPayChannel(payChannel);
            orderId = studentOrderService.add(orderAddParam);
            if (Boolean.TRUE.equals(param.getPayNow())) {
                StudentOrderOfflinePayParam payParam = new StudentOrderOfflinePayParam();
                payParam.setOrderId(orderId);
                payParam.setPaidAmount(param.getPaidAmount());
                payParam.setPayChannel(payChannel);
                payParam.setRemark(StrUtil.blankToDefault(param.getPayRemark(), "工作台快速录入"));
                studentOrderService.offlinePay(payParam);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("studentId", studentId);
        result.put("orderId", orderId);
        return result;
    }

    /**
     * 幂等键按租户 + 前端请求号隔离；缺省请求号时保持旧行为
     */
    private RBucket<Object> idempotentBucket(String requestId) {
        String trimmed = StrUtil.trim(requestId);
        if (StrUtil.isBlank(trimmed) || trimmed.length() > MAX_REQUEST_ID_LENGTH) {
            return null;
        }
        String tenantId = StrUtil.blankToDefault(TenantContext.getTenantId(), "unknown");
        return redissonClient.getBucket(IDEMPOTENT_KEY_PREFIX + tenantId + ":" + trimmed);
    }

    /**
     * 事务真正提交后再记录成功日志并缓存结果：回滚不会留下“成功”痕迹
     */
    private void afterCommit(RBucket<Object> bucket, Map<String, Object> result, StudentQuickEnrollParam param) {
        String tenantId = TenantContext.getTenantId();
        Runnable action = () -> {
            log.info("quickEnroll committed, tenantId={}, requestId={}, studentId={}, orderId={}",
                    tenantId, param.getRequestId(), result.get("studentId"), result.get("orderId"));
            if (bucket != null) {
                bucket.set(result, RESULT_TTL.toMinutes(), TimeUnit.MINUTES);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castResult(Map<?, ?> cachedResult) {
        return (Map<String, Object>) cachedResult;
    }
}
