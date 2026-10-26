<template>
	<div class="m-login">
		<div class="m-login-brand">
			<div class="m-login-logo">教</div>
			<div class="m-login-name">{{ sysName }}</div>
			<div class="m-login-desc">机构移动助手 · 课表 / 点名 / 招生 / 收款</div>
		</div>

		<div class="m-login-form">
			<a-form ref="formRef" :model="formState" :rules="rules" layout="vertical">
				<a-form-item label="机构编码" name="tenantCode">
					<a-input v-model:value="formState.tenantCode" placeholder="如 demo" size="large" autocomplete="off" />
				</a-form-item>
				<a-form-item label="账号" name="account">
					<a-input v-model:value="formState.account" placeholder="登录账号" size="large" autocomplete="username" />
				</a-form-item>
				<a-form-item label="密码" name="password">
					<a-input-password v-model:value="formState.password" placeholder="登录密码" size="large" @pressEnter="onSubmit" />
				</a-form-item>
				<a-button type="primary" size="large" block :loading="loading" @click="onSubmit">登 录</a-button>
			</a-form>
			<div class="m-login-tip">登录即表示同意《教培助手服务协议》</div>
		</div>
	</div>
</template>

<script setup name="appLogin">
	import { message } from 'ant-design-vue'
	import { useRouter } from 'vue-router'
	import loginApi from '@/api/auth/loginApi'
	import smCrypto from '@/utils/smCrypto'
	import tool from '@/utils/tool'
	import { useAppStore } from '@/store/app'
	import { globalStore } from '@/store'

	const router = useRouter()
	const appStore = useAppStore()
	const formRef = ref()
	const loading = ref(false)
	const formState = ref({ tenantCode: 'demo', account: '', password: '' })

	const sysName = computed(() => globalStore().sysBaseConfig?.SNOWY_SYS_NAME || 'Chameleon 教培助手')

	const rules = {
		tenantCode: [{ required: true, message: '请输入机构编码' }],
		account: [{ required: true, message: '请输入账号' }],
		password: [{ required: true, message: '请输入密码' }]
	}

	const onSubmit = () => {
		formRef.value
			.validate()
			.then(async () => {
				loading.value = true
				// 机构编码用于识别租户：登录前写入，登录后服务端以会话租户为准
				tool.data.set('TENANT_CODE', formState.value.tenantCode)
				const token = await loginApi.login({
					account: formState.value.account,
					password: smCrypto.doSm2Encrypt(formState.value.password)
				})
				tool.data.set('TOKEN', token)
				// 拉取用户信息（角色决定默认视角）
				const { useUserStore } = await import('@/store/user')
				await useUserStore().initUserInfo()
				appStore.refreshUser()
				appStore.setView(appStore.defaultView())
				message.success('登录成功')
				router.push('/app/home')
			})
			.catch(() => {
				loading.value = false
			})
	}
</script>

<style lang="less" scoped>
	.m-login {
		min-height: 100vh;
		display: flex;
		flex-direction: column;
		background: linear-gradient(180deg, #eaf2fd 0%, #f2f3f5 45%);
		padding: 0 20px 32px;
	}
	.m-login-brand {
		padding: 64px 0 36px;
		text-align: center;
	}
	.m-login-logo {
		width: 56px;
		height: 56px;
		margin: 0 auto 14px;
		border-radius: 16px;
		background: var(--primary-6);
		color: #fff;
		font-size: 26px;
		font-weight: 600;
		line-height: 56px;
	}
	.m-login-name {
		font-size: 21px;
		font-weight: 600;
		color: var(--text-color);
	}
	.m-login-desc {
		margin-top: 6px;
		font-size: 13px;
		color: var(--text-color-secondary);
	}
	.m-login-form {
		background: #fff;
		border: 1px solid var(--border-color-base);
		border-radius: var(--radius-card);
		padding: 20px 16px 16px;
		box-shadow: 0 2px 10px rgba(29, 33, 41, 0.06);
	}
	.m-login-tip {
		margin-top: 14px;
		font-size: 12px;
		color: var(--text-color-tertiary);
		text-align: center;
	}
</style>
