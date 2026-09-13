package com.ruoyi.lottery.controller;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.lottery.domain.LotteryActivity;
import com.ruoyi.lottery.domain.LotteryPrize;
import com.ruoyi.lottery.domain.LotteryRuleInput;
import com.ruoyi.lottery.service.LotteryAdminService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/lottery/admin") @PreAuthorize("@ss.hasRole('admin')")
public class LotteryAdminController {
 private final LotteryAdminService service; public LotteryAdminController(LotteryAdminService s){service=s;}
 @GetMapping("/overview") public AjaxResult overview(){return AjaxResult.success(service.overview());}
 @PutMapping("/activity") public AjaxResult activity(@RequestBody LotteryActivity v){return AjaxResult.success("保存成功",service.saveActivity(v));}
 @PostMapping("/prizes") public AjaxResult prize(@RequestBody LotteryPrize v){return AjaxResult.success("保存成功",service.savePrize(v));}
 @PutMapping("/prizes/{id}/move") public AjaxResult movePrize(@PathVariable long id,@RequestParam String direction){service.movePrize(id,direction);return AjaxResult.success("排序已更新");}
 @DeleteMapping("/prizes/{id}") public AjaxResult deletePrize(@PathVariable long id){service.deletePrize(id);return AjaxResult.success();}
 @GetMapping("/users") public AjaxResult users(@RequestParam(defaultValue="")String keyword){return AjaxResult.success(service.users(keyword.trim()));}
 @PutMapping("/users/{id}/enabled") public AjaxResult userEnabled(@PathVariable long id,@RequestParam boolean enabled){service.setUserEnabled(id,enabled);return AjaxResult.success(enabled?"用户已启用":"用户已停用");}
 @GetMapping("/tickets") public AjaxResult tickets(@RequestParam(defaultValue="")String keyword){return AjaxResult.success(service.tickets(keyword.trim()));}
 @GetMapping("/directives") public AjaxResult directives(){return AjaxResult.success(service.directives());}
 @PostMapping("/directives") public AjaxResult directive(@RequestBody Map<String,Object>b){service.addDirective(Long.parseLong(String.valueOf(b.get("userId"))),Long.parseLong(String.valueOf(b.get("prizeId"))),Integer.parseInt(String.valueOf(b.get("count"))));return AjaxResult.success("添加成功");}
 @DeleteMapping("/directives/{id}") public AjaxResult deleteDirective(@PathVariable long id){service.deleteDirective(id);return AjaxResult.success();}
 @PutMapping("/rules") public AjaxResult rules(@RequestBody JsonNode values){
  if(!values.isArray())throw new IllegalArgumentException("规则必须为数组");
  List<LotteryRuleInput> rules=new ArrayList<>();int sort=0;
  for(JsonNode value:values){
   int fallbackSort=sort++;
   if(value.isTextual())rules.add(new LotteryRuleInput(null,value.asText(),true,fallbackSort));
   else rules.add(new LotteryRuleInput(value.hasNonNull("id")?value.get("id").asLong():null,value.path("content").asText(),!value.has("enabled")||value.path("enabled").asBoolean(),value.has("sortOrder")?value.path("sortOrder").asInt():fallbackSort));
  }
  service.replaceRules(rules);return AjaxResult.success("保存成功");
 }
 @PostMapping("/merchants") public AjaxResult merchant(@RequestBody Map<String,Object> value){return AjaxResult.success("商户资料已保存",service.saveMerchant(value));}
}
