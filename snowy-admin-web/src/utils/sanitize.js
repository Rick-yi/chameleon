/**
 *  Copyright [2022] [https://www.xiaonuo.vip]
 *	Snowy采用APACHE LICENSE 2.0开源协议，您在使用过程中，需要注意以下几点：
 *	1.请不要删除和修改根目录下的LICENSE文件。
 *	2.请不要删除和修改Snowy源码头部的版权声明。
 *	3.本项目代码可免费商业使用，商业使用请保留源码和相关描述文件的项目出处，作者声明等。
 *	4.分发源码时候，请注明软件出处 https://www.xiaonuo.vip
 *	5.不可二次分发开源参与同类竞品，如有想法可联系团队xiaonuobase@qq.com商议合作。
 *	6.若您的项目无法满足以上几点，需要更多功能代码，获取Snowy商业授权许可，请在官网购买授权，地址为 https://www.xiaonuo.vip
 */
import DOMPurify from 'dompurify'

/**
 * 富文本安全净化
 *
 * 页面里凡是用 v-html 渲染的内容都必须先过这里。
 * 公告、邮件模板等正文来自编辑器，属于「用户可写」内容：
 * 直接 v-html 渲染会形成存储型 XSS —— 写入者植入的脚本会在
 * 其他用户（包括更高权限的人）打开页面时执行。
 *
 * DOMPurify 默认配置已覆盖脚本、事件属性、javascript: 协议等常见向量；
 * 这里额外禁掉 iframe/form/style 等在本项目正文里用不到的容器。
 */

/** 正文里不需要、且可能承载可执行内容的标签 */
const FORBID_TAGS = ['iframe', 'form', 'input', 'button', 'style', 'link', 'meta', 'base']

/** 编辑器会产生的安全属性白名单补充 */
const ADD_ATTR = ['target', 'rel']

/**
 * 净化 HTML，保留常规排版标签
 *
 * @param {string} html 原始 HTML
 * @return {string} 净化后的 HTML，输入为空时返回空字符串
 */
export function sanitizeHtml(html) {
	if (!html) return ''
	return DOMPurify.sanitize(String(html), { FORBID_TAGS, ADD_ATTR })
}

// 外链统一补 noopener/noreferrer，避免被打开的页面通过 window.opener 反向操作
DOMPurify.addHook('afterSanitizeAttributes', (node) => {
	if (node.tagName === 'A' && node.getAttribute('href')) {
		node.setAttribute('rel', 'noopener noreferrer')
	}
})

export default sanitizeHtml
