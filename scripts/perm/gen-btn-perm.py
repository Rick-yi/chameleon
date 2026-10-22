"""给业务页面的操作按钮加上按钮级权限（hasPerm）。

按钮码 = 该动作调用的接口地址，与后端角色模板授权的接口一致：
  有权限 → 按钮可见；无权限 → 按钮隐藏。
"""
import pathlib

EDITS = [
    # ---------------- 学员管理 ----------------
    ("views/student/index.vue", [
        ('<a-button type="primary" @click="onOpenAdd">',
         '<a-button v-if="hasPerm(\'/student/add\')" type="primary" @click="onOpenAdd">'),
        ('<a @click="onOpenSignup(record)">报名</a>',
         '<a v-if="hasPerm(\'/student/order/add\')" @click="onOpenSignup(record)">报名</a>'),
    ]),
    # ---------------- 收款台 ----------------
    ("views/student/order.vue", [
        ('<a-button v-if="record.status === \'UNPAID\'" type="link" size="small" @click="onOpenPay(record)">收款</a-button>',
         '<a-button v-if="record.status === \'UNPAID\' && hasPerm(\'/student/order/offlinePay\')" type="link" size="small" @click="onOpenPay(record)">收款</a-button>'),
        ('<a-button v-if="record.status === \'UNPAID\'" type="link" size="small" @click="onCancelOrder(record)">取消</a-button>',
         '<a-button v-if="record.status === \'UNPAID\' && hasPerm(\'/student/order/cancel\')" type="link" size="small" @click="onCancelOrder(record)">取消</a-button>'),
        ('<a-button v-if="record.status === \'PAID\'" type="link" size="small" @click="onOpenRefund(record)">退费</a-button>',
         '<a-button v-if="record.status === \'PAID\' && hasPerm(\'/student/refund/apply\')" type="link" size="small" @click="onOpenRefund(record)">退费</a-button>'),
        ('<a-button v-if="record.status === \'PENDING\'" type="link" size="small" @click="onOpenApprove(record)">审批</a-button>',
         '<a-button v-if="record.status === \'PENDING\' && hasPerm(\'/student/refund/approve\')" type="link" size="small" @click="onOpenApprove(record)">审批</a-button>'),
        ('<a-button v-if="record.status === \'TO_REFUND\'" type="link" size="small" @click="onConfirmRefund(record)">确认退款</a-button>',
         '<a-button v-if="record.status === \'TO_REFUND\' && hasPerm(\'/student/refund/confirm\')" type="link" size="small" @click="onConfirmRefund(record)">确认退款</a-button>'),
    ]),
    # ---------------- 招生线索 ----------------
    ("views/crm/index.vue", [
        ('<a-button type="primary" @click="onOpenAdd">',
         '<a-button v-if="hasPerm(\'/crm/lead/add\')" type="primary" @click="onOpenAdd">'),
        ('<a @click="onOpenFollow(record)">跟进</a>',
         '<a v-if="hasPerm(\'/crm/follow/add\')" @click="onOpenFollow(record)">跟进</a>'),
    ]),
    # ---------------- 课程管理 ----------------
    ("views/course/index.vue", [
        ('<a-button type="primary" @click="onOpenAdd">',
         '<a-button v-if="hasPerm(\'/course/add\')" type="primary" @click="onOpenAdd">'),
        ('<a @click="onOpenEdit(record)">编辑</a>',
         '<a v-if="hasPerm(\'/course/edit\')" @click="onOpenEdit(record)">编辑</a>'),
    ]),
    # ---------------- 课包管理 ----------------
    ("views/course/package.vue", [
        ('<a-button type="primary" @click="onOpenAdd">',
         '<a-button v-if="hasPerm(\'/course/package/add\')" type="primary" @click="onOpenAdd">'),
        ('<a @click="onOpenEdit(record)">编辑</a>',
         '<a v-if="hasPerm(\'/course/package/edit\')" @click="onOpenEdit(record)">编辑</a>'),
    ]),
    # ---------------- 班级管理 ----------------
    ("views/course/class.vue", [
        ('<a-button type="primary" @click="onOpenAdd">',
         '<a-button v-if="hasPerm(\'/course/class/add\')" type="primary" @click="onOpenAdd">'),
        ('<a @click="goSchedule(record)">排课</a>',
         '<a v-if="hasPerm(\'/course/schedule/generate\')" @click="goSchedule(record)">排课</a>'),
    ]),
    # ---------------- 点名消课 ----------------
    ("views/course/attend.vue", [
        ('<a-button type="primary" @click="onConfirm">确认点名并扣课</a-button>',
         '<a-button v-if="hasPerm(\'/course/attend/confirm\')" type="primary" @click="onConfirm">确认点名并扣课</a-button>'),
    ]),
    # ---------------- 续费预警 ----------------
    ("views/student/renewWarn.vue", [
        ('<a-button type="primary" @click="onScan">',
         '<a-button v-if="hasPerm(\'/student/renewWarn/scan\')" type="primary" @click="onScan">'),
    ]),
    # ---------------- 排课课表 ----------------
    ("views/course/schedule.vue", [
        ('<a-button type="primary" @click="onOpenAdd">',
         '<a-button v-if="hasPerm(\'/course/schedule/add\')" type="primary" @click="onOpenAdd">'),
    ]),
]


def apply():
    root = pathlib.Path('snowy-admin-web/src')
    total = 0
    for rel, pairs in EDITS:
        p = root / rel
        s = p.read_text(encoding='utf-8')
        for old, new in pairs:
            if old in s:
                s = s.replace(old, new, 1)
                total += 1
            else:
                print(f'  [跳过] {rel}: {old[:60]}')
        p.write_text(s, encoding='utf-8', newline='')
    print(f'共应用 {total} 处按钮权限')


if __name__ == '__main__':
    apply()
