package com.ruoyi.lottery.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("lottery_activity")
public class LotteryActivity {
    @TableId private Long id;
    private String title;
    private String description;
    private Boolean enabled;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer dailyFree;
    private Integer checkinReward;
    private Integer assistReward;
    private Integer assistDailyLimit;
    private String primaryColor;
    private String buttonColor;
    private String backgroundColor;
    private String backgroundUrl;
    private String bannerUrl;
    private String thanksLabel;
    private Boolean noticeEnabled;
    private String noticeText;
    private Integer poolVersion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public Integer getDailyFree() { return dailyFree; }
    public void setDailyFree(Integer dailyFree) { this.dailyFree = dailyFree; }
    public Integer getCheckinReward() { return checkinReward; }
    public void setCheckinReward(Integer checkinReward) { this.checkinReward = checkinReward; }
    public Integer getAssistReward() { return assistReward; }
    public void setAssistReward(Integer assistReward) { this.assistReward = assistReward; }
    public Integer getAssistDailyLimit() { return assistDailyLimit; }
    public void setAssistDailyLimit(Integer assistDailyLimit) { this.assistDailyLimit = assistDailyLimit; }
    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
    public String getButtonColor() { return buttonColor; }
    public void setButtonColor(String buttonColor) { this.buttonColor = buttonColor; }
    public String getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }
    public String getBackgroundUrl() { return backgroundUrl; }
    public void setBackgroundUrl(String backgroundUrl) { this.backgroundUrl = backgroundUrl; }
    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }
    public String getThanksLabel() { return thanksLabel; }
    public void setThanksLabel(String thanksLabel) { this.thanksLabel = thanksLabel; }
    public Boolean getNoticeEnabled() { return noticeEnabled; }
    public void setNoticeEnabled(Boolean noticeEnabled) { this.noticeEnabled = noticeEnabled; }
    public String getNoticeText() { return noticeText; }
    public void setNoticeText(String noticeText) { this.noticeText = noticeText; }
    public Integer getPoolVersion() { return poolVersion; }
    public void setPoolVersion(Integer poolVersion) { this.poolVersion = poolVersion; }
}
