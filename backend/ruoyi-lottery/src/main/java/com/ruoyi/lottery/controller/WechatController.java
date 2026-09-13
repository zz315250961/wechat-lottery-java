package com.ruoyi.lottery.controller;
import com.ruoyi.common.core.domain.AjaxResult;import com.ruoyi.lottery.service.*;import org.springframework.web.bind.annotation.*;import javax.servlet.http.*;import java.net.URI;import java.util.Map;
@RestController @RequestMapping("/lottery/wechat") public class WechatController {private final WechatService wechat;private final LotterySessionService sessions;public WechatController(WechatService w,LotterySessionService s){wechat=w;sessions=s;}
 @GetMapping("/authorize")public void authorize(@RequestParam(defaultValue="/")String target,HttpServletResponse response)throws Exception{response.sendRedirect(wechat.authorizeUrl(target));}
 @GetMapping("/callback")public void callback(@RequestParam String code,@RequestParam(defaultValue="/")String state,HttpServletRequest request,HttpServletResponse response)throws Exception{Map<String,Object>u=wechat.authenticate(code);sessions.login(((Number)u.get("id")).longValue(),request,response);URI target=URI.create(state);String safe=target.isAbsolute()?"/":state;response.sendRedirect(safe.startsWith("/")?safe:"/");}
 @GetMapping("/js-config")public AjaxResult config(@RequestParam String url){return AjaxResult.success(wechat.jsConfig(url));}
 @GetMapping("/status")public AjaxResult status(){return AjaxResult.success(java.util.Collections.singletonMap("configured",wechat.configured()));}}
