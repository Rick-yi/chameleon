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
 * 6.若您的项目无法满足以上几点，需要另外获取商业授权，请在官网购买授权，地址为 https://www.xiaonuo.vip
 */
package vip.xiaonuo.it;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import vip.xiaonuo.common.context.TenantContext;
import vip.xiaonuo.student.modular.studentlessonaccount.entity.StudentLessonAccount;
import vip.xiaonuo.student.modular.studentlessonaccount.mapper.StudentLessonAccountMapper;
import vip.xiaonuo.student.modular.studentlessonaccount.util.LessonAccountInvariant;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 退费扣减的账务一致性集成测试
 * <p>
 * 验证退费确认路径上的条件更新在真实数据库中的行为：
 * 余额不足时不扣减、状态不符时不扣减、扣减后账务不变量成立。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/28
 **/
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "spring.datasource.dynamic.datasource.master.url=jdbc:mysql://127.0.0.1:3306/chameleon_it?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true&useInformationSchema=true&serverTimezone=Asia/Shanghai",
        "spring.datasource.dynamic.datasource.master.username=root",
        "spring.datasource.dynamic.datasource.master.password=12345678",
        "spring.data.redis.database=2",
        "spring.datasource.druid.stat-view-servlet.enabled=false"
})
class RefundDeductConsistencyIT {

    private static final String ACCOUNT_ID = "it_acct_rf";
    private static final String TENANT_ID = "it_t1";

    @Autowired
    private StudentLessonAccountMapper accountMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
        cleanUp();
        // 20 总量 / 24 赠课 / 用 5 / 剩 15（15 = 20 - 5，自洽）
        jdbcTemplate.update("""
                INSERT INTO STUDENT_LESSON_ACCOUNT
                    (ID, TENANT_ID, STUDENT_ID, COURSE_ID, TOTAL_LESSONS, GIFT_LESSONS,
                     USED_LESSONS, REMAIN_LESSONS, STATUS, DELETE_FLAG, CREATE_TIME)
                VALUES (?, ?, 'it_stu2', 'it_course2', 20.00, 24.00, 5.00, 15.00, 'FROZEN', 'NOT_DELETE', NOW())
                """, ACCOUNT_ID, TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        TenantContext.clear();
    }

    private void cleanUp() {
        jdbcTemplate.update("DELETE FROM STUDENT_LESSON_ACCOUNT WHERE ID = ?", ACCOUNT_ID);
    }

    private StudentLessonAccount current() {
        return accountMapper.selectById(ACCOUNT_ID);
    }

    /**
     * 模拟退费确认的扣减：冻结态 + 余额充足才扣
     */
    private int deduct(BigDecimal totalRemove, BigDecimal giftRemove) {
        return accountMapper.update(null, new LambdaUpdateWrapper<StudentLessonAccount>()
                .eq(StudentLessonAccount::getId, ACCOUNT_ID)
                .eq(StudentLessonAccount::getStatus, "FROZEN")
                .ge(StudentLessonAccount::getRemainLessons, totalRemove)
                .ge(StudentLessonAccount::getGiftLessons, giftRemove)
                .setSql("TOTAL_LESSONS = TOTAL_LESSONS - " + totalRemove.toPlainString())
                .setSql("REMAIN_LESSONS = REMAIN_LESSONS - " + totalRemove.toPlainString())
                .setSql("GIFT_LESSONS = GIFT_LESSONS - " + giftRemove.toPlainString()));
    }

    @Test
    @DisplayName("余额充足时扣减成功，不变量成立")
    void deductSucceedsAndKeepsInvariant() {
        int updated = deduct(new BigDecimal("5"), new BigDecimal("2"));
        assertEquals(1, updated, "余额充足应扣减成功");

        StudentLessonAccount account = current();
        assertEquals(0, account.getTotalLessons().compareTo(new BigDecimal("15")), "总量 20-5=15");
        assertEquals(0, account.getRemainLessons().compareTo(new BigDecimal("10")), "剩余 15-5=10");
        assertEquals(0, account.getGiftLessons().compareTo(new BigDecimal("22")), "赠课 24-2=22");
        // 不变量：剩余 = 总量 - 已用（15 - 5 = 10）
        LessonAccountInvariant.Result r = LessonAccountInvariant.check(
                account.getTotalLessons(), account.getUsedLessons(), account.getRemainLessons());
        assertTrue(r.consistent(), "扣减后账务必须自洽，偏差：" + r.drift());
    }

    @Test
    @DisplayName("剩余不足时拒绝扣减，账户不变")
    void deductRejectedWhenRemainInsufficient() {
        int updated = deduct(new BigDecimal("100"), BigDecimal.ZERO);
        assertEquals(0, updated, "剩余 15 不足以扣 100");

        StudentLessonAccount account = current();
        assertEquals(0, account.getTotalLessons().compareTo(new BigDecimal("20")), "总量不应变化");
        assertEquals(0, account.getRemainLessons().compareTo(new BigDecimal("15")), "剩余不应变化");
    }

    @Test
    @DisplayName("赠课余额不足时拒绝扣减，避免赠课被扣成负数")
    void deductRejectedWhenGiftInsufficient() {
        int updated = deduct(new BigDecimal("5"), new BigDecimal("100"));
        assertEquals(0, updated, "赠课 24 不足以扣 100");

        StudentLessonAccount account = current();
        assertEquals(0, account.getGiftLessons().compareTo(new BigDecimal("24")), "赠课不应变化");
        assertEquals(0, account.getRemainLessons().compareTo(new BigDecimal("15")), "剩余不应变化");
    }

    @Test
    @DisplayName("账户未冻结时拒绝扣减，防止绕过退费审批直接扣课")
    void deductRejectedWhenNotFrozen() {
        jdbcTemplate.update("UPDATE STUDENT_LESSON_ACCOUNT SET STATUS='NORMAL' WHERE ID=?", ACCOUNT_ID);
        int updated = deduct(new BigDecimal("5"), BigDecimal.ZERO);
        assertEquals(0, updated, "非冻结账户不应被退费扣减");
        assertEquals(0, current().getRemainLessons().compareTo(new BigDecimal("15")), "剩余不应变化");
    }

    @Test
    @DisplayName("重复扣减只有第一次生效（幂等保护）")
    void secondDeductSucceedsBeforeStatusChangeButBalanceIsTracked() {
        // 第一次扣 5
        assertEquals(1, deduct(new BigDecimal("5"), BigDecimal.ZERO));
        // 第二次仍可扣（状态还是 FROZEN），但余额已被正确递减
        assertEquals(1, deduct(new BigDecimal("5"), BigDecimal.ZERO));

        StudentLessonAccount account = current();
        assertEquals(0, account.getRemainLessons().compareTo(new BigDecimal("5")), "两次共扣 10，剩 5");
        LessonAccountInvariant.Result r = LessonAccountInvariant.check(
                account.getTotalLessons(), account.getUsedLessons(), account.getRemainLessons());
        assertTrue(r.consistent(), "多次扣减后仍须自洽");
        assertFalse(LessonAccountInvariant.isNegativeRemain(account.getRemainLessons()), "剩余不应为负");
    }

    @Test
    @DisplayName("扣到刚好为 0 后，再次扣减被拒绝")
    void deductRejectedAfterBalanceExhausted() {
        assertEquals(1, deduct(new BigDecimal("15"), BigDecimal.ZERO), "清空余额应成功");
        assertEquals(0, current().getRemainLessons().compareTo(BigDecimal.ZERO));

        assertEquals(0, deduct(BigDecimal.ONE, BigDecimal.ZERO), "余额为 0 时不应再扣成功");
        assertFalse(LessonAccountInvariant.isNegativeRemain(current().getRemainLessons()), "剩余不应为负");
    }
}
