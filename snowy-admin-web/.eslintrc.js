module.exports = {
	root: true,
	env: {
		browser: true,
		node: true,
		es6: true,
		'vue/setup-compiler-macros': true
	},
	parser: 'vue-eslint-parser',
	extends: ['plugin:vue/vue3-recommended', 'eslint:recommended', 'plugin:prettier/recommended'],
	parserOptions: {
		ecmaVersion: 2020,
		sourceType: 'module',
		jsxPragma: 'React',
		ecmaFeatures: {
			jsx: true
		}
	},
	rules: {
		'no-unused-vars': 'off',
		'no-undef': 'off',
		'vue/script-setup-uses-vars': 'error',
		'vue/custom-event-name-casing': 'off',
		'no-use-before-define': 'off',
		'space-before-function-paren': 'off',
		'vue/attributes-order': 'off',
		'vue/one-component-per-file': 'off',
		'vue/html-closing-bracket-newline': 'off',
		'vue/max-attributes-per-line': 'off',
		'vue/multiline-html-element-content-newline': 'off',
		'vue/singleline-html-element-content-newline': 'off',
		'vue/attribute-hyphenation': 'off',
		'vue/require-default-prop': 'off',
		'vue/html-self-closing': 'off',
		'vue/v-on-event-hyphenation': 'off',
		'vue/multi-word-component-names': 'off',
		'prettier/prettier': ['error', { endOfLine: 'auto' }]
	},
	overrides: [
		{
			// v-html 已显式净化：正文经 @/utils/sanitize 处理后再渲染，故关闭该规则提示
			files: ['src/views/biz/notice/detail.vue', 'src/components/HomeCard/BizNoticeCard/detail.vue'],
			rules: { 'vue/no-v-html': 'off' }
		},
		{
			// 邮件模板预览：内容为当前管理员正在编辑的模板本身，输入者即查看者，无存储型 XSS 链路；
			// 净化会破坏预览与真实发信的一致性。若改为预览他人模板，须改为 sanitizeHtml 并移除本豁免。
			files: ['src/views/dev/config/emailTemplateConfig/preview.vue'],
			rules: { 'vue/no-v-html': 'off' }
		}
	]
}
