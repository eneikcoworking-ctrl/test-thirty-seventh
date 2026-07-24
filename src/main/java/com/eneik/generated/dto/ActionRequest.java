package com.eneik.generated.dto;

public class ActionRequest {
    private String actionType;
    private Double meanDelaySeconds;

    public ActionRequest() {}

    public ActionRequest(String actionType, Double meanDelaySeconds) {
        this.actionType = actionType;
        this.meanDelaySeconds = meanDelaySeconds;
    }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public Double getMeanDelaySeconds() { return meanDelaySeconds; }
    public void setMeanDelaySeconds(Double meanDelaySeconds) { this.meanDelaySeconds = meanDelaySeconds; }
}
