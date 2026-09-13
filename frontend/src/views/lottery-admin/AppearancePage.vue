<template>
  <section class="lottery-config-page">
    <LotteryPageHeader title="外观主题" subtitle="统一用户端品牌颜色、封面和页面背景">
      <template #actions><el-button type="primary" :loading="saving" @click="save">保存外观主题</el-button></template>
    </LotteryPageHeader>
    <div class="appearance-layout">
      <div class="settings-card appearance-form" @input.capture="markDirty" @change.capture="markDirty">
        <label>页面主色<el-color-picker v-model="activity.primaryColor" @change="markDirty" /></label>
        <label>按钮颜色<el-color-picker v-model="activity.buttonColor" @change="markDirty" /></label>
        <label>背景颜色<el-color-picker v-model="activity.backgroundColor" @change="markDirty" /></label>
        <label class="image-row">封面图
          <el-upload :show-file-list="false" :auto-upload="false" accept="image/jpeg,image/png" :on-change="file => upload(file, 'bannerUrl')"><el-button>上传封面图</el-button></el-upload>
          <el-input v-model="activity.bannerUrl" placeholder="封面图地址" />
          <img v-if="activity.bannerUrl" :src="activity.bannerUrl" alt="封面图预览" />
        </label>
        <label class="image-row">背景图
          <el-upload :show-file-list="false" :auto-upload="false" accept="image/jpeg,image/png" :on-change="file => upload(file, 'backgroundUrl')"><el-button>上传背景图</el-button></el-upload>
          <el-input v-model="activity.backgroundUrl" placeholder="背景图地址（可选）" />
          <img v-if="activity.backgroundUrl" :src="activity.backgroundUrl" alt="背景图预览" />
        </label>
      </div>
      <aside class="preview-panel" aria-label="用户端实时预览">
        <div class="preview-heading"><strong>用户端实时预览</strong><small>按手机端页面缩放展示</small></div>
        <div class="phone-preview" :style="previewStyle">
          <header class="preview-header"><span /><h2>{{ activity.title || '幸运抽奖' }}</h2><button type="button">活动规则</button></header>
          <div v-if="activity.noticeEnabled && activity.noticeText" class="preview-notice"><Megaphone /><span>{{ activity.noticeText }}</span><ChevronRight /></div>
          <main class="preview-main">
            <img v-if="activity.bannerUrl" class="preview-banner" :src="activity.bannerUrl" alt="活动封面预览" />
            <section class="preview-wheel-card">
              <div class="preview-wheel" :style="previewWheelStyle">
                <div v-for="(prize, index) in previewItems" :key="prize.id || index" class="preview-wheel-label" :style="previewLabelStyle(index)">
                  <img v-if="prize.imageUrl" :src="prize.imageUrl" alt="" /><span>{{ prize.name }}</span>
                </div>
                <button class="preview-draw" type="button">抽一次</button>
              </div>
            </section>
            <div class="preview-chances"><strong>今日剩余 <em>1</em> 次</strong><small>每日次数当天有效</small></div>
            <div class="preview-task"><CalendarCheck /><div><strong>每日签到</strong><small>完成签到得抽奖次数</small></div><button type="button">签到</button></div>
          </main>
          <nav class="preview-nav"><button class="active" type="button"><Gift />抽奖</button><button type="button"><ShoppingBag />我的奖品</button></nav>
        </div>
      </aside>
    </div>
    <p v-if="feedback" class="save-feedback" role="status">{{ feedback }}</p>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CalendarCheck, ChevronRight, Gift, Megaphone, ShoppingBag } from 'lucide-vue-next'
import LotteryPageHeader from '@/components/lottery/LotteryPageHeader.vue'
import { saveActivity, uploadImage } from '@/api/lottery'

const props = defineProps({ activity: { type: Object, required: true }, prizes: { type: Array, default: () => [] } })
const emit = defineEmits(['saved', 'dirty-change'])
const saving = ref(false)
const feedback = ref('')
const markDirty = () => emit('dirty-change', true)
const previewStyle = computed(() => ({ '--preview-primary': props.activity.primaryColor || '#ff6b2c', '--preview-button': props.activity.buttonColor || props.activity.primaryColor || '#ff6b2c', '--preview-background': props.activity.backgroundColor || '#fff8ec', backgroundImage: props.activity.backgroundUrl ? `url("${String(props.activity.backgroundUrl).replace(/"/g, '')}")` : 'none' }))
const previewItems = computed(() => {
  const items = props.prizes.filter(item => item.enabled !== false).slice(0, 6)
  return items.length ? items : [{ name: '神秘好礼' }, { name: '谢谢参与' }, { name: '校园福利' }, { name: '幸运奖券' }]
})
const previewWheelStyle = computed(() => {
  const step = 360 / previewItems.value.length
  const stops = previewItems.value.map((_, index) => `${index % 2 ? '#fff8ec' : '#ffedcf'} ${index * step}deg ${(index + 1) * step}deg`).join(',')
  return { background: `conic-gradient(${stops})` }
})
const previewLabelStyle = index => ({ transform: `rotate(${index * 360 / previewItems.value.length + 180 / previewItems.value.length}deg) translateY(-68px) rotate(90deg)` })

async function upload(file, field) {
  const result = await uploadImage(file.raw)
  props.activity[field] = result.url
  markDirty()
  ElMessage.success('图片上传成功，预览已更新')
}
async function save() {
  saving.value = true
  try { await saveActivity(props.activity); feedback.value = '外观主题已保存'; ElMessage.success(feedback.value); emit('saved') } finally { saving.value = false }
}
</script>

<style scoped>
.lottery-config-page{display:grid;gap:var(--lottery-space-3)}.appearance-layout{display:grid;grid-template-columns:minmax(0,1fr) 360px;align-items:start;gap:var(--lottery-space-3)}.settings-card{display:grid;gap:var(--lottery-space-2);padding:var(--lottery-space-3);border:1px solid var(--lottery-border);border-radius:var(--lottery-radius-lg);background:var(--lottery-paper);box-shadow:var(--lottery-shadow)}label{display:grid;gap:7px;color:var(--lottery-muted);font-size:13px}.image-row{padding-top:var(--lottery-space-2);border-top:1px solid var(--lottery-border)}.image-row img{width:100%;max-height:160px;border-radius:var(--lottery-radius-sm);background:var(--lottery-orange-soft);object-fit:contain}.preview-panel{position:sticky;top:18px;padding:18px;border:1px solid var(--lottery-border);border-radius:var(--lottery-radius-lg);background:var(--lottery-paper);box-shadow:var(--lottery-shadow)}.preview-heading{display:flex;align-items:flex-end;justify-content:space-between;margin-bottom:12px;color:var(--lottery-ink)}.preview-heading small{color:var(--lottery-muted);font-size:11px}.phone-preview{position:relative;min-height:590px;overflow:hidden;border:1px solid #f0ded0;border-radius:18px;background-color:var(--preview-background);background-image:none;background-position:center;background-size:cover;color:#482817;font-family:"PingFang SC","Microsoft YaHei",sans-serif;box-shadow:0 12px 34px #63350e14}.preview-header{display:grid;grid-template-columns:54px 1fr 54px;align-items:center;min-height:58px;padding:8px 10px;background:#fffaf1}.preview-header h2{margin:0;overflow:hidden;text-align:center;text-overflow:ellipsis;white-space:nowrap;font-size:17px}.preview-header button{padding:0;border:0;background:none;color:#74655b;font-size:10px}.preview-notice{display:flex;align-items:center;gap:6px;margin:7px 10px;padding:8px 9px;border:1px solid #ffdfbb;border-radius:9px;background:#fff;color:#7b4d2d;font-size:10px}.preview-notice svg{width:14px;height:14px;flex:none}.preview-notice span{min-width:0;flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.preview-main{padding:0 10px 74px}.preview-banner{width:100%;height:86px;border-radius:10px;object-fit:cover}.preview-wheel-card{margin-top:9px;padding:12px 0;border:1px solid #f2dcc3;border-radius:18px;background:#fff}.preview-wheel{position:relative;width:198px;aspect-ratio:1;margin:auto;overflow:hidden;border:8px solid #ff9e34;border-radius:50%}.preview-wheel-label{position:absolute;top:50%;left:50%;display:grid;width:52px;margin-top:-19px;margin-left:-26px;transform-origin:26px 19px;justify-items:center;gap:2px;text-align:center;font-size:8px;font-weight:600;line-height:1.1}.preview-wheel-label img{width:25px;height:25px;border-radius:6px;object-fit:cover}.preview-wheel-label span{display:-webkit-box;overflow:hidden;-webkit-box-orient:vertical;-webkit-line-clamp:2}.preview-draw{position:absolute;top:50%;left:50%;width:66px;height:66px;border:5px solid #ffd08b;border-radius:50%;background:var(--preview-button);color:#fff;font-size:13px;font-weight:700;transform:translate(-50%,-50%)}.preview-chances{display:grid;justify-items:center;margin:9px 0}.preview-chances strong{font-size:13px}.preview-chances em{color:var(--preview-button);font-size:18px;font-style:normal}.preview-chances small{color:#998277;font-size:9px}.preview-task{display:grid;grid-template-columns:28px 1fr auto;align-items:center;gap:7px;padding:10px;border:1px solid #f0ded0;border-radius:11px;background:#fff}.preview-task>svg{width:20px;color:var(--preview-button)}.preview-task div{display:grid}.preview-task strong{font-size:12px}.preview-task small{color:#917c6e;font-size:9px}.preview-task button{min-height:29px;padding:0 10px;border:0;border-radius:15px;background:var(--preview-button);color:#fff;font-size:10px;font-weight:600}.preview-nav{position:absolute;right:0;bottom:0;left:0;display:flex;padding:7px 0 9px;border-top:1px solid #f0ebe5;background:#fff}.preview-nav button{display:flex;width:50%;min-height:38px;flex-direction:column;align-items:center;gap:2px;border:0;background:none;color:#777;font-size:9px}.preview-nav svg{width:18px;height:18px}.preview-nav .active{color:var(--preview-button)}.save-feedback{margin:0;color:var(--lottery-orange);font-size:13px}@media(max-width:980px){.appearance-layout{grid-template-columns:1fr}.preview-panel{position:static}.phone-preview{max-width:340px;margin:auto}}@media(max-width:760px){.settings-card,.preview-panel{padding:var(--lottery-space-2)}}
</style>
