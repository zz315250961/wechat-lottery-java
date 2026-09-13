<template><section class="merchant-ticket-records"><slot /></section></template>
<script setup>defineProps({ records: { type: Array, default: () => [] }, loading: Boolean })</script>
<style scoped>.merchant-ticket-records{width:100%}</style>
