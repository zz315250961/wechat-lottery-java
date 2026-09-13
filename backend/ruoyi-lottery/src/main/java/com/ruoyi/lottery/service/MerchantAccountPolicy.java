package com.ruoyi.lottery.service;
public class MerchantAccountPolicy {
 public void validate(String username,String password,String name){if(username==null||!username.matches("[A-Za-z0-9_]{4,30}"))throw new IllegalArgumentException("商户账号只能使用字母、数字和下划线，长度4到30位");if(password!=null&&!password.isEmpty()&&password.length()<8)throw new IllegalArgumentException("商户密码至少8位");if(name==null||name.trim().isEmpty()||name.length()>200)throw new IllegalArgumentException("请输入正确的商户名称");}
}
