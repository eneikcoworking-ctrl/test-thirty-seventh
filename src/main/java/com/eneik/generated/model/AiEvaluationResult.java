package com.eneik.generated.model;

public class AiEvaluationResult {

    private String responseText;
    private boolean escalation;
    private String detectedIntent;

    public AiEvaluationResult() {
    }

    public AiEvaluationResult(String responseText, boolean escalation, String detectedIntent) {
        this.responseText = responseText;
        this.escalation = escalation;
        this.detectedIntent = detectedIntent;
    }

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }

    public boolean isEscalation() {
        return escalation;
    }

    public void setEscalation(boolean escalation) {
        this.escalation = escalation;
    }

    public String getDetectedIntent() {
        return detectedIntent;
    }

    public void setDetectedIntent(String detectedIntent) {
        this.detectedIntent = detectedIntent;
    }
}
