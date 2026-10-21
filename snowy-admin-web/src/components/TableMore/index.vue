<template>
	<a-dropdown :trigger="['click']" placement="bottomRight">
		<a class="table-more" @click.prevent>
			{{ label }}
			<down-outlined class="table-more-caret" />
		</a>
		<template #overlay>
			<a-menu>
				<a-menu-item v-for="action in actions" :key="action.key" :danger="action.danger" @click="onClick(action)">
					{{ action.label }}
				</a-menu-item>
			</a-menu>
		</template>
	</a-dropdown>
</template>

<script setup name="tableMore">
	import { DownOutlined } from '@ant-design/icons-vue'
	import { Modal } from 'ant-design-vue'

	/**
	 * 表格「更多」下拉：把次要操作收进菜单，避免操作列出现一长串文字链接
	 */
	const props = defineProps({
		// 触发器文案
		label: { type: String, default: '更多' },
		/**
		 * 操作项
		 * @type {{key: string, label: string, danger?: boolean, confirmTitle?: string, confirmContent?: string}[]}
		 */
		actions: { type: Array, default: () => [] }
	})
	const emit = defineEmits(['select'])

	const onClick = (action) => {
		// 危险操作二次确认
		if (action.danger) {
			Modal.confirm({
				title: action.confirmTitle || '确认执行该操作？',
				content: action.confirmContent,
				okText: '确定',
				cancelText: '取消',
				okType: 'danger',
				onOk: () => emit('select', action)
			})
			return
		}
		emit('select', action)
	}
</script>

<style scoped>
	.table-more {
		display: inline-flex;
		align-items: center;
		gap: 2px;
		color: #1668dc;
		cursor: pointer;
		white-space: nowrap;
	}
	.table-more:hover {
		color: #3c85f0;
	}
	.table-more-caret {
		font-size: 10px;
	}
</style>
