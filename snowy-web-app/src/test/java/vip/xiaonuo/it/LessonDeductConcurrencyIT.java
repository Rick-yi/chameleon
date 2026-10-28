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
package vip.xiaonuo.it;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import vip.xiaonuo.common.context.TenantContext;
import vip.xiaonuo.student.modular.studentlessonaccount.mapper.StudentLessonAccountMapper;
import vip.xiaonuo.student.modular.studentlessonaccount.entity.StudentLessonAccount;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 课时账户并发扣减集成测试
 * <p>
 * 纯单测无法验证「条件更新在真实并发下是否真的防住超扣」。
 * 本类连本机独立测试库 chameleon_it，用多线程同时扣减同一账户，
 * 断言最终账务不变量成立、且扣减总量不超过账户余额。
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
class LessonDeductConcurrencyIT {

    private static final String ACCOUNT_ID = "it_acct_cc";
    private static final String TENANT_ID = "it_t1";

    @Autowired
    private StudentLessonAccountMapper accountMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // 表已纳入租户拦截器：无租户上下文时会被拼成 TENANT_ID='0'，故显式设置
        TenantContext.setTenantId(TENANT_ID);
        cleanUp();
        jdbcTemplate.update("""
                INSERT INTO STUDENT_LESSON_ACCOUNT
                    (ID, TENANT_ID, STUDENT_ID, COURSE_ID, TOTAL_LESSONS, GIFT_LESSONS,
                     USED_LESSONS, REMAIN_LESSONS, STATUS, DELETE_FLAG, CREATE_TIME)
                VALUES (?, ?, 'it_stu', 'it_course', 10.00, 0.00, 0.00, 10.00, 'NORMAL', 'NOT_DELETE', NOW())
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

    /**
     * 模拟生产扣课的条件更新：账户正常且剩余充足才扣
     */
    private boolean tryDeduct(BigDecimal lessons) {
        return accountMapper.update(null, new LambdaUpdateWrapper<StudentLessonAccount>()
                .eq(StudentLessonAccount::getId, ACCOUNT_ID)
                .eq(StudentLessonAccount::getStatus, "NORMAL")
                .ge(StudentLessonAccount::getRemainLessons, lessons)
                .setSql("USED_LESSONS = USED_LESSONS + " + lessons.toPlainString())
                .setSql("REMAIN_LESSONS = REMAIN_LESSONS - " + lessons.toPlainString())) > 0;
    }

    private StudentLessonAccount current() {
        return accountMapper.selectById(ACCOUNT_ID);
    }

    @Test
    @DisplayName("并发扣减不会超扣：10 课时余额，20 个并发各扣 1，只允许成功 10 次")
    void concurrentDeductNeverOverdraws() throws Exception {
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();

        List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                try {
                    start.await();
                    // ThreadLocal 不跨线程继承，并发任务内需各自设置
                    TenantContext.setTenantId(TENANT_ID);
                    if (tryDeduct(BigDecimal.ONE)) {
                        success.incrementAndGet();
                    }
                } catch (Exception ignored) {
                    // 条件更新失败属预期，业务层会抛异常由调用方处理
                } finally {
                    TenantContext.clear();
                    done.countDown();
                }
            }));
        }
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "并发扣减未在超时内完成");
        pool.shutdown();

        StudentLessonAccount account = current();
        assertEquals(10, success.get(), "余额 10 时最多只能成功扣减 10 次");
        assertEquals(0, account.getRemainLessons().compareTo(BigDecimal.ZERO), "剩余应正好为 0");
        assertEquals(0, account.getUsedLessons().compareTo(new BigDecimal("10")), "已用应正好为 10");
        // 账务不变量：剩余 = 总课时 - 已用
        assertEquals(0, account.getTotalLessons().subtract(account.getUsedLessons())
                .compareTo(account.getRemainLessons()), "账务不变量必须成立");
    }

    @Test
    @DisplayName("并发冻结后扣减全部失败，余额不变")
    void deductFailsWhenAccountFrozen() throws Exception {
        jdbcTemplate.update("UPDATE STUDENT_LESSON_ACCOUNT SET STATUS='FROZEN' WHERE ID=?", ACCOUNT_ID);

        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    // ThreadLocal 不跨线程继承，并发任务内需各自设置
                    TenantContext.setTenantId(TENANT_ID);
                    if (tryDeduct(BigDecimal.ONE)) {
                        success.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS));
        pool.shutdown();

        StudentLessonAccount account = current();
        assertEquals(0, success.get(), "冻结账户不应扣减成功");
        assertEquals(0, account.getRemainLessons().compareTo(new BigDecimal("10")), "余额不应变化");
    }

    @Test
    @DisplayName("并发扣减后账户自洽，无负数剩余")
    void accountStaysConsistentAfterConcurrency() throws Exception {
        int threads = 15;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    TenantContext.setTenantId(TENANT_ID);
                    tryDeduct(new BigDecimal("1.5"));
                } catch (Exception ignored) {
                } finally {
                    TenantContext.clear();
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS));
        pool.shutdown();

        StudentLessonAccount account = current();
        assertTrue(account.getRemainLessons().compareTo(BigDecimal.ZERO) >= 0,
                "剩余不应为负，实际：" + account.getRemainLessons());
        assertTrue(account.getUsedLessons().compareTo(account.getTotalLessons()) <= 0,
                "已用不应超过总量");
        assertEquals(0, account.getTotalLessons().subtract(account.getUsedLessons())
                .compareTo(account.getRemainLessons()), "账务不变量必须成立");
    }
}
