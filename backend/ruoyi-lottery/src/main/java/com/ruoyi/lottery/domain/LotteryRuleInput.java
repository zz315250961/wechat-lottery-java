package com.ruoyi.lottery.domain;

/** 管理端规则保存 DTO；与 lottery_rule 的可持久化字段一一对应。 */
public class LotteryRuleInput {
    private Long id;
    private String content;
    private Boolean enabled;
    private Integer sortOrder;

    public LotteryRuleInput() { }

    public LotteryRuleInput(Long id, String content, Boolean enabled, Integer sortOrder) {
        this.id = id;
        this.content = content;
        this.enabled = enabled;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
