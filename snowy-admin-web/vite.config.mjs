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
import { resolve } from 'path'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'
import VueJSX from '@vitejs/plugin-vue-jsx'
import AutoImport from 'unplugin-auto-import/vite'
import vueSetupExtend from 'vite-plugin-vue-setup-extend'
import { visualizer } from 'rollup-plugin-visualizer'
import Less2CssVariablePlugin from 'antd-less-to-css-variable'
import viteCompression from 'vite-plugin-compression'

//  ant-design-vue 的 less 变量，通过兼容包将 v4 变量转译成 v3 版本，并通过 less-loader 注入
import { theme } from 'ant-design-vue/lib'
import convertLegacyToken from 'ant-design-vue/lib/theme/convertLegacyToken'
const { defaultAlgorithm, defaultSeed } = theme
const mapToken = defaultAlgorithm(defaultSeed)
const v3Token = convertLegacyToken.default(mapToken)

/**
 * 企业级主题令牌（覆盖 antd v3 变量，整体换肤）
 * 设计取向：冷灰底 + 沉稳企业蓝 + 细边框 + 紧凑信息密度 + 弱装饰
 */
const enterpriseToken = {
	// 主色：沉稳的企业蓝（比默认 #1677ff 更深更稳）
	'primary-color': '#1668dc',
	'primary-color-hover': '#3c85f0',
	'primary-color-active': '#0e42b2',
	'primary-color-outline': '#b3d1f5',
	'primary-1': '#eaf2fd',
	'primary-2': '#c8dcf9',
	'primary-3': '#9dc2f4',
	'primary-4': '#6ea5ee',
	'primary-5': '#3f88e5',
	'primary-6': '#1668dc',
	'primary-7': '#0f52b6',
	'primary-8': '#0b3d8c',
	'primary-9': '#072862',
	'primary-10': '#041634',

	// 语义色：降低饱和度，避免刺眼
	'success-color': '#2ba471',
	'success-color-hover': '#3fb989',
	'success-color-active': '#1f8c5e',
	'success-color-outline': '#a7e0c8',
	'warning-color': '#d89614',
	'warning-color-hover': '#e5a92b',
	'warning-color-active': '#b87a0c',
	'warning-color-outline': '#f0d199',
	'error-color': '#e5484d',
	'error-color-hover': '#eb6266',
	'error-color-active': '#ce3539',
	'error-color-outline': '#f2b5b7',
	'info-color': '#1668dc',

	// 文字层级
	'text-color': '#1d2129',
	'text-color-secondary': '#4e5969',
	'text-color-inverse': '#ffffff',
	'heading-color': '#1d2129',

	// 边框与分隔线
	'border-color-base': '#e5e6eb',
	'border-color-split': '#f0f1f3',
	'border-color-light': '#f2f3f5',
	'divider-color': '#f0f1f3',

	// 背景
	'component-background': '#ffffff',
	'background-color-light': '#f7f8fa',
	'background-color-base': '#f2f3f5',
	'layout-body-background': '#f5f6f7',
	'item-hover-bg': '#f2f3f5',

	// 圆角：收敛到 2/4/6，弱化"玩具感"
	'border-radius-base': '4px',
	'border-radius-sm': '2px',
	'btn-border-radius-base': '4px',
	'btn-border-radius-sm': '2px',
	'tag-border-radius': '2px',

	// 字体
	'font-size-base': '14px',
	'font-size-lg': '16px',
	'font-size-sm': '12px',
	'font-family':
		"-apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif",

	// 表格：浅表头、淡分隔线、紧凑行高
	'table-bg': '#ffffff',
	'table-header-bg': '#f7f8fa',
	'table-header-color': '#4e5969',
	'table-header-split-color': '#e5e6eb',
	'table-border-color': '#eef0f3',
	'table-row-hover-bg': '#f4f7fd',
	'table-footer-bg': '#f7f8fa',
	'table-padding-vertical': '10px',
	'table-padding-horizontal': '12px',

	// 卡片：细边框、极轻阴影
	'card-background': '#ffffff',
	'card-head-background': '#ffffff',
	'card-head-color': '#1d2129',
	'card-head-font-size': '14px',
	'card-head-height': '44px',
	'card-padding-base': '16px',
	'card-shadow': '0 1px 2px rgba(29, 33, 41, 0.06)',

	// 按钮：去掉立体阴影
	'btn-shadow': 'none',
	'btn-primary-shadow': 'none',
	'btn-text-shadow': 'none',
	'btn-default-bg': '#ffffff',
	'btn-default-border': '#d9dce0',
	'btn-danger-bg': '#ffffff',
	'btn-danger-border': '#f0b4b6',
	'btn-danger-color': '#e5484d',

	// 输入类控件
	'input-height-base': '32px',
	'input-height-lg': '36px',
	'input-height-sm': '26px',
	'input-bg': '#ffffff',
	'input-border-color': '#d9dce0',
	'input-placeholder-color': '#86909c',

	// 标签：中性化
	'tag-default-bg': '#f2f3f5',
	'tag-default-color': '#4e5969',
	'tag-font-size': '12px',

	// 阴影
	'box-shadow-base': '0 4px 12px rgba(29, 33, 41, 0.08)',

	// 侧边导航：冷灰深色，弱化默认蓝块
	'layout-sider-background': '#1e2330',
	'layout-header-background': '#ffffff',
	'menu-item-height': '38px',
	'menu-item-font-size': '13px',
	'menu-item-active-bg': '#1668dc',
	'menu-item-group-title-color': 'rgba(255,255,255,0.45)',

	// 弹层
	'modal-header-bg': '#ffffff'
}

export const r = (...args) => resolve(__dirname, '.', ...args)

export default defineConfig(({ command, mode }) => {
	const envConfig = loadEnv(mode, './')
	const alias = {
		'~': `${resolve(__dirname, './')}`,
		'@/': `${resolve(__dirname, 'src')}/`
	}
	return {
		server: {
			port: envConfig.VITE_PORT,
			proxy: {
				'/api': {
					target: envConfig.VITE_API_BASEURL,
					ws: false,
					changeOrigin: true,
					rewrite: (path) => path.replace(/^\/api/, '')
				}
			}
		},
		resolve: {
			alias,
			// 优化依赖解析
			dedupe: ['vue', 'ant-design-vue']
		},
		// 解决警告You are running the esm-bundler build of vue-i18n.
		define: {
			__VUE_I18N_FULL_INSTALL__: true,
			__VUE_I18N_LEGACY_API__: true,
			__VUE_I18N_PROD_DEVTOOLS__: true,
			__VUE_PROD_HYDRATION_MISMATCH_DETAILS__: true
		},
		build: {
			// 生产环境移除console
			minify: 'terser',
			terserOptions: {
				compress: {
					drop_console: true,
					drop_debugger: true,
					pure_funcs: ['console.log']
				}
			},
			// 禁用 gzip 压缩大小报告，因为压缩大型文件可能会很慢
			reportCompressedSize: true,
			// CSS代码分割，设为false将所有CSS合并到一个文件
			cssCodeSplit: true,
			// 启用源码映射用于调试
			sourcemap: command === 'serve',
			manifest: true,
			brotliSize: false,
			assetsInlineLimit: 4096,
			rollupOptions: {
				output: {
					// 静态资源分类打包
					chunkFileNames: 'static/js/[name]-[hash].js',
					entryFileNames: 'static/js/[name]-[hash].js',
					assetFileNames: 'static/[ext]/[name]-[hash].[ext]',
					manualChunks: {
						// 第三方库分包配置保持不变
						'vue-vendor': ['vue', 'vue-router', 'pinia', 'vue-i18n'],
						'ant-design-vendor': ['ant-design-vue', '@ant-design/icons-vue', 'lodash-es', 'axios', 'dayjs'],
						'office-vendor': ['@vue-office/docx', 'vue-pdf-embed', '@vue-office/excel']
					}
				}
			},
			// 调整chunk大小警告限制
			chunkSizeWarningLimit: 2000
		},
		plugins: [
			vue({
				script: {
					refTransform: true
				}
			}),
			viteCompression(),
			vueSetupExtend(),
			VueJSX(),
			AutoImport({
				imports: ['vue'],
				dirs: ['./src/utils/permission'],
				dts: r('src/auto-imports.d.ts')
			}),
			// 组件按需引入
			Components({
				dirs: [r('src/components'), r('src/components/HomeCard')],
				dts: false,
				resolvers: [
					AntDesignVueResolver({
						importStyle: false // css in js
					})
				]
			}),
			visualizer()
		],
		css: {
			preprocessorOptions: {
				less: {
					javascriptEnabled: true,
					plugins: [new Less2CssVariablePlugin()],
					modifyVars: { ...v3Token, ...enterpriseToken }
				}
			}
		},
		optimizeDeps: {}
	}
})
