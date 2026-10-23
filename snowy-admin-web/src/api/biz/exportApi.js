/**
 * 通用异步导出 API
 *
 * @author 会飞的猪
 * @date 2026/10/23
 */
import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/biz/export/` + url, ...arg)

export default {
	// 创建导出任务
	create(data) {
		return request('async', data)
	},
	// 查询任务状态
	status(data) {
		return request('status', data, 'get')
	},
	// 我的导出记录
	page(data) {
		return request('page', data, 'get')
	},
	// 下载导出文件
	download(data) {
		return request('download', data, 'get', {
			responseType: 'blob'
		})
	}
}
