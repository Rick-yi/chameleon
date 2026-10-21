/**
 * 招生线索来源字典（PC 与移动端共用，避免各写一份）
 *
 * @author 会飞的猪
 * @date 2026/10/22
 */

/** 来源选项 */
export const CRM_SOURCE_OPTIONS = [
	{ value: 'GROUND', label: '地推' },
	{ value: 'REFERRAL', label: '转介绍' },
	{ value: 'ONLINE', label: '线上' },
	{ value: 'VISIT', label: '自然到访' }
]

/**
 * 来源编码转中文
 * @param {string} value 来源编码
 */
export const crmSourceText = (value) => {
	const item = CRM_SOURCE_OPTIONS.find((option) => option.value === value)
	return item ? item.label : value || '-'
}

/**
 * 来源对应的标签色
 * @param {string} value 来源编码
 */
export const crmSourceColor = (value) =>
	({ GROUND: 'blue', REFERRAL: 'green', ONLINE: 'purple', VISIT: 'default' }[value] || 'default')
