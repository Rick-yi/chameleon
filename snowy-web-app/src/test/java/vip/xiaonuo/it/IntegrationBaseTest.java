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

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 集成测试基座连通性自检
 * <p>
 * 连接本机独立测试库 chameleon_it（与演示库 chameleon 物理隔离），
 * 确认 Spring 上下文可启动、数据源可用。
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
class IntegrationBaseTest {

    @Test
    void contextLoads() {
        assertTrue(true, "Spring 上下文可启动即代表测试库连通");
    }
}
