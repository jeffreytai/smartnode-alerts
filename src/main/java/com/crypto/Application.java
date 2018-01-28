package com.crypto;

public class Application {

    public static void main(String[] args) {
        // Send smart node status to slack channel
        SmartNode node = new SmartNode();
        node.alertCurrentStatus();
    }
}