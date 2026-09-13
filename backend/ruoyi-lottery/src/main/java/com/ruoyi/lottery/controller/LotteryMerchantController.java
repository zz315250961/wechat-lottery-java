package com.ruoyi.lottery.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.lottery.service.LotteryMerchantService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.enums.LimitType;

@RestController
@RequestMapping("/lottery/merchant")
@PreAuthorize("isAuthenticated()")
public class LotteryMerchantController {
    private final LotteryMerchantService service;
    public LotteryMerchantController(LotteryMerchantService service){this.service=service;}

    @GetMapping("/profile")
    public AjaxResult profile(){
        return AjaxResult.success(service.profile(SecurityUtils.getUserId()));
    }

    @GetMapping("/tickets")
    public AjaxResult tickets(@RequestParam(defaultValue="") String keyword,
                              @RequestParam(defaultValue="") String status){
        return AjaxResult.success(service.ticketRecords(SecurityUtils.getUserId(),keyword,status));
    }

    @PostMapping("/redeem")
    @RateLimiter(time=60,count=30,limitType=LimitType.IP)
    public AjaxResult redeem(@RequestBody Map<String,String> body){
        return AjaxResult.success("核销成功",service.redeem(SecurityUtils.getUserId(),body.get("code")));
    }
}
