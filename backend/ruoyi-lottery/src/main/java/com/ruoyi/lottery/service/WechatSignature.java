package com.ruoyi.lottery.service;
import java.nio.charset.StandardCharsets;import java.security.MessageDigest;
public final class WechatSignature {private WechatSignature(){}public static String canonicalUrl(String url){int i=url.indexOf('#');return i<0?url:url.substring(0,i);}public static String sign(String source){try{byte[] out=MessageDigest.getInstance("SHA-1").digest(source.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte v:out)b.append(String.format("%02x",v));return b.toString();}catch(Exception e){throw new IllegalStateException("无法生成微信签名",e);}}}
