/**
 * 收款渠道字典
 * <p>
 * 后端只存渠道编码（WECHAT/OFFLINE/CASH…），列表页直接显示编码对使用者不友好，
 * 这里统一给出中文名，避免各页面各写一份。
 * </p>
 */
export const PAY_CHANNEL_TEXT = {
	WECHAT: '微信',
	OFFLINE: '线下转账',
	CASH: '现金',
	ALIPAY: '支付宝',
	BANK_TRANSFER: '银行转账'
}

/**
 * 收款渠道下拉选项
 */
export const PAY_CHANNEL_OPTIONS = [
	{ value: 'OFFLINE', label: '线下转账' },
	{ value: 'CASH', label: '现金' }
]

/**
 * 收款渠道编码转中文
 * @param value 渠道编码
 */
export const payChannelText = (value) => PAY_CHANNEL_TEXT[value] || value || '-'
