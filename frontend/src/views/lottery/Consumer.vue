<template>
  <main class="consumer" :style="themeStyle">
    <template v-if="selectedTicket">
      <header class="mobile-header">
        <button class="icon-button" type="button" aria-label="返回我的奖品" @click="selectedTicket = null"><ArrowLeft /></button>
        <h1>奖券详情</h1><span />
      </header>
      <section class="ticket-detail">
        <article class="ticket-paper">
          <div class="ticket-product">
            <img :src="ticketImage(selectedTicket)" :alt="selectedTicket.prizeName" />
            <div><h2>{{ selectedTicket.prizeName }}</h2><p>{{ selectedTicket.merchantName }}</p></div>
          </div>
          <div class="ticket-code">
            <p>核销码</p>
            <img v-if="ticketState(selectedTicket)==='pending'" class="detail-qr" :src="qrCodes[selectedTicket.ticketNo]" alt="核销二维码" />
            <CheckCircle v-else :size="72" class="muted-icon" />
            <strong>{{ selectedTicket.ticketNo }}</strong>
            <small><Info :size="16" />到店出示，由商家核销</small>
          </div>
          <div class="ticket-guidance">
            <section><MapPin /><div><h3>核销门店</h3><strong>{{ selectedTicket.merchantName }}</strong><p>{{ selectedTicket.merchantAddress || '门店地址待商家补充，请核销前联系商家确认。' }}</p></div></section>
            <section><ClipboardList /><div><h3>使用规则</h3><p>{{ selectedTicket.usageRules || '使用规则待商家补充，请核销前联系商家确认。' }}</p></div></section>
          </div>
        </article>
        <dl class="ticket-meta">
          <div><Clock /><dt>有效期</dt><dd>{{ formatTime(selectedTicket.expiresAt) }}</dd></div>
          <div><Store /><dt>适用门店</dt><dd>{{ selectedTicket.merchantName }}</dd></div>
          <div><CheckCircle /><dt>状态</dt><dd class="accent">{{ statusText(selectedTicket) }}</dd></div>
        </dl>
      </section>
    </template>

    <template v-else>
      <header class="mobile-header">
        <span /><h1>{{ page === 'draw' ? activity.title : '我的奖品' }}</h1>
        <button v-if="page==='draw'" class="rules-link" type="button" @click="noticeVisible = true">活动规则</button>
        <button v-else class="logout-link" type="button" @click="loggedIn ? handleLogout() : loginVisible=true">{{ loggedIn ? '退出' : '登录' }}</button>
      </header>

      <template v-if="page==='draw'">
        <button v-if="activity.noticeEnabled && activity.noticeText" class="notice" type="button" @click="noticeVisible=true">
          <Megaphone :size="18" /><span><i>{{ activity.noticeText }}</i></span><ChevronRight :size="17" />
        </button>
        <section class="lottery-main">
          <img v-if="activity.bannerUrl" class="activity-banner" :src="activity.bannerUrl" alt="活动封面" />
          <div class="wheel-card">
            <div class="wheel" :style="wheelStyle">
              <div v-for="(prize,index) in wheelItems" :key="prize.id||index" class="wheel-label" :style="labelStyle(index)">
                <img v-if="prize.imageUrl" :src="prize.imageUrl" alt="" /><span>{{ prize.name }}</span>
              </div>
              <button type="button" class="draw-button" :disabled="drawing" @click="handleDraw">{{ drawing ? '抽奖中' : '抽一次' }}</button>
            </div>
          </div>
          <div class="chances"><h2>今日剩余 <em>{{ user.remainingChances }}</em> 次</h2><p>每日次数当天有效</p></div>
          <div class="task-list">
            <div class="task-row"><CalendarCheck /><div><h3>每日签到</h3><p>完成签到得抽奖次数</p></div><button type="button" @click="handleCheckIn">签到</button></div>
            <div class="task-row"><Users /><div><h3>好友助力</h3><p>每次 +1 次，每日最多 {{ activity.assistDailyLimit }} 次</p><small>今日已获 {{ user.assistCount }}/{{ activity.assistDailyLimit }} 次好友助力</small></div><button type="button" @click="shareVisible=true">邀请好友</button></div>
          </div>
        </section>
      </template>

      <section v-else class="prizes-main">
        <div class="tabs">
          <button v-for="item in tabs" :key="item.key" :class="{active:ticketTab===item.key}" @click="ticketTab=item.key">{{ item.label }}</button>
        </div>
        <div v-if="filteredTickets.length" class="coupon-list">
          <button v-for="ticket in filteredTickets" :key="ticket.id" class="coupon" type="button" @click="selectedTicket=ticket">
            <img :src="ticketImage(ticket)" :alt="ticket.prizeName" />
            <div><h2>{{ ticket.prizeName }}</h2><p>{{ ticket.merchantName || '指定核销门店' }}</p><small class="coupon-info">有效期至：{{ formatTime(ticket.expiresAt) }}</small><small class="coupon-info">核销地点：{{ ticket.merchantAddress || '门店地址待补充' }}</small><span>{{ ticketState(ticket)==='pending'?'查看核销码':'查看详情' }}</span></div>
          </button>
        </div>
        <div v-else class="empty-state"><Ticket :size="42" /><p>暂无{{ tabs.find(x=>x.key===ticketTab)?.label }}奖品</p></div>
      </section>

      <nav class="bottom-nav">
        <button :class="{active:page==='draw'}" @click="page='draw'"><Gift />抽奖</button>
        <button :class="{active:page==='prizes'}" @click="openPrizes"><ShoppingBag />我的奖品</button>
      </nav>
    </template>

    <el-dialog v-model="noticeVisible" title="活动规则" width="min(90vw,420px)" align-center :teleported="false">
      <section v-if="activity.noticeEnabled && activity.noticeText" class="rules-announcement">
        <Megaphone :size="18" />
        <div><strong>活动公告</strong><p>{{ activity.noticeText }}</p></div>
      </section>
      <ol v-if="activity.rules?.length" class="rules-list"><li v-for="(rule,index) in activity.rules" :key="`${index}-${rule}`">{{ rule }}</li></ol>
      <p v-else class="dialog-copy">{{ activity.description || '暂无活动规则' }}</p>
    </el-dialog>
    <el-dialog v-model="loginVisible" title="登录参与活动" width="min(90vw,420px)" align-center :teleported="false" :close-on-click-modal="false">
      <el-form label-position="top"><el-form-item label="手机号"><el-input v-model.trim="loginForm.phone" maxlength="11" inputmode="numeric" /></el-form-item><el-form-item label="验证码"><el-input v-model.trim="loginForm.code" maxlength="4" inputmode="numeric" /></el-form-item><el-button type="primary" class="wide" :loading="loggingIn" @click="handleLogin">登录</el-button></el-form>
    </el-dialog>
    <el-dialog v-model="shareVisible" title="邀请好友助力" width="min(90vw,420px)" align-center :teleported="false" :close-on-click-modal="false"><p class="dialog-copy">微信内请点击右上角发送给朋友；也可以复制专属助力链接。</p><el-button type="primary" class="wide" @click="copyInvite">复制助力链接</el-button></el-dialog>

    <div v-if="resultVisible" class="modal-mask" role="dialog" aria-modal="true" aria-labelledby="result-title" @click.self="resultVisible=false">
      <section class="result-modal"><button class="modal-close" aria-label="关闭" @click="resultVisible=false">×</button><h2 id="result-title">{{ result.won?'恭喜中奖':'谢谢参与' }}</h2>
        <div v-if="result.won" class="win-content"><img :src="result.imageUrl||fallbackImage" :alt="result.prizeName" /><h3>{{ result.prizeName }}</h3><p>{{ result.merchantName }}</p><button @click="viewResultTicket">查看核销码</button></div>
        <div v-else class="lose-content"><Gift :size="58" /><p>{{ result.message||'这次与好运擦肩而过，下次再试试。' }}</p><button @click="resultVisible=false">返回抽奖</button></div>
      </section>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ArrowLeft, CalendarCheck, CheckCircle, ChevronRight, ClipboardList, Clock, Gift, Info, MapPin, Megaphone, ShoppingBag, Store, Ticket, Users } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import QRCode from 'qrcode'
import { assist, checkIn, draw, getMyLottery, getPublicActivity, getWechatStatus, testLogin, userLogout } from '@/api/lottery'

const activity=reactive({title:'校园幸运抽奖',description:'好礼转起来，幸运带回家',prizes:[],rules:[],noticeEnabled:false,noticeText:'',assistDailyLimit:3})
const user=reactive({remainingChances:0,assistCount:0,tickets:[]}), page=ref('draw'), ticketTab=ref('pending'), selectedTicket=ref(null)
const drawing=ref(false),noticeVisible=ref(false),shareVisible=ref(false),loginVisible=ref(false),loggingIn=ref(false),resultVisible=ref(false),rotation=ref(0),loggedIn=ref(false),wechatReady=ref(false)
const result=reactive({won:false,prizeId:null,prizeName:'',merchantName:'',message:'',imageUrl:'',ticketNo:''}),loginForm=reactive({phone:'',code:''}),qrCodes=reactive({})
const tabs=[{key:'pending',label:'待核销'},{key:'redeemed',label:'已核销'},{key:'expired',label:'已过期'}]
const fallbackImage='data:image/svg+xml,'+encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect width="100" height="100" rx="20" fill="#fff0dc"/><path d="M25 43h50v40H25zM20 32h60v14H20z" fill="#ff742f"/><path d="M50 32v51" stroke="#fff" stroke-width="7"/><path d="M49 32c-18 0-19-18-7-18 7 0 9 8 7 18zm2 0c18 0 19-18 7-18-7 0-9 8-7 18z" fill="none" stroke="#ff742f" stroke-width="6"/></svg>')
const wheelItems=computed(()=>activity.prizes.length?activity.prizes:[{name:'神秘好礼'},{name:'谢谢参与'},{name:'校园福利'},{name:'幸运奖券'}])
const themeStyle=computed(()=>({'--accent':activity.buttonColor||activity.primaryColor||'#ff6b2c','--surface':activity.backgroundColor||'#fff8ec','--page-image':activity.backgroundUrl?`url("${String(activity.backgroundUrl).replace(/"/g,'')}")`:'none'}))
const wheelStyle=computed(()=>{const count=wheelItems.value.length,step=360/count;const stops=wheelItems.value.map((_,i)=>`${i%2?'#fff8ec':'#ffedcf'} ${i*step}deg ${(i+1)*step}deg`).join(',');return {'--segments':count,'--turn':`${rotation.value}deg`,background:`conic-gradient(${stops})`}})
function drawRequestId(){const c=globalThis.crypto;if(typeof c?.randomUUID==='function')return c.randomUUID();if(typeof c?.getRandomValues==='function'){const bytes=new Uint8Array(16);c.getRandomValues(bytes);bytes[6]=(bytes[6]&15)|64;bytes[8]=(bytes[8]&63)|128;const hex=Array.from(bytes,b=>b.toString(16).padStart(2,'0'));return `${hex.slice(0,4).join('')}-${hex.slice(4,6).join('')}-${hex.slice(6,8).join('')}-${hex.slice(8,10).join('')}-${hex.slice(10).join('')}`}return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g,ch=>{const n=Math.random()*16|0;return (ch==='x'?n:(n&3)|8).toString(16)})}
const labelStyle=i=>({transform:`rotate(${i*360/wheelItems.value.length+180/wheelItems.value.length}deg) translateY(-118px) rotate(90deg)`})
const ticketState=t=>t.status==='REDEEMED'?'redeemed':(new Date(t.expiresAt)<new Date()?'expired':'pending')
const statusText=t=>({pending:'待核销',redeemed:'已核销',expired:'已过期'}[ticketState(t)])
const filteredTickets=computed(()=>(user.tickets||[]).filter(t=>ticketState(t)===ticketTab.value))
const ticketImage=t=>t.prizeImageUrl||t.imageUrl||activity.prizes.find(p=>p.id===t.prizeId)?.imageUrl||fallbackImage

onMounted(async()=>{try{Object.assign(activity,await getPublicActivity())}catch(e){ElMessage.error(e.message)}try{wechatReady.value=Boolean((await getWechatStatus()).data?.configured)}catch(_){}try{Object.assign(user,await getMyLottery());loggedIn.value=true;await resumeAssist()}catch(_){if(!wechatReady.value)loginVisible.value=true}})
async function refreshUser(){Object.assign(user,await getMyLottery())}
async function handleLogin(){if(!/^1\d{10}$/.test(loginForm.phone)||!/^[0-9]{4}$/.test(loginForm.code))return ElMessage.warning('请输入正确的手机号和4位验证码');loggingIn.value=true;try{await testLogin(loginForm);await refreshUser();loggedIn.value=true;loginVisible.value=false;ElMessage.success('登录成功');await resumeAssist()}catch(e){ElMessage.error(e.message)}finally{loggingIn.value=false}}
async function handleLogout(){try{await userLogout()}catch(_){}Object.assign(user,{remainingChances:0,assistCount:0,tickets:[]});loggedIn.value=false;selectedTicket.value=null;ElMessage.success('已退出登录')}
async function handleCheckIn(){if(!loggedIn.value)return loginVisible.value=true;try{Object.assign(user,await checkIn());ElMessage.success('签到成功，抽奖次数已到账')}catch(e){ElMessage.error(e.message)}}
async function handleDraw(){if(!loggedIn.value)return loginVisible.value=true;drawing.value=true;try{const r=await draw(drawRequestId());const index=Math.max(0,wheelItems.value.findIndex(x=>x.id===r.prizeId));rotation.value+=1440+(360-index*360/wheelItems.value.length-rotation.value%360);await new Promise(resolve=>setTimeout(resolve,1800));const prize=activity.prizes.find(x=>x.id===r.prizeId);Object.assign(result,{...r,won:Boolean(r.won),imageUrl:prize?.imageUrl||'',merchantName:prize?.merchantName||''});await refreshUser();resultVisible.value=true}catch(e){ElMessage.error(e.message)}finally{drawing.value=false}}
async function openPrizes(){page.value='prizes';if(loggedIn.value)await refreshUser();else loginVisible.value=true}
function viewResultTicket(){const ticket=(user.tickets||[]).find(t=>t.ticketNo===result.ticketNo)||(user.tickets||[]).find(t=>t.prizeId===result.prizeId&&ticketState(t)==='pending');resultVisible.value=false;page.value='prizes';if(ticket)selectedTicket.value=ticket}
const inviterId=new URLSearchParams(location.search).get('inviter')
async function resumeAssist(){if(!inviterId||sessionStorage.getItem(`assisted:${inviterId}`))return;await assist(inviterId);sessionStorage.setItem(`assisted:${inviterId}`,'1');await refreshUser();ElMessage.success('已为好友助力成功')}
function inviteUrl(){return `${location.origin}${location.pathname}?inviter=${user.id}`}
async function copyInvite(){await navigator.clipboard.writeText(inviteUrl());ElMessage.success('助力链接已复制')}
function formatTime(v){return v?String(v).replace('T',' ').slice(0,16):'未设置'}
watch(()=>user.tickets,async list=>{for(const t of list||[])qrCodes[t.ticketNo]=await QRCode.toDataURL(t.ticketNo,{margin:1,width:220})},{deep:true,immediate:true})
</script>

<style scoped>
.consumer{width:100%;max-width:480px;min-height:100vh;margin:auto;padding-bottom:86px;background-color:var(--surface);background-image:var(--page-image);background-size:cover;background-position:center;color:#482817;font-family:"PingFang SC","Microsoft YaHei",sans-serif;box-shadow:0 0 40px #4830140d}.consumer :deep(.el-dialog){border-radius:20px;box-shadow:0 20px 70px #24170c33}.consumer :deep(.el-dialog__title){color:#482817;font-size:21px;font-weight:700}.consumer :deep(.el-button--primary){--el-button-bg-color:var(--accent);--el-button-border-color:var(--accent)}.mobile-header{display:flex;min-height:78px;align-items:center;justify-content:space-between;padding:18px 20px;background:#fffaf1}.mobile-header h1{flex:1;margin:0;text-align:center;font-size:23px}.mobile-header>span{width:34px}.icon-button,.rules-link,.logout-link{min-width:44px;min-height:44px;border:0;background:none;color:#74655b}.rules-link,.logout-link{font-size:13px}.notice{display:flex;width:calc(100% - 28px);align-items:center;gap:9px;margin:10px 14px;padding:11px 13px;border:1px solid #ffdfbb;border-radius:13px;background:#fff;color:#7b4d2d;overflow:hidden}.notice span{min-width:0;flex:1;overflow:hidden;white-space:nowrap}.notice i{display:inline-block;padding-left:100%;font-style:normal;animation:ticker 12s linear infinite}@keyframes ticker{to{transform:translateX(-100%)}}.lottery-main{padding:0 14px 22px}.activity-banner{width:100%;max-height:210px;border-radius:15px;object-fit:cover}.wheel-card{margin-top:14px;padding:18px 0 16px;border:1px solid #f2dcc3;border-radius:26px;background:#fff}.wheel{position:relative;width:min(78vw,340px);aspect-ratio:1;margin:auto;border:13px solid #ff9e34;border-radius:50%;background:conic-gradient(#ffedcf 0 25%,#fff8ec 0 50%,#ffedcf 0 75%,#fff8ec 0);transform:rotate(var(--turn));transition:transform 1.8s cubic-bezier(.12,.72,.18,1);overflow:hidden}.wheel-label{position:absolute;top:50%;left:50%;display:grid;width:76px;margin-top:-28px;margin-left:-38px;transform-origin:38px 28px;justify-items:center;gap:3px;text-align:center;font-size:12px;font-weight:600;line-height:1.15}.wheel-label img{width:34px;height:34px;border-radius:8px;object-fit:cover}.wheel-label span{display:-webkit-box;overflow:hidden;-webkit-box-orient:vertical;-webkit-line-clamp:2}.draw-button{position:absolute;top:50%;left:50%;width:106px;height:106px;border:8px solid #ffd08b;border-radius:50%;background:var(--accent);color:#fff;font-size:19px;font-weight:700;transform:translate(-50%,-50%) rotate(calc(-1 * var(--turn)));transition:transform 1.8s cubic-bezier(.12,.72,.18,1)}.chances{text-align:center}.chances h2{margin:15px 0 2px;font-size:18px}.chances em{color:var(--accent);font-size:25px;font-style:normal}.chances p{margin:0;color:#998277;font-size:12px}.task-list{display:grid;gap:11px;margin-top:16px}.task-row{display:grid;grid-template-columns:38px 1fr auto;align-items:center;gap:10px;padding:15px;border:1px solid #f0ded0;border-radius:16px;background:#fff}.task-row>svg{color:var(--accent)}.task-row h3,.task-row p{margin:0}.task-row h3{font-size:16px}.task-row p,.task-row small{display:block;margin-top:4px;color:#917c6e;font-size:12px}.task-row button,.win-content button,.lose-content button{min-height:42px;padding:0 16px;border:0;border-radius:21px;background:var(--accent);color:#fff;font-weight:600}.bottom-nav{position:fixed;z-index:8;bottom:0;left:50%;display:flex;width:100%;max-width:480px;padding:10px 0 max(10px,env(safe-area-inset-bottom));background:#fff;border-top:1px solid #f0ebe5;transform:translateX(-50%)}.bottom-nav button{display:flex;width:50%;min-height:52px;flex-direction:column;align-items:center;gap:4px;border:0;background:none;color:#777;font-size:13px}.bottom-nav svg{width:23px}.bottom-nav .active{color:var(--accent)}.prizes-main{min-height:calc(100vh - 164px);background:#fff}.tabs{display:flex;border-bottom:1px solid #eee5dc}.tabs button{position:relative;width:33.33%;min-height:60px;border:0;background:none;color:#6f625a;font-size:17px}.tabs button.active{color:var(--accent)}.tabs button.active:after{position:absolute;bottom:0;left:calc(50% - 17px);width:34px;height:3px;border-radius:2px;background:var(--accent);content:""}.coupon-list{display:grid;gap:22px;padding:22px 12px}.coupon{position:relative;display:flex;width:100%;min-height:180px;align-items:center;gap:15px;padding:24px 22px 24px 13px;border:1px solid #ffcf93;border-radius:13px;background:#fff3df;text-align:left;color:#482817}.coupon:before,.coupon:after{position:absolute;top:50%;width:13px;height:20px;background:#fff;transform:translateY(-50%);content:""}.coupon:before{left:-1px;border:1px solid #ffcf93;border-left:0;border-radius:0 12px 12px 0}.coupon:after{right:-1px;border:1px solid #ffcf93;border-right:0;border-radius:12px 0 0 12px}.coupon>img{width:115px;height:130px;flex:none;object-fit:contain}.coupon h2{margin:0 0 10px;font-size:19px}.coupon p{margin:0 0 17px;font-size:14px}.coupon span{display:inline-block;padding:8px 17px;border:1px solid var(--accent);border-radius:20px;background:#fffaf1;color:var(--accent);font-size:13px}.empty-state{display:grid;justify-items:center;gap:10px;padding:90px 20px;color:#a3978e}.ticket-detail{padding:2px 18px 26px}.ticket-paper{overflow:hidden;border:1px solid #ffcd91;border-radius:15px;background:#fff}.ticket-product{display:flex;align-items:center;gap:18px;padding:22px 17px;border-bottom:2px dashed #eebf84;background:#fff2dc}.ticket-product img{width:86px;height:86px;border-radius:13px;object-fit:contain}.ticket-product h2{margin:0 0 8px;font-size:20px}.ticket-product p{margin:0;color:#8b6e5a}.ticket-code{display:grid;justify-items:center;padding:24px 16px;border-bottom:1px solid #f1e4d6}.ticket-code p{margin:0 0 13px;color:#9b806d}.detail-qr{width:190px;height:190px}.ticket-code strong{margin-top:12px;word-break:break-all;font-size:14px;letter-spacing:.06em}.ticket-code small{display:flex;align-items:center;gap:5px;margin-top:12px;color:#9b877a}.ticket-guidance{display:grid;gap:20px;padding:22px 17px}.ticket-guidance section{display:grid;grid-template-columns:24px 1fr;gap:12px}.ticket-guidance svg{color:var(--accent)}.ticket-guidance h3,.ticket-guidance p{margin:0}.ticket-guidance h3{margin-bottom:7px;font-size:15px}.ticket-guidance p{margin-top:6px;color:#806f64;font-size:13px;line-height:1.7}.ticket-meta{display:grid;gap:1px;margin:14px 0 0;background:#eadfd5}.ticket-meta>div{display:grid;grid-template-columns:26px 75px 1fr;align-items:center;padding:13px;background:#fff}.ticket-meta dt{color:#8d7b70}.ticket-meta dd{margin:0;text-align:right}.accent{color:var(--accent)}.muted-icon{color:#b7aaa0}.modal-mask{position:fixed;z-index:100;inset:0;display:grid;padding:18px;background:#25191080;place-items:center}.result-modal{position:relative;width:min(100%,420px);box-sizing:border-box;padding:24px;border-radius:20px;background:#fff;color:#482817;box-shadow:0 20px 70px #24170c33}.result-modal>h2{margin:0 0 20px;font-size:22px}.modal-close{position:absolute;top:10px;right:10px;width:44px;height:44px;border:0;background:none;color:#817269;font-size:28px}.win-content,.lose-content{display:grid;justify-items:center;text-align:center}.win-content img{width:140px;height:140px;border-radius:16px;object-fit:contain}.win-content h3{margin:16px 0 5px;font-size:21px}.win-content p{margin:0 0 20px;color:#8a7568}.win-content button,.lose-content button{width:100%}.lose-content svg{color:var(--accent)}.lose-content p{margin:18px 0 22px;color:#79675c}.dialog-copy{line-height:1.8;color:#725f53}.wide{width:100%}
@media(min-width:600px){.consumer{margin:24px auto;min-height:calc(100vh - 48px);border-radius:17px;overflow:hidden}}@media(max-width:360px){.wheel{border-width:11px}.draw-button{width:92px;height:92px}.coupon>img{width:92px}.coupon{padding-right:15px}}@media(prefers-reduced-motion:reduce){.wheel,.draw-button{transition:none}.notice i{animation:none;padding-left:0}}
.coupon-info{display:block;margin:4px 0;color:#8b7668;font-size:12px;line-height:1.35}.coupon-info + span{margin-top:7px}
.rules-announcement{display:grid;grid-template-columns:22px 1fr;gap:10px;margin-bottom:18px;padding:13px 14px;border-radius:13px;background:#fff4e5;color:#774829}.rules-announcement>svg{margin-top:2px;color:var(--accent)}.rules-announcement strong{font-size:14px}.rules-announcement p{margin:5px 0 0;font-size:14px;line-height:1.65}.rules-list{display:grid;gap:12px;margin:0;padding-left:22px;color:#725f53;line-height:1.75}.rules-list li{padding-left:4px}
</style>
