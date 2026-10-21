/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 *
 * Snowy采用APACHE LICENSE 2.0开源协议，您使用时需要注意以下几点：
 * 1.请不要删除和修改根目录下的LICENSE文件。
 * 2.请不要删除和修改Snowy源码头部的版权声明。
 * 3.本项目代码可免费商业使用，商业使用请保留源码和相关描述文件的项目出处，作者声明等。
 * 4.分发源码时候，请注明软件出处 https://www.xiaonuo.vip
 * 5.不可二次分发开源参与同类竞品，如有想法可联系团队xiaonuobase@163.com商议合作。
 * 6.如需更多功能代码，获取Snowy商业授权许可，请在官网购买授权，地址为：https://www.xiaonuo.vip
 */
package vip.xiaonuo.student.modular.student.provider;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import vip.xiaonuo.student.api.StudentApi;
import vip.xiaonuo.student.api.dto.StudentBriefDTO;
import vip.xiaonuo.student.modular.student.entity.Student;
import vip.xiaonuo.student.modular.student.mapper.StudentMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * 学员域查询API实现类
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
@Service
public class StudentApiProvider implements StudentApi {

    @Resource
    private StudentMapper studentMapper;

    @Override
    public List<StudentBriefDTO> listStudentBrief(List<String> studentIdList) {
        List<StudentBriefDTO> briefList = new ArrayList<>();
        if (studentIdList == null || studentIdList.isEmpty()) {
            return briefList;
        }
        studentMapper.selectBatchIds(studentIdList).forEach(student -> {
            StudentBriefDTO brief = new StudentBriefDTO();
            brief.setId(student.getId());
            brief.setName(student.getName());
            brief.setStatus(student.getStatus());
            brief.setParentPhone(student.getParentPhone());
            brief.setCampusId(student.getCampusId());
            briefList.add(brief);
        });
        return briefList;
    }
}
