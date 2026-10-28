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
package vip.xiaonuo.student.modular.studentsundry.init;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vip.xiaonuo.common.context.StartupMigrationRegistry;
import vip.xiaonuo.common.context.StartupPhase;

/**
 * 学杂费结构升级
 * <p>
 * 学杂费（校服、教材、活动费等）与课包订单是两套账：
 * 课包订单产生课时账户、按课消确认收入；学杂费是一次性收入，没有课时、没有消课。
 * 因此用独立表承载，避免混进 {@code STUDENT_ORDER} 后污染客单价、续费率、课消确认等口径。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/28
 **/
@Slf4j
@Component
public class StudentSundrySchemaMigration implements ApplicationListener<ApplicationStartedEvent>, Ordered {

    /** 学杂费单据表 */
    private static final String SUNDRY_ORDER_DDL = """
            CREATE TABLE IF NOT EXISTS `STUDENT_SUNDRY_ORDER` (
              `ID` varchar(20) NOT NULL COMMENT '主键',
              `TENANT_ID` varchar(20) NOT NULL DEFAULT '0' COMMENT '租户id',
              `STUDENT_ID` varchar(20) NOT NULL COMMENT '学员id',
              `CAMPUS_ID` varchar(20) NULL DEFAULT NULL COMMENT '校区id',
              `FEE_TYPE` varchar(30) NOT NULL DEFAULT 'OTHER' COMMENT '费用类型(UNIFORM校服/BOOK教材/ACTIVITY活动费/MEAL餐费/OTHER其他)',
              `ITEM_NAME` varchar(200) NOT NULL COMMENT '物品/费用名称',
              `QUANTITY` decimal(10,2) NOT NULL DEFAULT 1.00 COMMENT '数量',
              `UNIT_PRICE` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '单价',
              `AMOUNT` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '应收金额',
              `PAID_AMOUNT` decimal(12,2) NULL DEFAULT 0.00 COMMENT '实收金额',
              `REFUNDED_AMOUNT` decimal(12,2) NULL DEFAULT 0.00 COMMENT '已退金额',
              `PAY_CHANNEL` varchar(20) NULL DEFAULT NULL COMMENT '收款渠道',
              `TRADE_NO` varchar(64) NULL DEFAULT NULL COMMENT '外部交易号',
              `PAY_TIME` datetime NULL DEFAULT NULL COMMENT '收款时间',
              `REFUND_TIME` datetime NULL DEFAULT NULL COMMENT '退费时间',
              `STATUS` varchar(20) NOT NULL DEFAULT 'UNPAID' COMMENT '状态(UNPAID/PENDING/PAID/REFUNDING/REFUNDED/CANCELED)',
              `REMARK` varchar(500) NULL DEFAULT NULL COMMENT '备注',
              `RECEIPT_NO` varchar(32) NULL DEFAULT NULL COMMENT '收据号',
              `DELETE_FLAG` varchar(255) NULL DEFAULT NULL COMMENT '删除标志',
              `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
              `CREATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '创建人',
              `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
              `UPDATE_USER` varchar(20) NULL DEFAULT NULL COMMENT '更新人',
              PRIMARY KEY (`ID`) USING BTREE,
              INDEX `IDX_SUNDRY_TENANT_STUDENT` (`TENANT_ID`, `STUDENT_ID`),
              INDEX `IDX_SUNDRY_TENANT_TIME` (`TENANT_ID`, `CREATE_TIME`),
              INDEX `IDX_SUNDRY_TENANT_STATUS` (`TENANT_ID`, `STATUS`)
            ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci
              COMMENT = '学员学杂费单据（与课包订单分账）' ROW_FORMAT = Dynamic
            """;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        try {
            if (!tableExists("STUDENT_SUNDRY_ORDER")) {
                jdbcTemplate.execute(SUNDRY_ORDER_DDL);
                log.info(">>> 学杂费结构升级：STUDENT_SUNDRY_ORDER 已创建");
            }
        } catch (Exception e) {
            StartupMigrationRegistry.record("学杂费结构升级",
                    "请手工创建 STUDENT_SUNDRY_ORDER 表：" + e.getMessage());
            log.error(">>> 学杂费结构升级失败", e);
        }
    }

    /**
     * 判断表是否存在
     */
    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    @Override
    public int getOrder() {
        // 业务域结构阶段，与学员档案结构同级
        return StartupPhase.DOMAIN_SCHEMA;
    }
}
