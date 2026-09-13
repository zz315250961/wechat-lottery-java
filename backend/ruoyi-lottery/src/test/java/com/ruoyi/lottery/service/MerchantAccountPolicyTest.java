package com.ruoyi.lottery.service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MerchantAccountPolicyTest {
 private final MerchantAccountPolicy policy=new MerchantAccountPolicy();
 @Test void acceptsTeamAccount(){assertDoesNotThrow(()->policy.validate("test1","test1pass88","校园咖啡店"));}
 @Test void rejectsShortPassword(){assertEquals("商户密码至少8位",assertThrows(IllegalArgumentException.class,()->policy.validate("test1","test1","校园咖啡店")).getMessage());}
 @Test void rejectsInvalidUsername(){assertEquals("商户账号只能使用字母、数字和下划线，长度4到30位",assertThrows(IllegalArgumentException.class,()->policy.validate("店铺","password88","店铺")).getMessage());}
}
