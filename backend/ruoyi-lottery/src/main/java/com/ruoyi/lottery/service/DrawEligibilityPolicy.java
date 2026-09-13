package com.ruoyi.lottery.service;

/** 计算用户当天可用抽奖次数，不依赖缓存快照。 */
public class DrawEligibilityPolicy {
    public int remainingChances(int dailyFree, int checkInReward, boolean checkedIn,
                                int assistRewardCount, int used) {
        if (dailyFree < 0 || checkInReward < 0 || assistRewardCount < 0 || used < 0) {
            throw new IllegalArgumentException("抽奖次数参数不能为负数");
        }
        long total = (long) dailyFree + (checkedIn ? checkInReward : 0L) + assistRewardCount;
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, total - used));
    }
}
