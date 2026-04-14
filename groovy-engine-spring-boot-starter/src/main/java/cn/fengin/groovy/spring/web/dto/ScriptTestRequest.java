package cn.fengin.groovy.spring.web.dto;

import java.util.Map;

/**
 * 脚本测试执行请求 DTO
 *
 * @author 凌封 (https://aibook.ren)
 */
public class ScriptTestRequest {

    /** 业务编码 */
    private String bizCode;
    /** 请求参数 */
    private Map<String, Object> params;
    /** 是否开启追踪模式 */
    private Boolean track;

    public String getBizCode() { return bizCode; }
    public void setBizCode(String bizCode) { this.bizCode = bizCode; }
    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }
    public Boolean getTrack() { return track; }
    public void setTrack(Boolean track) { this.track = track; }
}
