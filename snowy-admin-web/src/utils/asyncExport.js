/**
 * 通用异步导出：创建任务 → 轮询状态 → 自动下载
 *
 * 大数据量报表走后台生成，避免请求超时；交互与财务异步导出保持一致。
 *
 * @author 会飞的猪
 * @date 2026/10/23
 */
import { message } from 'ant-design-vue'
import exportApi from '@/api/biz/exportApi'
import downloadUtil from '@/utils/downloadUtil'

/**
 * @param {String} type 导出类型编码，如 COURSE_ACADEMIC
 * @param {Object} param 查询参数（会序列化成 JSON 交给后端 Builder）
 */
export function runAsyncExport(type, param) {
	return exportApi.create({ type, paramJson: JSON.stringify(param || {}) }).then((task) => {
		message.info('已开始导出，完成后会自动下载')
		return poll(task.id)
	})
}

function poll(taskId) {
	return new Promise((resolve, reject) => {
		let times = 0
		const timer = setInterval(() => {
			times += 1
			if (times > 100) {
				clearInterval(timer)
				message.error('导出超时，请稍后在导出记录中查看')
				reject(new Error('导出超时'))
				return
			}
			exportApi
				.status({ id: taskId })
				.then((task) => {
					if (task.status === 'SUCCESS') {
						clearInterval(timer)
						exportApi.download({ id: taskId }).then((res) => {
							downloadUtil.resultDownload(res)
							message.success(`导出完成，共 ${task.rowCount || 0} 行`)
							resolve(task)
						})
					} else if (task.status === 'FAILED') {
						clearInterval(timer)
						message.error(task.errorMsg || '导出失败')
						reject(new Error(task.errorMsg || '导出失败'))
					}
				})
				.catch((err) => {
					clearInterval(timer)
					reject(err)
				})
		}, 1500)
	})
}
