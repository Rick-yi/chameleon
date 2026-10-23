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
	// 主色：果子绿（明亮但不刺眼）
	'primary-color': '#4bb964',
	'primary-color-hover': '#6fcc85',
	'primary-color-active': '#3b9a50',
	'primary-color-outline': '#c2eacb',
	'primary-1': '#f0fbf3',
	'primary-2': '#dcf5e3',
	'primary-3': '#bceac8',
	'primary-4': '#95dca7',
	'primary-5': '#6fcc85',
	'primary-6': '#4bb964',
	'primary-7': '#3b9a50',
	'primary-8': '#2c7a3d',
	'primary-9': '#1d5a2c',
	'primary-10': '#0f3a1c',

	// 语义色：爱马仕橙作为提醒/强调色，红绿保持辨识度
	'success-color': '#2ba471',
	'success-color-hover': '#3fb989',
	'success-color-active': '#1f8c5e',
	'success-color-outline': '#a7e0c8',
	'warning-color': '#f37021',
	'warning-color-hover': '#ff8a3d',
	'warning-color-active': '#d45a12',
	'warning-color-outline': '#ffd7bf',
	'error-color': '#e5484d',
	'error-color-hover': '#eb6266',
	'error-color-active': '#ce3539',
	'error-color-outline': '#f2b5b7',
	'info-color': '#4bb964',

	// 文字层级
	'text-color': '#26302a',
	'text-color-secondary': '#5b665e',
	'text-color-inverse': '#ffffff',
	'heading-color': '#1f2a23',

	// 边框与分隔线（偏暖）
	'border-color-base': '#e3eade',
	'border-color-split': '#eef3ea',
	'border-color-light': '#f2f6ee',
	'divider-color': '#eef3ea',

	// 背景：奶油绿底，卡片纯白
	'component-background': '#ffffff',
	'background-color-light': '#f7faf3',
	'background-color-base': '#f1f6ec',
	'layout-body-background': '#f4f9f0',
	'item-hover-bg': '#f2f8ee',

	// 圆角：儿童风，大圆角
	'border-radius-base': '10px',
	'border-radius-sm': '8px',
	'btn-border-radius-base': '14px',
	'btn-border-radius-sm': '12px',
	'tag-border-radius': '999px',

	// 字体
	'font-size-base': '14px',
	'font-size-lg': '16px',
	'font-size-sm': '12px',
	'font-weight-bold': '600',
	'btn-font-weight': '500',

	// 按钮
	'btn-height-base': '36px',
	'btn-height-lg': '42px',
	'btn-height-sm': '28px',
	'btn-shadow': 'none',
	'btn-primary-shadow': '0 2px 6px rgba(75, 185, 100, 0.28)',
	'btn-text-shadow': 'none',
	'btn-default-bg': '#ffffff',
	'btn-default-border': '#dbe5d6',
	'btn-danger-bg': '#ffffff',
	'btn-danger-border': '#f0b4b6',
	'btn-danger-color': '#e5484d',

	// 输入类控件
	'input-height-base': '36px',
	'input-height-lg': '42px',
	'input-height-sm': '28px',
	'input-bg': '#ffffff',
	'input-border-color': '#dbe5d6',
	'input-placeholder-color': '#93a096',

	// 标签：胶囊形
	'tag-default-bg': '#f1f6ec',
	'tag-default-color': '#5b665e',
	'tag-font-size': '12px',

	// 阴影：柔和
	'box-shadow-base': '0 6px 20px rgba(43, 92, 52, 0.08)',

	// 侧边导航：深果子绿 + 爱马仕橙选中态
	'layout-sider-background': '#123b2b',
	'menu-dark-bg': '#123b2b',
	'menu-dark-submenu-bg': '#0e3022',
	'menu-dark-item-active-bg': '#f37021',
	'menu-dark-item-selected-bg': '#f37021',
	'layout-header-background': '#ffffff',
	'menu-item-height': '40px',
	'menu-item-font-size': '13px',
	'menu-item-active-bg': '#f37021',
	'menu-item-group-title-color': 'rgba(255,255,255,0.5)',

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
			// 不做降级转译：目标浏览器（Chrome 87+/Edge 88+ 等）已原生支持所用语法。
			// 同时规避 Windows 上 esbuild 转译产物临时文件无法删除导致构建失败的问题。
			target: 'esnext',
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
