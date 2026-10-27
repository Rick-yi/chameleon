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
package vip.xiaonuo.student.modular.student.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.util.CommonDownloadUtil;
import vip.xiaonuo.common.util.CommonExcelUtil;
import vip.xiaonuo.student.core.enums.StudentStatusMachineEnum;
import vip.xiaonuo.student.modular.student.entity.Student;
import vip.xiaonuo.student.modular.student.mapper.StudentMapper;
import vip.xiaonuo.student.modular.student.param.StudentAddParam;
import vip.xiaonuo.student.modular.student.param.StudentImportParam;
import vip.xiaonuo.student.modular.student.param.StudentPageParam;
import vip.xiaonuo.student.modular.student.service.StudentImportExportService;
import vip.xiaonuo.student.modular.student.service.StudentService;
import vip.xiaonuo.student.modular.studenttag.entity.StudentTag;
import vip.xiaonuo.sys.api.SysOrgApi;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 学员导入导出实现
 *
 * @author 会飞的猪
 * @date 2026/10/27
 **/
@Service
public class StudentImportExportServiceImpl implements StudentImportExportService {

    /** 单次导入最大行数 */
    private static final int MAX_IMPORT_ROWS = 2000;

    /** 单次导出最大行数 */
    private static final int MAX_EXPORT_ROWS = 5000;

    @Resource
    private StudentService studentService;

    @Resource
    private StudentMapper studentMapper;

    @Resource
    private SysOrgApi sysOrgApi;

    @Override
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        String fileName = "学员导入模板.xlsx";
        File file = FileUtil.file(FileUtil.getTmpDir(), fileName);
        try {
            List<List<String>> headList = List.of(
                    List.of("学员姓名"), List.of("性别"), List.of("生日"), List.of("家长姓名"),
                    List.of("家长手机"), List.of("校区"), List.of("状态"), List.of("备注"));
            List<List<String>> instructionRowList = List.of(
                    List.of("学员姓名必填；性别填男/女；生日格式 yyyy-MM-dd；家长手机建议填写，用于查重与家长端绑定。"),
                    List.of("校区填机构内已有校区名称；留空时使用当前账号可见的默认校区。"),
                    List.of("状态可填：在读、停课、休学、退学、结课；留空默认在读。"),
                    List.of("导入按行校验，失败行会在页面上回显行号与原因，修正后可只保留失败行再次上传重试。"));
            CommonExcelUtil.writeMulti(file, List.of(
                    new CommonExcelUtil.Sheet("学员导入", headList, new ArrayList<>()),
                    new CommonExcelUtil.Sheet("填写说明", List.of(List.of("填写说明")), instructionRowList)));
            CommonDownloadUtil.download(file, response);
        } finally {
            FileUtil.del(file);
        }
    }

    @Override
    public Map<String, Object> importStudents(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CommonException("请选择要导入的 Excel 文件");
        }
        List<StudentImportParam> rowList;
        try {
            rowList = EasyExcel.read(file.getInputStream()).head(StudentImportParam.class)
                    .sheet().headRowNumber(1).doReadSync();
        } catch (Exception e) {
            throw new CommonException("Excel 解析失败，请使用系统模板重新填写后上传");
        }
        if (ObjectUtil.isEmpty(rowList)) {
            throw new CommonException("Excel 中没有可导入的数据行");
        }
        if (rowList.size() > MAX_IMPORT_ROWS) {
            throw new CommonException("单次最多导入 {} 行，请拆分后重试", MAX_IMPORT_ROWS);
        }
        Map<String, String> campusNameMap = campusNameMap();
        int successCount = 0;
        List<Map<String, Object>> errorDetail = new ArrayList<>();
        Set<String> fileDuplicateSet = new HashSet<>();
        for (int i = 0; i < rowList.size(); i++) {
            StudentImportParam row = rowList.get(i);
            if (isEmptyRow(row)) {
                continue;
            }
            try {
                importOne(row, campusNameMap, fileDuplicateSet);
                successCount++;
            } catch (Exception e) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("index", i);
                error.put("rowNum", i + 2);
                error.put("name", StrUtil.trim(row.getName()));
                error.put("parentPhone", StrUtil.trim(row.getParentPhone()));
                error.put("campusName", StrUtil.trim(row.getCampusName()));
                error.put("msg", e instanceof CommonException ? e.getMessage() : "导入失败：" + e.getMessage());
                errorDetail.add(error);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCount", rowList.size());
        result.put("successCount", successCount);
        result.put("errorCount", errorDetail.size());
        result.put("errorDetail", errorDetail);
        return result;
    }

    /**
     * 导入一行（失败抛出业务异常，由上层收集行号）
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    private void importOne(StudentImportParam row, Map<String, String> campusNameMap, Set<String> fileDuplicateSet) {
        String name = StrUtil.trim(row.getName());
        if (StrUtil.isBlank(name)) {
            throw new CommonException("学员姓名不能为空");
        }
        String phone = StrUtil.trim(row.getParentPhone());
        String duplicateKey = name + "|" + StrUtil.blankToDefault(phone, "");
        if (StrUtil.isNotBlank(phone)) {
            if (!fileDuplicateSet.add(duplicateKey)) {
                throw new CommonException("文件内存在重复的姓名与家长手机");
            }
            long exists = studentMapper.selectCount(new LambdaQueryWrapper<Student>()
                    .eq(Student::getName, name)
                    .eq(Student::getParentPhone, phone));
            if (exists > 0) {
                throw new CommonException("系统中已存在同名同手机号的学员，请勿重复导入");
            }
        }
        StudentAddParam param = new StudentAddParam();
        param.setName(name);
        param.setGender(normalizeGender(row.getGender()));
        param.setBirthday(parseBirthday(row.getBirthday()));
        param.setParentName(StrUtil.trim(row.getParentName()));
        param.setParentPhone(StrUtil.trim(row.getParentPhone()));
        param.setRemark(StrUtil.trim(row.getRemark()));
        String campusName = StrUtil.trim(row.getCampusName());
        if (StrUtil.isNotBlank(campusName)) {
            String campusId = campusNameMap.get(campusName);
            if (StrUtil.isBlank(campusId)) {
                throw new CommonException("校区【" + campusName + "】不存在，请填写机构内已有校区名称");
            }
            param.setCampusId(campusId);
        }
        String status = resolveStatus(row.getStatus());
        studentService.addForImport(param, status);
    }

    /**
     * 空行判断（姓名、手机、家长、校区、备注都为空）
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    private boolean isEmptyRow(StudentImportParam row) {
        return StrUtil.isBlank(row.getName()) && StrUtil.isBlank(row.getParentPhone())
                && StrUtil.isBlank(row.getParentName()) && StrUtil.isBlank(row.getCampusName())
                && StrUtil.isBlank(row.getRemark()) && StrUtil.isBlank(row.getBirthday());
    }

    private String normalizeGender(String gender) {
        String text = StrUtil.trim(gender);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        if ("男".equals(text) || "MALE".equalsIgnoreCase(text)) {
            return "男";
        }
        if ("女".equals(text) || "FEMALE".equalsIgnoreCase(text)) {
            return "女";
        }
        return text;
    }

    private Date parseBirthday(String birthday) {
        String text = StrUtil.trim(birthday);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        try {
            return DateUtil.parse(text);
        } catch (Exception e) {
            throw new CommonException("生日格式不正确，请使用 yyyy-MM-dd");
        }
    }

    private String resolveStatus(String status) {
        String text = StrUtil.trim(status);
        if (StrUtil.isBlank(text)) {
            return StudentStatusMachineEnum.STUDYING.getValue();
        }
        for (StudentStatusMachineEnum item : StudentStatusMachineEnum.values()) {
            if (item.getValue().equalsIgnoreCase(text) || item.getMessage().equals(text)) {
                return item.getValue();
            }
        }
        throw new CommonException("状态【" + status + "】不合法，请填写：在读/停课/休学/退学/结课");
    }

    @Override
    public void exportStudents(StudentPageParam studentPageParam, HttpServletResponse response) throws IOException {
        List<Student> studentList = studentService.listForExport(studentPageParam);
        if (ObjectUtil.isEmpty(studentList)) {
            throw new CommonException("当前筛选条件下没有可导出的学员");
        }
        if (studentList.size() > MAX_EXPORT_ROWS) {
            throw new CommonException("学员数量超过 {} 条，请缩小筛选范围后导出", MAX_EXPORT_ROWS);
        }
        Map<String, String> campusNameMap = campusNameMapById(studentList);
        List<List<String>> headList = new ArrayList<>();
        for (String head : List.of("学员姓名", "性别", "生日", "家长姓名", "家长手机", "校区", "状态",
                "状态原因", "标签", "剩余课时", "课包到期", "所在班级", "待收款", "备注", "创建时间")) {
            headList.add(List.of(head));
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<List<String>> rowList = new ArrayList<>();
        for (Student student : studentList) {
            List<String> row = new ArrayList<>();
            row.add(StrUtil.blankToDefault(student.getName(), ""));
            row.add(StrUtil.blankToDefault(student.getGender(), ""));
            row.add(student.getBirthday() == null ? "" : dateFormat.format(student.getBirthday()));
            row.add(StrUtil.blankToDefault(student.getParentName(), ""));
            row.add(StrUtil.blankToDefault(student.getParentPhone(), ""));
            row.add(campusNameMap.getOrDefault(student.getCampusId(), ""));
            row.add(StudentStatusMachineEnum.messageOf(student.getStatus()));
            row.add(StrUtil.blankToDefault(student.getStatusReason(), ""));
            row.add(tagNames(student.getTagList()));
            row.add(student.getRemainLessons() == null ? "0" : student.getRemainLessons().stripTrailingZeros().toPlainString());
            row.add(student.getAccountExpireTime() == null ? "" : dateFormat.format(student.getAccountExpireTime()));
            row.add(StrUtil.blankToDefault(student.getClassNames(), ""));
            row.add(student.getUnpaidAmount() == null ? "0" : student.getUnpaidAmount().stripTrailingZeros().toPlainString());
            row.add(StrUtil.blankToDefault(student.getRemark(), ""));
            row.add(student.getCreateTime() == null ? "" : dateTimeFormat.format(student.getCreateTime()));
            rowList.add(row);
        }
        String fileName = "学员档案_" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + ".xlsx";
        File file = FileUtil.file(FileUtil.getTmpDir(), fileName);
        try {
            CommonExcelUtil.write(file, "学员档案", headList, rowList);
            CommonDownloadUtil.download(file, response);
        } finally {
            FileUtil.del(file);
        }
    }

    private String tagNames(List<StudentTag> tagList) {
        if (ObjectUtil.isEmpty(tagList)) {
            return "";
        }
        return String.join("、", tagList.stream().map(StudentTag::getName).toList());
    }

    /**
     * 校区名称 → id（仅机构内的部门/校区）
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    private Map<String, String> campusNameMap() {
        Map<String, String> result = new HashMap<>();
        collectCampus(sysOrgApi.orgTreeSelector("0", ""), result);
        return result;
    }

    private void collectCampus(List<JSONObject> orgList, Map<String, String> result) {
        if (ObjectUtil.isEmpty(orgList)) {
            return;
        }
        for (JSONObject org : orgList) {
            String category = org.getStr("category");
            if ("DEPT".equalsIgnoreCase(category) || "CAMPUS".equalsIgnoreCase(category)) {
                String name = StrUtil.trim(org.getStr("name"));
                if (StrUtil.isNotBlank(name)) {
                    result.putIfAbsent(name, org.getStr("id"));
                }
            }
            collectCampus(org.getBeanList("children", JSONObject.class), result);
        }
    }

    /**
     * 导出用：按学员校区id批量取名称
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    private Map<String, String> campusNameMapById(List<Student> studentList) {
        List<String> campusIdList = studentList.stream().map(Student::getCampusId)
                .filter(StrUtil::isNotBlank).distinct().toList();
        Map<String, String> result = new HashMap<>();
        if (campusIdList.isEmpty()) {
            return result;
        }
        for (JSONObject org : sysOrgApi.getOrgListByIdListWithoutException(campusIdList)) {
            result.put(org.getStr("id"), org.getStr("name"));
        }
        return result;
    }
}
