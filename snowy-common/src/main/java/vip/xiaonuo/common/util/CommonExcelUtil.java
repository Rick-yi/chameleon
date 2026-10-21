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
package vip.xiaonuo.common.util;

import com.alibaba.excel.EasyExcel;

import java.io.File;
import java.util.List;

/**
 * Excel 导出工具
 * <p>
 * 报表类导出统一使用「表头 + 行数据」的动态列结构，避免为每个报表维护一份 VO。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
public class CommonExcelUtil {

    /**
     * 写出 Excel（动态表头）
     *
     * @param file      目标文件
     * @param sheetName 工作表名
     * @param headList  表头（每个元素是一列，多行表头用嵌套 List）
     * @param rowList   行数据
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    public static void write(File file, String sheetName, List<List<String>> headList, List<List<String>> rowList) {
        EasyExcel.write(file)
                .head(headList)
                .sheet(sheetName)
                .doWrite(rowList);
    }
}
