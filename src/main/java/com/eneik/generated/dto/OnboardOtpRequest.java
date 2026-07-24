package com.eneik.generated.dto;

public class OnboardOtpRequest {
    private String phoneNumber;
    private String otpCode;
    private String proxyIp;
    private Integer proxyPort;
    private String proxyProtocol;
    private String proxyUsername;
    private String proxyPassword;

    // Getters and Setters
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }

    public String getProxyIp() { return proxyIp; }
    public void setProxyIp(String proxyIp) { this.proxyIp = proxyIp; }

    public Integer getProxyPort() { return proxyPort; }
    public void setProxyPort(Integer proxyPort) { this.proxyPort = proxyPort; }

    public String getProxyProtocol() { return proxyProtocol; }
    public void setProxyProtocol(String proxyProtocol) { this.proxyProtocol = proxyProtocol; }

    public String getProxyUsername() { return proxyUsername; }
    public void setProxyUsername(String proxyUsername) { this.proxyUsername = proxyUsername; }

    public String getProxyPassword() { return proxyPassword; }
    public void setProxyPassword(String proxyPassword) { this.proxyPassword = proxyPassword; }
}
