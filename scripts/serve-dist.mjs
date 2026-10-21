/**
 * 本地预览生产构建产物（dist）的轻量静态服务
 *
 * 用法：node scripts/serve-dist.mjs [端口] [dist目录]
 * 说明：
 *   - 前端为 hash 路由，静态服务无需 history fallback；
 *   - 生产构建的接口地址来自 .env.production（默认 http://127.0.0.1:82），无需代理；
 *   - 适合「构建产物 + 真实后端」的联调与页面验收，避免 dev server 的 HMR 连接不稳定。
 */
import { createServer } from 'node:http'
import { createReadStream, existsSync, statSync } from 'node:fs'
import { extname, join, resolve } from 'node:path'

const PORT = Number(process.argv[2] || 81)
const DIST = resolve(process.argv[3] || 'snowy-admin-web/dist')

const MIME = {
	'.html': 'text/html; charset=utf-8',
	'.js': 'text/javascript; charset=utf-8',
	'.mjs': 'text/javascript; charset=utf-8',
	'.css': 'text/css; charset=utf-8',
	'.json': 'application/json; charset=utf-8',
	'.png': 'image/png',
	'.jpg': 'image/jpeg',
	'.svg': 'image/svg+xml',
	'.ico': 'image/x-icon',
	'.woff': 'font/woff',
	'.woff2': 'font/woff2',
	'.ttf': 'font/ttf',
	'.map': 'application/json; charset=utf-8'
}

createServer((req, res) => {
	const urlPath = decodeURIComponent((req.url || '/').split('?')[0])
	let filePath = join(DIST, urlPath)
	if (!existsSync(filePath) || statSync(filePath).isDirectory()) {
		filePath = join(DIST, 'index.html')
	}
	if (!existsSync(filePath)) {
		res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' })
		res.end('未找到构建产物，请先执行 npm run build')
		return
	}
	res.writeHead(200, { 'Content-Type': MIME[extname(filePath)] || 'application/octet-stream' })
	createReadStream(filePath).pipe(res)
}).listen(PORT, () => {
	console.log(`静态服务已启动：http://localhost:${PORT}  （目录：${DIST}）`)
})
