/**
 * 报表环比工具：根据当前区间算出上一周期
 *
 * @author 会飞的猪
 * @date 2026/10/23
 */
import dayjs from 'dayjs'

/**
 * 上一周期（与当前区间等长、紧邻其前）
 *
 * @param {String} start YYYY-MM-DD
 * @param {String} end   YYYY-MM-DD
 * @returns {[String, String] | null}
 */
export function previousRange(start, end) {
	if (!start || !end) return null
	const startDay = dayjs(start)
	const endDay = dayjs(end)
	const days = endDay.diff(startDay, 'day') + 1
	if (days <= 0) return null
	return [startDay.subtract(days, 'day').format('YYYY-MM-DD'), startDay.subtract(1, 'day').format('YYYY-MM-DD')]
}

/**
 * 把日期区间转成接口需要的起止时间
 */
export function rangeToQuery(range) {
	if (!range) return {}
	return { startTime: `${range[0]} 00:00:00`, endTime: `${range[1]} 23:59:59` }
}
