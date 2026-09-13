<template>
  <section class="lottery-config-page">
    <LotteryConfirm ref="confirmBox" />
    <LotteryPageHeader title="奖池管理" subtitle="配置中奖概率、库存、商品图片和核销有效期">
      <template #actions><el-button type="primary" @click="addPrize">添加奖品</el-button><el-button type="primary" :loading="saving" @click="saveAll">保存奖池设置</el-button></template>
    </LotteryPageHeader>
    <div class="summary"><span><strong>{{ enabledPrizeCount }}</strong> 个启用奖品</span><span>中奖总概率 <strong>{{ totalProbability }}%</strong></span><span>剩余库存 <strong>{{ totalStock }}</strong></span></div>
    <div class="toolbar"><el-input v-model="keyword" clearable placeholder="搜索奖品名称或所属商户" /><div class="filter-buttons"><el-button v-for="filter in filters" :key="filter.value" :type="statusFilter === filter.value ? 'primary' : 'default'" @click="statusFilter = filter.value">{{ filter.label }}</el-button></div><div class="batch-actions"><span>已选 {{ selectedRows.length }} 项</span><el-button :disabled="!selectedRows.length" @click="batchToggle(true)">批量启用</el-button><el-button :disabled="!selectedRows.length" @click="batchToggle(false)">批量停用</el-button></div></div>
    <div class="prize-table" role="table" aria-label="奖品管理" @input.capture="markDirty" @change.capture="markDirty">
      <div class="prize-table-head" role="row"><span class="select-cell"><el-checkbox :model-value="allVisibleSelected" @change="toggleAll" />奖品 / 图片</span><span>所属商户</span><span>概率%</span><span>剩余库存</span><span>已抽中</span><span>已核销</span><span>每人上限</span><span>核销截止时间</span><span>上架状态</span><span>操作</span></div>
      <div v-for="row in filteredPrizes" :key="row._key || row.id" class="prize-row" :class="{ 'is-off-shelf': !row.enabled }" role="row">
        <div class="prize-product" data-label="奖品 / 图片"><span class="select-cell"><el-checkbox v-model="selectedKeys" :value="row._key || row.id" /></span><el-upload :show-file-list="false" :auto-upload="false" accept="image/jpeg,image/png" :on-change="file => upload(file, row)"><button class="image-button" type="button" aria-label="更换奖品图片"><img :src="row.imageUrl || fallbackImage" :alt="row.name || '奖品图片'" /><small>更换图片</small></button></el-upload><el-input v-model="row.name" placeholder="奖品名称" /><el-tag class="mobile-status" :type="row.enabled ? 'success' : 'danger'" effect="light" size="small">{{ row.enabled ? '已上架' : '已下架' }}</el-tag></div>
        <div data-label="所属商户"><el-select v-model="row.merchantId" placeholder="选择商户"><el-option v-for="merchant in merchants" :key="merchant.id" :label="merchant.name" :value="String(merchant.id)" /></el-select></div>
        <div data-label="概率%"><el-input-number v-model="row.probability" :min="0" :max="100" :controls="false" /></div>
        <div data-label="剩余库存"><el-input-number v-model="row.stock" :min="0" :controls="false" /></div>
        <strong class="metric" data-label="已抽中">{{ row.wonCount || 0 }}</strong><strong class="metric" data-label="已核销">{{ row.redeemedCount || 0 }}</strong>
        <div data-label="每人上限"><el-input-number v-model="row.perUserLimit" :min="1" :controls="false" /></div>
        <div class="expiry-field" data-label="核销截止时间"><el-date-picker v-model="row.redeemEndAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="未设置则中奖后 30 天" /><small v-if="row.redeemEndAt">固定截止：{{ row.redeemEndAt }}</small><small v-else>未设置：中奖后 30 天</small></div>
        <div class="status-control" data-label="上架状态"><el-tag :type="row.enabled ? 'success' : 'danger'" effect="light" size="small">{{ row.enabled ? '已上架' : '已下架' }}</el-tag><el-switch v-model="row.enabled" :aria-label="`${row.name || '奖品'}上架状态`" /></div>
        <div class="row-actions" data-label="操作"><el-button link @click="move(row, 'up')">上移</el-button><el-button link @click="move(row, 'down')">下移</el-button><el-button link type="danger" @click="remove(row)">删除</el-button></div>
      </div>
      <div v-if="!filteredPrizes.length" class="empty">没有匹配的奖品</div>
    </div>
    <div class="save-bar"><span>{{ prizes.some(prize => !prize.id) ? '新增奖品尚未保存' : '可直接在表格中修改，完成后统一保存' }}</span><el-button type="primary" :loading="saving" @click="saveAll">保存奖池设置</el-button></div>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import LotteryConfirm from '@/components/LotteryConfirm.vue'
import LotteryPageHeader from '@/components/lottery/LotteryPageHeader.vue'
import { savePrize, movePrize, deletePrize, uploadImage } from '@/api/lottery'
import { savePrizePool } from '@/composables/lotteryConfigurationPersistence'

const props = defineProps({ prizes: { type: Array, required: true }, merchants: { type: Array, required: true }, hasUnsavedChanges: { type: Boolean, default: false } })
const emit = defineEmits(['saved', 'dirty-change'])
const confirmBox = ref()
const keyword = ref('')
const statusFilter = ref('all')
const selectedKeys = ref([])
const filters = [{ value: 'all', label: '全部' }, { value: 'enabled', label: '已启用' }, { value: 'disabled', label: '已停用' }]
const saving = ref(false)
const fallbackImage = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect width="100" height="100" rx="20" fill="#fff0dc"/><path d="M25 43h50v40H25zM20 32h60v14H20z" fill="#ff742f"/><path d="M50 32v51" stroke="#fff" stroke-width="7"/></svg>')
const filteredPrizes = computed(() => { const search = keyword.value.trim().toLowerCase(); return props.prizes.filter(prize => (statusFilter.value === 'all' || (statusFilter.value === 'enabled' ? prize.enabled : !prize.enabled)) && (!search || `${prize.name || ''} ${prize.merchantName || ''}`.toLowerCase().includes(search))) })
const selectedRows = computed(() => props.prizes.filter(row => selectedKeys.value.includes(row._key || row.id)))
const allVisibleSelected = computed(() => filteredPrizes.value.length > 0 && filteredPrizes.value.every(row => selectedKeys.value.includes(row._key || row.id)))
const enabledPrizeCount = computed(() => props.prizes.filter(prize => prize.enabled).length)
const totalProbability = computed(() => props.prizes.filter(prize => prize.enabled).reduce((sum, prize) => sum + Number(prize.probability || 0), 0))
const totalStock = computed(() => props.prizes.reduce((sum, prize) => sum + Number(prize.stock || 0), 0))
const markDirty = () => emit('dirty-change', true)
const normalise = (prize) => ({ ...prize, merchantId: prize.merchantId == null && prize.merchant_id == null ? null : String(prize.merchantId ?? prize.merchant_id), merchantName: prize.merchantName ?? prize.merchant_name ?? '', imageUrl: prize.imageUrl ?? prize.image_url ?? '', perUserLimit: prize.perUserLimit ?? prize.per_user_limit ?? 1, validDays: 30, redeemEndAt: prize.redeemEndAt ?? prize.redeem_end_at ?? null, wonCount: prize.wonCount ?? prize.won_count ?? 0, redeemedCount: prize.redeemedCount ?? prize.redeemed_count ?? 0, enabled: prize.enabled === true || prize.enabled === 1, _originalEnabled: false, _originalProbability: 0, _key: prize.id || `draft-${Date.now()}-${Math.random()}` })

function addPrize() { props.prizes.unshift(normalise({ name: '新奖品', probability: 0, stock: 0, perUserLimit: 1, validDays: 30, enabled: true })); markDirty(); ElMessage.success('已添加奖品，请完善资料后保存') }
function toggleAll(value) { const visible = filteredPrizes.value.map(row => row._key || row.id); selectedKeys.value = value ? [...new Set([...selectedKeys.value, ...visible])] : selectedKeys.value.filter(key => !visible.includes(key)) }
function batchToggle(enabled) { const count = selectedRows.value.length; selectedRows.value.forEach(row => { row.enabled = enabled }); markDirty(); ElMessage.success(`已批量${enabled ? '启用' : '停用'} ${count} 项，请保存奖池设置`) }
async function upload(file, prize) { const result = await uploadImage(file.raw); prize.imageUrl = result.url; markDirty(); ElMessage.success('图片上传成功，预览已更新') }
async function saveAll() { saving.value = true; try { await savePrizePool(props.prizes, savePrize); ElMessage.success('奖池设置已保存'); emit('saved') } finally { saving.value = false } }
async function move(prize, direction) {
  if (!prize.id) {
    const index = props.prizes.indexOf(prize)
    const target = direction === 'up' ? index - 1 : index + 1
    if (target < 0 || target >= props.prizes.length) return
    props.prizes.splice(index, 1)
    props.prizes.splice(target, 0, prize)
    markDirty()
    return
  }
  if (props.hasUnsavedChanges) { ElMessage.warning('请先保存其他奖品编辑，再调整已保存奖品顺序'); return }
  await movePrize(prize.id, direction)
  ElMessage.success('奖品顺序已更新')
  emit('saved')
}
async function remove(prize) { if (!prize.id) { props.prizes.splice(props.prizes.indexOf(prize), 1); markDirty(); return } if (props.hasUnsavedChanges) { ElMessage.warning('请先保存其他奖品编辑，再删除已保存奖品'); return } const accepted = await confirmBox.value.open({ title: '删除奖品', message: `确定删除“${prize.name}”？已发出的奖券仍会保留。`, danger: true }); if (!accepted) return; await deletePrize(prize.id); ElMessage.success('已删除'); emit('saved') }
</script>

<style scoped>
.lottery-config-page{display:grid;gap:var(--lottery-space-2)}.summary{display:flex;gap:var(--lottery-space-4);padding:18px var(--lottery-space-3);border:1px solid var(--lottery-border);border-radius:var(--lottery-radius-md);background:var(--lottery-orange-soft);color:var(--lottery-muted)}.summary strong,.metric{color:var(--lottery-orange);font-variant-numeric:tabular-nums}.summary strong{font-size:21px}.toolbar :deep(.el-input){max-width:520px}.prize-table{overflow-x:auto;border:1px solid var(--lottery-border);border-radius:var(--lottery-radius-lg);background:var(--lottery-paper)}.prize-table-head,.prize-row{display:grid;grid-template-columns:minmax(250px,1.7fr) minmax(170px,1.2fr) 92px 104px 70px 70px 96px minmax(190px,1.25fr) 112px 150px;align-items:center;gap:12px;min-width:1360px;padding:14px 16px}.prize-table-head{background:#faf8f5;color:var(--lottery-muted);font-size:13px;font-weight:600}.prize-row{border-top:1px solid var(--lottery-border);transition:background-color .18s ease,box-shadow .18s ease}.prize-row.is-off-shelf{background:#fbf8f6;box-shadow:inset 1px 0 #d88d7f}.prize-row.is-off-shelf .image-button img{filter:grayscale(.55);opacity:.72}.prize-product{display:grid;grid-template-columns:72px minmax(0,1fr);align-items:center;gap:12px}.image-button{display:grid;gap:4px;width:72px;padding:0;border:0;background:transparent;color:var(--lottery-orange);cursor:pointer;text-align:center}.image-button img{width:58px;height:58px;margin:auto;border-radius:var(--lottery-radius-sm);background:var(--lottery-orange-soft);object-fit:contain;transition:filter .18s ease,opacity .18s ease}.image-button small,.expiry-field small{font-size:11px}.expiry-field{display:grid;gap:5px}.prize-row :deep(.el-input-number),.prize-row :deep(.el-select),.prize-row :deep(.el-date-editor){width:100%}.status-control{display:flex;align-items:center;gap:8px}.status-control :deep(.el-tag){min-width:54px;justify-content:center;font-weight:600}.mobile-status{display:none}.row-actions{display:flex;flex-wrap:wrap;gap:2px}.row-actions :deep(.el-button){margin-left:0}.empty{padding:44px;text-align:center;color:var(--lottery-muted)}.save-bar{display:flex;align-items:center;justify-content:space-between;gap:16px;color:var(--lottery-muted);font-size:13px}@media(max-width:760px){.summary{overflow-x:auto;gap:22px;padding:15px 16px;white-space:nowrap}.prize-table{overflow:visible;border:0;background:transparent}.prize-table-head{display:none}.prize-row{grid-template-columns:1fr 1fr;min-width:0;margin-bottom:14px;padding:16px;gap:14px;border:1px solid var(--lottery-border);border-radius:var(--lottery-radius-md)}.prize-row>div:not(.prize-product):not(.row-actions),.prize-row>.metric{display:grid;gap:6px;text-align:left}.prize-row>div:not(.prize-product):not(.row-actions)::before,.prize-row>.metric::before{content:attr(data-label);color:var(--lottery-muted);font-size:12px}.prize-product,.expiry-field,.row-actions{grid-column:1/-1}.prize-product{grid-template-columns:22px 68px minmax(0,1fr) auto}.mobile-status{display:inline-flex}.status-control :deep(.el-tag){display:none}.row-actions{justify-content:flex-end;border-top:1px solid var(--lottery-border);padding-top:10px}.save-bar{position:sticky;z-index:2;bottom:12px;padding:12px 14px;border:1px solid var(--lottery-border);border-radius:var(--lottery-radius-md);background:var(--lottery-paper);box-shadow:var(--lottery-shadow)}.save-bar span{display:none}.save-bar :deep(.el-button){width:100%;min-height:44px}}@media(max-width:520px){.prize-row{grid-template-columns:1fr}.prize-product,.expiry-field,.row-actions{grid-column:auto}.prize-product{grid-template-columns:22px 58px minmax(0,1fr)}.mobile-status{grid-column:3;justify-self:start}}
.prize-product{grid-template-columns:22px 72px minmax(0,1fr)}
.select-cell{display:inline-flex;align-items:center;gap:4px;white-space:nowrap}
.filter-buttons,.batch-actions{display:flex;align-items:center;gap:8px;flex-wrap:wrap}.batch-actions{color:var(--lottery-muted);font-size:13px}
@media(max-width:520px){.prize-product{grid-template-columns:22px 58px minmax(0,1fr)}}
</style>
