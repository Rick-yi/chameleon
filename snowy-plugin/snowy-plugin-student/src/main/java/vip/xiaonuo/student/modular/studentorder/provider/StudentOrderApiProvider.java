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
package vip.xiaonuo.student.modular.studentorder.provider;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import vip.xiaonuo.student.api.StudentOrderApi;
import vip.xiaonuo.student.api.dto.StudentOrderRefDTO;
import vip.xiaonuo.student.modular.studentorder.entity.StudentOrder;
import vip.xiaonuo.student.modular.studentorder.enums.StudentOrderStatusEnum;
import vip.xiaonuo.student.modular.studentorder.mapper.StudentOrderMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单域查询API实现类
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
@Service
public class StudentOrderApiProvider implements StudentOrderApi {

    @Resource
    private StudentOrderMapper studentOrderMapper;

    @Override
    public List<StudentOrderRefDTO> listPaidOrderRefs(List<String> studentIdList) {
        if (ObjectUtil.isEmpty(studentIdList)) {
            return new ArrayList<>();
        }
        List<StudentOrder> orderList = studentOrderMapper.selectList(new LambdaQueryWrapper<StudentOrder>()
                .in(StudentOrder::getStudentId, studentIdList)
                .eq(StudentOrder::getStatus, StudentOrderStatusEnum.PAID.getValue())
                .orderByDesc(StudentOrder::getCreateTime));
        List<StudentOrderRefDTO> refList = new ArrayList<>();
        for (StudentOrder order : orderList) {
            StudentOrderRefDTO dto = new StudentOrderRefDTO();
            dto.setStudentId(order.getStudentId());
            dto.setCourseId(order.getCourseId());
            dto.setCoursePackageId(order.getCoursePackageId());
            dto.setCreateTime(order.getCreateTime());
            refList.add(dto);
        }
        return refList;
    }
}
