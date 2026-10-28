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
package vip.xiaonuo.student.modular.studenttag.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.common.context.TenantContext;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.student.modular.student.entity.Student;
import vip.xiaonuo.student.modular.student.mapper.StudentMapper;
import vip.xiaonuo.student.modular.studenttag.entity.StudentTag;
import vip.xiaonuo.student.modular.studenttag.entity.StudentTagRel;
import vip.xiaonuo.student.modular.studenttag.mapper.StudentTagMapper;
import vip.xiaonuo.student.modular.studenttag.mapper.StudentTagRelMapper;
import vip.xiaonuo.student.modular.studenttag.param.StudentTagAddParam;
import vip.xiaonuo.student.modular.studenttag.param.StudentTagBindParam;
import vip.xiaonuo.student.modular.studenttag.param.StudentTagEditParam;
import vip.xiaonuo.student.modular.studenttag.param.StudentTagIdParam;
import vip.xiaonuo.student.modular.studenttag.service.StudentTagService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学员标签Service实现
 * <p>
 * STUDENT_TAG / STUDENT_TAG_REL 未登记到全局租户表清单（本次改动不修改 GlobalConfigure），
 * 因此所有读写都显式使用当前租户 TENANT_ID，避免跨机构串数据。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/27
 **/
@Service
public class StudentTagServiceImpl implements StudentTagService {

    @Resource
    private StudentTagMapper studentTagMapper;

    @Resource
    private StudentTagRelMapper studentTagRelMapper;

    @Resource
    private StudentMapper studentMapper;

    @Override
    public List<StudentTag> listTags() {
        return studentTagMapper.selectList(new LambdaQueryWrapper<StudentTag>()
                .eq(StudentTag::getTenantId, currentTenantId())
                .orderByAsc(StudentTag::getSortCode)
                .orderByAsc(StudentTag::getName));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String add(StudentTagAddParam param) {
        String tenantId = currentTenantId();
        long exists = studentTagMapper.selectCount(new LambdaQueryWrapper<StudentTag>()
                .eq(StudentTag::getTenantId, tenantId)
                .eq(StudentTag::getName, param.getName().trim()));
        if (exists > 0) {
            throw new CommonException("标签【{}】已存在", param.getName());
        }
        StudentTag tag = new StudentTag();
        tag.setTenantId(tenantId);
        tag.setName(param.getName().trim());
        tag.setColor(StrUtil.blankToDefault(param.getColor(), "blue"));
        tag.setSortCode(param.getSortCode() == null ? 0 : param.getSortCode());
        tag.setRemark(param.getRemark());
        studentTagMapper.insert(tag);
        return tag.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(StudentTagEditParam param) {
        StudentTag tag = getTag(param.getId());
        long exists = studentTagMapper.selectCount(new LambdaQueryWrapper<StudentTag>()
                .eq(StudentTag::getTenantId, tag.getTenantId())
                .eq(StudentTag::getName, param.getName().trim())
                .ne(StudentTag::getId, tag.getId()));
        if (exists > 0) {
            throw new CommonException("标签【{}】已存在", param.getName());
        }
        tag.setName(param.getName().trim());
        tag.setColor(StrUtil.blankToDefault(param.getColor(), "blue"));
        tag.setSortCode(param.getSortCode() == null ? 0 : param.getSortCode());
        tag.setRemark(param.getRemark());
        studentTagMapper.updateById(tag);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(StudentTagIdParam param) {
        StudentTag tag = getTag(param.getId());
        studentTagRelMapper.deleteByTag(tag.getTenantId(), tag.getId());
        studentTagMapper.deleteById(tag.getId());
    }

    @Override
    public List<StudentTag> listByStudent(String studentId) {
        if (StrUtil.isBlank(studentId)) {
            return List.of();
        }
        List<String> tagIdList = studentTagRelMapper.selectList(new LambdaQueryWrapper<StudentTagRel>()
                        .eq(StudentTagRel::getTenantId, currentTenantId())
                        .eq(StudentTagRel::getStudentId, studentId))
                .stream().map(StudentTagRel::getTagId).filter(StrUtil::isNotBlank).distinct().toList();
        return listTagsByIds(tagIdList);
    }

    @Override
    public Map<String, List<StudentTag>> mapByStudentIds(List<String> studentIdList) {
        Map<String, List<StudentTag>> result = new HashMap<>();
        if (ObjectUtil.isEmpty(studentIdList)) {
            return result;
        }
        List<String> idList = studentIdList.stream().filter(StrUtil::isNotBlank).distinct().toList();
        if (idList.isEmpty()) {
            return result;
        }
        List<StudentTagRel> relList = studentTagRelMapper.selectList(new LambdaQueryWrapper<StudentTagRel>()
                .eq(StudentTagRel::getTenantId, currentTenantId())
                .in(StudentTagRel::getStudentId, idList));
        if (relList.isEmpty()) {
            return result;
        }
        List<String> tagIdList = relList.stream().map(StudentTagRel::getTagId)
                .filter(StrUtil::isNotBlank).distinct().toList();
        Map<String, StudentTag> tagMap = listTagsByIds(tagIdList).stream()
                .collect(Collectors.toMap(StudentTag::getId, item -> item, (first, second) -> first));
        for (StudentTagRel rel : relList) {
            StudentTag tag = tagMap.get(rel.getTagId());
            if (tag == null) {
                continue;
            }
            result.computeIfAbsent(rel.getStudentId(), key -> new ArrayList<>()).add(tag);
        }
        // 按排序号稳定输出，避免列表标签顺序抖动
        result.values().forEach(list -> list.sort((a, b) ->
                Integer.compare(a.getSortCode() == null ? 0 : a.getSortCode(),
                        b.getSortCode() == null ? 0 : b.getSortCode())));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void bind(StudentTagBindParam param) {
        String tenantId = currentTenantId();
        Student student = studentMapper.selectById(param.getStudentId());
        if (ObjectUtil.isEmpty(student)) {
            throw new CommonException("学员不存在，id值为：{}", param.getStudentId());
        }
        List<String> tagIdList = ObjectUtil.isEmpty(param.getTagIdList()) ? List.of()
                : param.getTagIdList().stream().filter(StrUtil::isNotBlank).distinct().toList();
        if (!tagIdList.isEmpty()) {
            List<StudentTag> tagList = listTagsByIds(tagIdList);
            if (tagList.size() != tagIdList.size()) {
                throw new CommonException("部分标签不存在或已被删除，请刷新后重试");
            }
        }
        studentTagRelMapper.deleteByStudent(tenantId, param.getStudentId());
        for (String tagId : tagIdList) {
            StudentTagRel rel = new StudentTagRel();
            rel.setTenantId(tenantId);
            rel.setStudentId(param.getStudentId());
            rel.setTagId(tagId);
            studentTagRelMapper.insert(rel);
        }
    }

    private List<StudentTag> listTagsByIds(List<String> tagIdList) {
        if (ObjectUtil.isEmpty(tagIdList)) {
            return new ArrayList<>();
        }
        return studentTagMapper.selectList(new LambdaQueryWrapper<StudentTag>()
                .eq(StudentTag::getTenantId, currentTenantId())
                .in(StudentTag::getId, tagIdList)
                .orderByAsc(StudentTag::getSortCode)
                .orderByAsc(StudentTag::getName));
    }

    private StudentTag getTag(String id) {
        StudentTag tag = studentTagMapper.selectOne(new LambdaQueryWrapper<StudentTag>()
                .eq(StudentTag::getTenantId, currentTenantId())
                .eq(StudentTag::getId, id));
        if (ObjectUtil.isEmpty(tag)) {
            throw new CommonException("标签不存在，id值为：{}", id);
        }
        return tag;
    }

    /**
     * 当前租户id
     * <p>
     * 标签表现已纳入全局租户拦截器，这里的显式条件作为双保险保留；
     * 取值统一委托给 TenantContext，避免兜底值在多处各写一份。
     * </p>
     */
    private static String currentTenantId() {
        return TenantContext.currentTenantId();
    }
}
