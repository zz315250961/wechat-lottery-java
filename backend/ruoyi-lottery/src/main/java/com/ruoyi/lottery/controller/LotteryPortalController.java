package com.ruoyi.lottery.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.lottery.service.LotteryPortalService;
import com.ruoyi.lottery.service.LotterySessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.enums.LimitType;

@RestController
@RequestMapping("/lottery")
public class LotteryPortalController {
    private final LotteryPortalService portal;
    private final LotterySessionService sessions;
    @Value("${lottery.test-login-enabled:false}") private boolean testLoginEnabled;
    @Value("${lottery.test-verification-code:}") private String testCode;

    public LotteryPortalController(LotteryPortalService portal,LotterySessionService sessions){this.portal=portal;this.sessions=sessions;}

    @GetMapping("/public") public Map<String,Object> publicActivity(){Map<String,Object> result=portal.publicActivity();result.put("code",200);return result;}

    @PostMapping("/auth/test-login")
    @RateLimiter(time=60,count=10,limitType=LimitType.IP)
    public AjaxResult testLogin(@RequestBody Map<String,String> body,HttpServletRequest request,HttpServletResponse response){
        if(!testLoginEnabled)return AjaxResult.error(403,"测试登录未启用");
        String phone=body.getOrDefault("phone",""); String code=body.getOrDefault("code","");
        if(!phone.matches("1\\d{10}"))return AjaxResult.error("请输入正确的11位手机号");
        if(!code.matches("\\d{4}")||!constantEquals(code,testCode))return AjaxResult.error("验证码错误");
        Map<String,Object> user=portal.findOrCreateTestUser(phone); sessions.login(((Number)user.get("id")).longValue(),request,response); return AjaxResult.success(user);
    }

    @PostMapping("/auth/logout")
    public AjaxResult logout(HttpServletRequest request,HttpServletResponse response){sessions.logout(request,response);return AjaxResult.success();}

    @GetMapping("/me") public Object me(HttpServletRequest request){Long id=current(request);Map<String,Object> result=portal.myState(id);result.put("code",200);return result;}
    @PostMapping("/check-in") @RateLimiter(time=60,count=10,limitType=LimitType.IP) public Object checkIn(HttpServletRequest request){Map<String,Object> result=portal.checkIn(current(request));result.put("code",200);return result;}
    @PostMapping("/assist") @RateLimiter(time=60,count=20,limitType=LimitType.IP) public Object assist(@RequestBody Map<String,Object> body,HttpServletRequest request){Map<String,Object> result=portal.assist(current(request),Long.parseLong(String.valueOf(body.get("inviterId"))));result.put("code",200);return result;}
    @PostMapping("/draw") @RateLimiter(time=60,count=10,limitType=LimitType.IP) public Object draw(@RequestBody Map<String,String> body,HttpServletRequest request){Map<String,Object> result=portal.draw(current(request),body.get("requestId"));result.put("code",200);return result;}
    @GetMapping("/tickets") public AjaxResult tickets(HttpServletRequest request){return AjaxResult.success(portal.tickets(current(request)));}

    private long current(HttpServletRequest request){return sessions.currentUser(request).orElseThrow(()->new UnauthorizedException("请先登录"));}
    private static boolean constantEquals(String a,String b){if(a==null||b==null||a.length()!=b.length())return false;int diff=0;for(int i=0;i<a.length();i++)diff|=a.charAt(i)^b.charAt(i);return diff==0;}

    @ResponseStatus(org.springframework.http.HttpStatus.UNAUTHORIZED)
    private static class UnauthorizedException extends RuntimeException{UnauthorizedException(String message){super(message);}}
}
