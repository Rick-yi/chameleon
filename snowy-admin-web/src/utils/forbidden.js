import { ref } from 'vue'

/**
 * 无权限状态（全局）
 * <p>
 * 后端对越权接口返回 HTTP 200 + body.code=403，前端只弹提示会让页面留白；
 * 这里用全局标记让布局层渲染「无权访问」页，而不是白屏。
 * </p>
 */
export const forbidden = ref(false)

/**
 * 当前是否处于无权限状态
 */
export const isForbidden = () => forbidden.value

/**
 * 设置无权限状态
 * @param value 是否无权限
 */
export const setForbidden = (value) => {
	forbidden.value = value
}
