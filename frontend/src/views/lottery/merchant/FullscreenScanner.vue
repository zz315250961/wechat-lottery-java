<template><div v-if="visible" class="fullscreen-scanner"><video ref="video" autoplay muted playsinline /><button type="button" class="close" @click="close">关闭</button><p>{{ message }}</p></div></template>
<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
const props = defineProps({ visible: Boolean, message: { type: String, default: '将二维码放入取景框内' } })
const emit = defineEmits(['detected', 'close'])
const video = ref(null); let stream
function close(){ stream?.getTracks().forEach(track=>track.stop()); stream=null; emit('close') }
watch(() => props.visible, async value => { if(value && navigator.mediaDevices?.getUserMedia){ try { stream=await navigator.mediaDevices.getUserMedia({video:{facingMode:{ideal:'environment'}},audio:false}); if(video.value) video.value.srcObject=stream } catch { /* parent displays permission error */ } } else if(!value) close() })
onBeforeUnmount(close)
</script>
<style scoped>.fullscreen-scanner{position:fixed;inset:0;z-index:9999;background:#080808;color:#fff}.fullscreen-scanner video{width:100%;height:100%;object-fit:cover}.close{position:absolute;top:20px;right:18px;min-height:44px;padding:0 18px;border-radius:22px;background:#0009;color:#fff;border:1px solid #fff6}.fullscreen-scanner p{position:absolute;bottom:24px;left:16px;right:16px;text-align:center}</style>
