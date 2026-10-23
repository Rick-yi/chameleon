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
package vip.xiaonuo.biz.api.export;

import java.util.List;

/**
 * 异步导出构建器：业务模块实现该接口并把 Bean 交给 Spring 管理，
 * 导出任务按 type() 找到对应实现来生成表格。
 * <p>
 * 实现类在后台线程执行，方法内部可自由查库；但不要操作 HttpServletResponse。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/23 11:00
 **/
public interface BizExportBuilder {

    /** 导出类型编码（全局唯一），如 COURSE_ACADEMIC */
    String type();

    /** 文件名前缀，如「教务报表」 */
    String title();

    /**
     * 构建导出内容
     *
     * @param paramJson 前端提交的参数（JSON 字符串，由实现类自行反序列化）
     * @return 一个或多个 Sheet，至少一个
     */
    List<BizExportSheet> build(String paramJson);
}
