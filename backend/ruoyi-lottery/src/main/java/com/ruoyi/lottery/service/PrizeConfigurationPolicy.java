package com.ruoyi.lottery.service;
import java.math.BigDecimal;
import java.util.Collection;
public class PrizeConfigurationPolicy {
    public void validateTotal(Collection<BigDecimal> values){BigDecimal total=BigDecimal.ZERO;for(BigDecimal value:values){if(value!=null)total=total.add(value);}if(total.compareTo(new BigDecimal("100"))>0)throw new IllegalArgumentException("启用奖品概率合计不能超过100%");}
    public void validatePrize(BigDecimal probability,Integer stock,Integer limit,Integer validDays){if(probability==null||probability.signum()<0||probability.compareTo(new BigDecimal("100"))>0)throw new IllegalArgumentException("概率必须在0到100之间");if(stock==null)throw new IllegalArgumentException("库存不能为空");if(stock<0)throw new IllegalArgumentException("库存不能小于0");if(limit==null)throw new IllegalArgumentException("个人上限不能为空");if(limit<1)throw new IllegalArgumentException("个人上限至少为1");if(validDays==null)throw new IllegalArgumentException("有效天数不能为空");if(validDays<1)throw new IllegalArgumentException("有效天数至少为1");}
}
