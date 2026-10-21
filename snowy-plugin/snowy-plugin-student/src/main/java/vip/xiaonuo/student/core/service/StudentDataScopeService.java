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
package vip.xiaonuo.student.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import vip.xiaonuo.auth.core.util.StpDataScopeUtil;
import vip.xiaonuo.student.modular.student.entity.Student;
import vip.xiaonuo.student.modular.student.mapper.StudentMapper;
import vip.xiaonuo.student.modular.studentorder.entity.StudentOrder;
import vip.xiaonuo.student.modular.studentorder.mapper.StudentOrderMapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 学员域数据范围服务
 * <p>
 * 业务表的 {@code CAMPUS_ID} 对应 {@code SYS_ORG} 中的校区机构，
 * 因此把登录用户的可见机构集合直接作为校区过滤条件即可。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
@Component
public class StudentDataScopeService {

    /** 无可见数据时使用的占位值（不可能存在的 id） */
    public static final String NO_DATA_ID = "__NO_DATA__";

    @Resource
    private StudentMapper studentMapper;

    @Resource
    private StudentOrderMapper studentOrderMapper;

    /**
     * 可见校区id
     *
     * @return null 表示不限制；空集合表示没有可见数据
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    public List<String> visibleCampusIds() {
        return StpDataScopeUtil.visibleOrgIds();
    }

    /**
     * 可见学员id
     *
     * @return null 表示不限制；空集合表示没有可见数据
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    public List<String> visibleStudentIdList() {
        List<String> campusIdList = visibleCampusIds();
        if (campusIdList == null) {
            return null;
        }
        if (campusIdList.isEmpty()) {
            return List.of();
        }
        return studentMapper.selectList(new LambdaQueryWrapper<Student>()
                        .select(Student::getId)
                        .in(Student::getCampusId, campusIdList))
                .stream().map(Student::getId).collect(Collectors.toList());
    }

    /**
     * 可见订单id
     *
     * @return null 表示不限制；空集合表示没有可见数据
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    public List<String> visibleOrderIdList() {
        List<String> campusIdList = visibleCampusIds();
        if (campusIdList == null) {
            return null;
        }
        if (campusIdList.isEmpty()) {
            return List.of();
        }
        return studentOrderMapper.selectList(new LambdaQueryWrapper<StudentOrder>()
                        .select(StudentOrder::getId)
                        .in(StudentOrder::getCampusId, campusIdList))
                .stream().map(StudentOrder::getId).collect(Collectors.toList());
    }
}
