/**
 * 一键初始化数据库（新环境部署用）
 *
 * 用法：
 *   node scripts/db-init.mjs
 *   node scripts/db-init.mjs --host 127.0.0.1 --port 3306 --user root --password 12345678 --database chameleon
 *
 * 环境变量（优先级低于命令行参数）：
 *   DB_HOST / DB_PORT / DB_USER / DB_PASSWORD / DB_NAME
 *   MYSQL_BIN（默认 mysql，Windows 可指向绝对路径）
 *
 * 做的事：
 *   1. 建库（utf8mb4）
 *   2. 按依赖顺序执行 _sql 下的脚本
 *   3. 迁移类脚本的「已存在」错误视为幂等跳过，其余错误中止
 *   4. 健康检查：关键表/角色/菜单数量
 */
import { spawnSync } from 'node:child_process'
import { existsSync, readFileSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const SQL_DIR = join(ROOT, 'snowy-web-app/src/main/resources/_sql')

/** 执行顺序：底座 → 各业务模块建表 → 菜单 → 迁移 → 角色体系 */
const ORDER = [
	'snowy_mysql.sql',
	'chameleon_course.sql',
	'chameleon_crm.sql',
	'chameleon_student.sql',
	'chameleon_finance.sql',
	'chameleon_platform.sql',
	'chameleon_event.sql',
	// 底座表加租户列必须早于「按租户写关系」的菜单脚本
	'chameleon_tenant_migration.sql',
	'chameleon_menu.sql',
	'chameleon_ux_menu.sql',
	'chameleon_c1_migration.sql',
	'chameleon_c2_migration.sql',
	'chameleon_c3_migration.sql',
	'chameleon_c4_migration.sql',
	'chameleon_p0_migration.sql',
	'chameleon_report_migration.sql',
	'chameleon_report_permission.sql',
	'chameleon_report_r2r4_migration.sql',
	'chameleon_automation.sql',
	'chameleon_role_api_button.sql',
	'chameleon_role_template.sql'
]

/** 幂等错误：迁移脚本重复执行时会遇到，直接跳过 */
const IDEMPOTENT_ERRORS = [
	'Duplicate column name',
	'Duplicate key name',
	'Duplicate entry',
	'already exists',
	"Can't DROP",
	'check that column/key exists'
]

const args = process.argv.slice(2)
const SKIP_DEMO = args.includes('--skip-demo') || process.env.SKIP_DEMO === '1'
const arg = (name) => {
	const idx = args.indexOf(`--${name}`)
	return idx >= 0 ? args[idx + 1] : undefined
}
const env = process.env
const cfg = {
	host: arg('host') || env.DB_HOST || '127.0.0.1',
	port: arg('port') || env.DB_PORT || '3306',
	user: arg('user') || env.DB_USER || 'root',
	password: arg('password') || env.DB_PASSWORD || '12345678',
	database: arg('database') || env.DB_NAME || 'chameleon',
	mysqlBin: env.MYSQL_BIN || 'mysql'
}

const baseArgs = [
	'--default-character-set=utf8mb4',
	`-h${cfg.host}`,
	`-P${cfg.port}`,
	`-u${cfg.user}`,
	`-p${cfg.password}`
]

const runSql = (sql, database) => {
	const argv = database ? [...baseArgs, database] : baseArgs
	return spawnSync(cfg.mysqlBin, argv, { input: sql, encoding: 'utf8' })
}

const errorsOf = (stderr = '') => stderr.split('\n').filter((line) => line.includes('ERROR'))

const main = () => {
	console.log(`目标库：${cfg.user}@${cfg.host}:${cfg.port}/${cfg.database}`)

	// 1. 建库
	const create = runSql(
		`CREATE DATABASE IF NOT EXISTS \`${cfg.database}\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;`
	)
	if (create.status !== 0) {
		console.error('建库失败：', create.stderr)
		process.exit(1)
	}

	// 2. 按顺序执行（演示数据可选：--skip-demo 或 SKIP_DEMO=1）
	const executed = []
	const scriptList = SKIP_DEMO ? ORDER : [...ORDER, 'chameleon_demo_org.sql']
	for (const file of scriptList) {
		const path = join(SQL_DIR, file)
		if (!existsSync(path)) {
			console.warn(`  [跳过] ${file}（文件不存在）`)
			continue
		}
		const result = runSql(readFileSync(path, 'utf8'), cfg.database)
		const errors = errorsOf(result.stderr)
		const fatal = errors.filter((line) => !IDEMPOTENT_ERRORS.some((keyword) => line.includes(keyword)))
		if (fatal.length) {
			console.error(`  [失败] ${file}`)
			fatal.slice(0, 5).forEach((line) => console.error('    ' + line))
			process.exit(1)
		}
		executed.push(file)
		console.log(`  [完成] ${file}${errors.length ? `（跳过 ${errors.length} 处已存在的变更）` : ''}`)
	}

	// 3. 健康检查
	console.log('\n=== 健康检查 ===')
	const checks = [
		['系统用户', 'SELECT COUNT(*) FROM SYS_USER'],
		['角色', 'SELECT COUNT(*) FROM SYS_ROLE'],
		['菜单/按钮资源', 'SELECT COUNT(*) FROM SYS_RESOURCE'],
		['租户', 'SELECT COUNT(*) FROM T_TENANT'],
		['课程', 'SELECT COUNT(*) FROM COURSE'],
		['学员', 'SELECT COUNT(*) FROM STUDENT'],
		['埋点表', 'SELECT COUNT(*) FROM BIZ_EVENT'],
		['权限审计表', 'SELECT COUNT(*) FROM SYS_AUTH_AUDIT'],
		['定时任务', 'SELECT COUNT(*) FROM DEV_JOB']
	]
	let missing = 0
	for (const [label, sql] of checks) {
		const result = runSql(`${sql};`, cfg.database)
		if (result.status !== 0) {
			missing++
			console.log(`  [缺失] ${label}：${(result.stderr || '').split('\n').find((l) => l.includes('ERROR')) || ''}`)
			continue
		}
		const value = (result.stdout || '').trim().split('\n').pop()
		console.log(`  ${label}: ${value}`)
	}

	console.log(`\n完成：执行 ${executed.length} 个脚本，${missing} 项检查未通过`)
	console.log('下一步：启动后端（首次启动会补齐建表与平台账号），然后用平台账号开通租户')
	process.exit(missing > 0 ? 1 : 0)
}

main()
