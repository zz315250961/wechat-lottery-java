<template>
  <main class="merchant-page">
    <LotteryConfirm ref="confirmBox" />
    <header class="merchant-header">
      <div class="merchant-header__bar">
        <div class="merchant-brand"><span><Store :size="22" /></span><div><strong>校园幸运抽奖</strong><small>商户核销工作台</small></div></div>
        <button v-if="authenticated" class="logout-button" type="button" :disabled="loggingOut" @click="signOut"><LogOut :size="17" /><span>{{ loggingOut ? '正在退出' : '退出登录' }}</span></button>
      </div>
      <template v-if="authenticated"><h1>{{ `${merchantName || '本店'}核销工作台` }}</h1><p>输入核销码或扫描顾客出示的二维码，核对无误后再确认核销</p></template>
    </header>
    <MerchantLogin v-if="!authenticated" class="card login-card">
      <h2>商户登录</h2>
      <el-input v-model.trim="loginForm.username" size="large" placeholder="商户账号" />
      <el-input v-model="loginForm.password" size="large" type="password" show-password placeholder="登录密码" @keyup.enter="signIn" />
      <div v-if="captchaEnabled" class="captcha-row"><el-input v-model.trim="loginForm.code" size="large" placeholder="图形验证码" @keyup.enter="signIn" /><img :src="captchaImage" alt="点击刷新验证码" @click="loadCaptcha" /></div>
      <el-button type="primary" size="large" :loading="loggingIn" @click="signIn">登录</el-button>
    </MerchantLogin>
    <template v-else>
      <RedeemWorkbench class="card redeem-card" :merchant-name="merchantName">
        <ScanLine class="redeem-icon" :size="42" />
        <h2>查询奖券</h2>
        <p>扫码识别后会自动返回本页并填入核销码</p>
        <label for="ticket-code">核销码</label>
        <el-input id="ticket-code" v-model.trim="code" size="large" placeholder="请输入或扫描24位核销码" clearable />
        <div class="buttons"><el-button size="large" @click="openScanner"><ScanLine :size="18" /> 打开相机</el-button><el-button type="primary" size="large" :loading="submitting" @click="confirmRedeem">确认核销</el-button></div>
        <article v-if="matchedTicket" class="redemption-preview">
          <img :src="matchedTicket.prizeImageUrl || fallbackImage" :alt="matchedTicket.prizeName" />
          <div><h3>{{ matchedTicket.prizeName }}</h3><p>{{ matchedTicket.nickname || '微信用户' }} · {{ matchedTicket.phone || '未绑定手机' }}</p><small>{{ matchedTicket.status==='REDEEMED'?'该奖券已核销':'请核对奖品和用户信息后确认核销' }}</small></div>
        </article>
      </RedeemWorkbench>
      <MerchantTicketRecords class="card records" :records="records">
        <div class="section-title"><h2>本店奖券记录</h2><el-input v-model.trim="keyword" clearable placeholder="搜索券码、用户、手机号或奖品" @input="loadRecords" /></div>
        <div class="record-summary" role="group" aria-label="按奖券状态筛选">
          <button v-for="item in statusOptions" :key="item.value" type="button" :class="{ active: statusFilter === item.value }" @click="setStatusFilter(item.value)">
            <span>{{ item.label }}</span><strong>{{ statistics[item.key] }}</strong>
          </button>
        </div>
        <div v-if="records.length" class="record-list">
          <article v-for="item in records" :key="item.id">
            <img :src="item.prizeImageUrl || fallbackImage" :alt="item.prizeName" />
            <div class="record-details"><strong><span>奖品</span>{{ item.prizeName }}</strong><p><span>顾客</span>{{ item.nickname || '微信用户' }}</p><p><span>手机号</span>{{ item.phone || '未绑定手机' }}</p><small><span>券码</span>{{ item.ticketNo }}</small></div>
            <el-tag :type="ticketState(item).type">{{ ticketState(item).label }}</el-tag>
          </article>
        </div>
        <el-empty v-else :description="statusFilter ? `暂无${statusOptions.find(item => item.value === statusFilter)?.label || ''}奖券` : '暂无匹配的奖券记录'" />
      </MerchantTicketRecords>
    </template>
    <teleport to="body"><div v-if="scannerVisible" class="scanner"><video ref="video" autoplay muted playsinline></video><canvas ref="canvas" hidden></canvas><div class="shade"><div class="scan-frame"></div></div><button class="close-scan" @click="closeScanner">关闭</button><p>{{ scanMessage }}</p></div></teleport>
  </main>
</template>
<script setup>
import { computed, nextTick, onBeforeUnmount, reactive, ref } from 'vue'
import { LogOut, ScanLine, Store } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import LotteryConfirm from '@/components/LotteryConfirm.vue'
import jsQR from 'jsqr'
import { getCodeImg, login, logout as logoutRequest } from '@/api/login'
import { merchantProfile, merchantTickets, redeemTicket } from '@/api/lottery'
import MerchantLogin from './merchant/MerchantLogin.vue'
import RedeemWorkbench from './merchant/RedeemWorkbench.vue'
import MerchantTicketRecords from './merchant/MerchantTicketRecords.vue'
import { getToken, removeToken, setToken } from '@/utils/auth'
const authenticated=ref(Boolean(getToken())),loggingIn=ref(false),loggingOut=ref(false),submitting=ref(false),confirmBox=ref()
const loginForm=reactive({username:'',password:'',code:'',uuid:''}),captchaEnabled=ref(true),captchaImage=ref(''),code=ref(''),keyword=ref(''),records=ref([]),statusFilter=ref('')
const statistics=reactive({total:0,pending:0,redeemed:0,expired:0})
const statusOptions=[{label:'全部',value:'',key:'total'},{label:'待核销',value:'PENDING',key:'pending'},{label:'已核销',value:'REDEEMED',key:'redeemed'},{label:'已过期',value:'EXPIRED',key:'expired'}]
const scannerVisible=ref(false),scanMessage=ref('将二维码放入取景框内'),video=ref(),canvas=ref()
const fallbackImage='data:image/svg+xml,'+encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect width="100" height="100" rx="20" fill="#fff0dc"/><path d="M25 43h50v40H25zM20 32h60v14H20z" fill="#ff742f"/><path d="M50 32v51" stroke="#fff" stroke-width="7"/></svg>')
const merchantName=ref('')
const matchedTicket=computed(()=>records.value.find(item=>item.ticketNo?.toUpperCase()===code.value.toUpperCase()))
let stream=null,frameId=0,searchTimer=0,lastScanAt=0,barcodeDetector=null,recordsRequestId=0
async function loadCaptcha(){const result=await getCodeImg();captchaEnabled.value=result.captchaEnabled!==false;if(captchaEnabled.value){captchaImage.value=`data:image/gif;base64,${result.img}`;loginForm.uuid=result.uuid;loginForm.code=''}}
function clearMerchantSession(){removeToken();authenticated.value=false;merchantName.value='';records.value=[];code.value='';keyword.value='';statusFilter.value='';Object.assign(statistics,{total:0,pending:0,redeemed:0,expired:0})}
async function signIn(){if(!loginForm.username||!loginForm.password)return ElMessage.warning('请输入账号和密码');if(captchaEnabled.value&&!loginForm.code)return ElMessage.warning('请输入图形验证码');loggingIn.value=true;try{const result=await login(loginForm.username,loginForm.password,loginForm.code,loginForm.uuid);setToken(result.token);await loadMerchantProfile();authenticated.value=true;ElMessage.success(`已登录：${merchantName.value}`);await loadRecords()}catch(e){clearMerchantSession();if(captchaEnabled.value)await loadCaptcha();throw e}finally{loggingIn.value=false}}
async function signOut(){if(loggingOut.value)return;loggingOut.value=true;try{await logoutRequest()}catch(e){}finally{clearMerchantSession();loginForm.password='';loggingOut.value=false;await loadCaptcha();ElMessage.success('已退出商户账号')}}
async function loadMerchantProfile(){const result=await merchantProfile();merchantName.value=result.data?.name||'当前商户'}
async function loadRecords(){clearTimeout(searchTimer);const requestId=++recordsRequestId;searchTimer=setTimeout(async()=>{try{const result=await merchantTickets({keyword:keyword.value,status:statusFilter.value});if(requestId!==recordsRequestId)return;const data=result.data||{};records.value=Array.isArray(data)?data:(data.records||[]);if(!Array.isArray(data))Object.assign(statistics,{total:Number(data.statistics?.total||0),pending:Number(data.statistics?.pending||0),redeemed:Number(data.statistics?.redeemed||0),expired:Number(data.statistics?.expired||0)})}catch(e){if(e?.response?.status===401)authenticated.value=false}},250)}
function setStatusFilter(value){if(statusFilter.value===value)return;statusFilter.value=value;loadRecords()}
function ticketState(item){if(item.status==='REDEEMED')return{label:'已核销',type:'success'};if(item.expiresAt&&new Date(item.expiresAt).getTime()<Date.now())return{label:'已过期',type:'info'};return{label:'待核销',type:'warning'}}
async function confirmRedeem(){if(!/^[A-Fa-f0-9]{24}$/.test(code.value))return ElMessage.warning('请输入正确的24位核销码');if(!await confirmBox.value.open({title:'确认核销',message:`确认核销券码 ${code.value.toUpperCase()}？核销后不可撤销。`}))return;submitting.value=true;try{await redeemTicket(code.value);ElMessage.success('核销成功');code.value='';await loadRecords()}finally{submitting.value=false}}
async function openScanner(){if(!navigator.mediaDevices?.getUserMedia)return ElMessage.error('当前浏览器无法调用摄像头，请在 HTTPS 页面中打开，或手动输入核销码');scannerVisible.value=true;scanMessage.value='将二维码放入取景框内';lastScanAt=0;await nextTick();try{stream=await navigator.mediaDevices.getUserMedia({video:{facingMode:{ideal:'environment'},width:{ideal:1280},height:{ideal:720}},audio:false});video.value.srcObject=stream;await video.value.play();barcodeDetector='BarcodeDetector' in window?new window.BarcodeDetector({formats:['qr_code']}):null;frameId=requestAnimationFrame(scanFrame)}catch(e){closeScanner();ElMessage.error(e?.name==='NotAllowedError'?'未获得相机权限，请在浏览器设置中允许访问摄像头':'摄像头无法启动，请关闭占用相机的应用后重试')}}
async function scanFrame(time){if(!stream||!video.value)return;try{let value='';if(barcodeDetector){const results=await barcodeDetector.detect(video.value);value=results[0]?.rawValue||''}else if(time-lastScanAt>120&&video.value.videoWidth&&video.value.videoHeight){lastScanAt=time;const c=canvas.value,scale=Math.min(1,720/video.value.videoWidth);c.width=Math.max(1,Math.round(video.value.videoWidth*scale));c.height=Math.max(1,Math.round(video.value.videoHeight*scale));const ctx=c.getContext('2d',{willReadFrequently:true});ctx.drawImage(video.value,0,0,c.width,c.height);value=jsQR(ctx.getImageData(0,0,c.width,c.height).data,c.width,c.height,{inversionAttempts:'dontInvert'})?.data||''}if(value){code.value=String(value).trim().toUpperCase();closeScanner();ElMessage.success('已识别券码，请核对后确认核销');return}}catch(e){}frameId=requestAnimationFrame(scanFrame)}
function closeScanner(){cancelAnimationFrame(frameId);stream?.getTracks().forEach(track=>track.stop());stream=null;barcodeDetector=null;scannerVisible.value=false}
async function initializeMerchant(){if(!authenticated.value)return loadCaptcha();try{await loadMerchantProfile();loadRecords()}catch(e){clearMerchantSession();await loadCaptcha()}}
onBeforeUnmount(()=>{closeScanner();clearTimeout(searchTimer)});initializeMerchant()
</script>
<style scoped>
.merchant-page{min-height:100vh;padding:30px 20px 70px;background:#f8f7f4;color:#35271e;font-family:"PingFang SC","Microsoft YaHei",sans-serif}.merchant-page>header,.card{width:min(100%,860px);box-sizing:border-box;margin-inline:auto}.merchant-header{padding-bottom:24px;border-bottom:1px solid #e7ddd4}.merchant-header__bar{display:flex;align-items:center;justify-content:space-between;gap:18px}.merchant-brand{display:flex;align-items:center;gap:11px}.merchant-brand>span{display:grid;width:44px;height:44px;border-radius:13px;background:#fff0e2;color:#f36a1c;place-items:center}.merchant-brand strong,.merchant-brand small{display:block}.merchant-brand small{margin-top:3px;color:#9a897e;font-size:12px}.logout-button{display:flex;min-height:44px;align-items:center;gap:7px;padding:0 15px;border:1px solid #e7d7ca;border-radius:12px;background:#fff;color:#715c4e;font:inherit;font-size:14px;cursor:pointer;transition:background-color .18s ease,color .18s ease,border-color .18s ease}.logout-button:hover{border-color:#f36a1c;background:#fff5eb;color:#d95712}.logout-button:focus-visible{outline:3px solid #f36a1c40;outline-offset:2px}.logout-button:disabled{cursor:wait;opacity:.62}.merchant-header h1{margin:28px 0 8px;font-size:34px;letter-spacing:-.03em}.merchant-header>p{margin:0;color:#8b7b70}.card{margin-top:24px;padding:28px;border:1px solid #eadfd5;border-radius:16px;background:#fff}.login-card{display:grid;max-width:520px;gap:14px}.login-card h2,.records h2,.redeem-card h2{margin:0;font-size:21px}.redeem-card{max-width:620px;text-align:center}.redeem-icon{color:#f36a1c}.redeem-card>p{margin:8px 0 22px;color:#8b7b70}.redeem-card label{display:block;margin-bottom:9px;text-align:left;font-weight:700}.buttons{display:flex;gap:10px;margin-top:14px}.buttons>*{flex:1}.section-title{display:flex;align-items:center;gap:18px}.section-title h2{white-space:nowrap}.record-summary{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;margin-top:18px}.record-summary button{display:flex;min-height:58px;align-items:center;justify-content:space-between;padding:10px 14px;border:1px solid #eadfd5;border-radius:12px;background:#fffaf5;color:#806d60;font:inherit;cursor:pointer;transition:.18s ease}.record-summary button strong{color:#f36a1c;font-size:20px}.record-summary button:hover,.record-summary button.active{border-color:#f36a1c;background:#fff0e2;color:#b84d13}.record-summary button.active{box-shadow:inset 0 0 0 1px #f36a1c}.record-summary button:focus-visible{outline:3px solid #f36a1c35;outline-offset:2px}.record-list{margin-top:10px}.record-list article{display:grid;grid-template-columns:64px minmax(0,1fr) auto;align-items:center;gap:14px;padding:15px 0;border-bottom:1px solid #f0e7df}.record-list img{width:64px;height:64px;border-radius:12px;background:#fff4e4;object-fit:contain}.record-details{display:grid;gap:5px}.record-details strong,.record-details p,.record-details small{display:grid;grid-template-columns:52px minmax(0,1fr);align-items:baseline;gap:8px;margin:0}.record-details strong{font-size:16px}.record-details p,.record-details small{color:#8b7b70}.record-details small{word-break:break-all;font-size:12px}.record-details span{color:#a18e80;font-size:12px;font-weight:500}.redemption-preview{display:grid;grid-template-columns:84px 1fr;align-items:center;gap:16px;margin-top:22px;padding-top:20px;border-top:1px dashed #e5d0b5;text-align:left}.redemption-preview img{width:84px;height:84px;border-radius:13px;background:#fff4e4;object-fit:contain}.redemption-preview h3,.redemption-preview p{margin:0}.redemption-preview p,.redemption-preview small{display:block;margin-top:6px;color:#8b7b70}.scanner{position:fixed;z-index:9999;inset:0;background:#080808;color:#fff;overflow:hidden}.scanner video{width:100%;height:100%;object-fit:cover}.shade{position:absolute;inset:0;display:grid;place-items:center;background:#0005}.scan-frame{width:min(72vw,320px);aspect-ratio:1;border:3px solid #ff712b;border-radius:24px;box-shadow:0 0 0 9999px #0004}.close-scan{position:absolute;top:max(20px,env(safe-area-inset-top));right:18px;min-height:44px;padding:0 17px;border:1px solid #ffffff4d;border-radius:22px;background:#0009;color:#fff}.scanner>p{position:absolute;right:16px;bottom:max(30px,env(safe-area-inset-bottom));left:16px;text-align:center}.captcha-row{display:grid;grid-template-columns:1fr 126px;gap:10px}.captcha-row img{width:126px;height:40px;border-radius:8px;object-fit:cover;cursor:pointer}@media(max-width:600px){.merchant-page{padding:18px 12px 50px}.merchant-header__bar{align-items:flex-start}.logout-button{padding:0 12px}.logout-button span{display:none}.merchant-header h1{margin-top:22px;font-size:29px}.card{padding:20px 16px}.buttons,.section-title{flex-direction:column;align-items:stretch}.record-summary{grid-template-columns:repeat(2,1fr);gap:8px}.record-summary button{min-height:52px;padding:8px 12px}.record-list article{grid-template-columns:58px minmax(0,1fr)}.record-list img{width:58px;height:58px}.record-list .el-tag{grid-column:2;justify-self:start}.captcha-row{grid-template-columns:1fr 104px}.captcha-row img{width:104px}}
</style>
