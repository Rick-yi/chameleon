import dayjs from 'dayjs'

export const PERIOD_OPTIONS = [
	{ value: 'DAY', label: '日' },
	{ value: 'MONTH', label: '月' },
	{ value: 'YEAR', label: '年' }
]

export const PERIOD_LABEL = {
	DAY: '今日',
	MONTH: '本月',
	YEAR: '本年',
	CUSTOM: '自定义'
}

/** 日/月/年快捷区间：日=今天，月=本月，年=本年 */
export const periodDateRange = (period) => {
	const now = dayjs()
	const start = period === 'YEAR' ? now.startOf('year') : period === 'MONTH' ? now.startOf('month') : now
	return [start.format('YYYY-MM-DD'), now.format('YYYY-MM-DD')]
}

/** 自定义区间按跨度自动选择趋势粒度，避免日趋势被后端保护上限截断 */
export const rangeDimension = (start, end) => {
	if (!start || !end) return 'MONTH'
	const days = dayjs(end).diff(dayjs(start), 'day') + 1
	if (days <= 31) return 'DAY'
	if (days <= 366) return 'MONTH'
	return 'YEAR'
}
