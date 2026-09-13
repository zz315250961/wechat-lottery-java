package com.ruoyi.lottery.service;public class UserAccessPolicy{public void assertEnabled(boolean enabled){if(!enabled)throw new IllegalStateException("账号已停用，请联系管理员");}}
