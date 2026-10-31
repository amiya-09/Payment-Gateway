package com.springboot.razorpay.payment.simulator;

import com.springboot.razorpay.common.enums.ChaosMode;
import com.springboot.razorpay.common.enums.PaymentMethod;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "payment.simulator")
@Getter
@Setter
public class SimulatorConfig {

    private Integer pollIntervalMs = 2000;
    private ChaosMode chaosMode = ChaosMode.NORMAL;
    private Map<String, MethodSimulatorConfig> methods = new HashMap<>();

    public SimulatorConfig.MethodSimulatorConfig configFor(PaymentMethod method){
        // Here we are going to return the simulator config based upon a particular payment method that we request.
        // And all that is going to be stored inside the hash map as well.
        // If the config is not found for a method, we will return a default config.
        return methods.getOrDefault(method.name(), new MethodSimulatorConfig());
    }

    @Getter
    @Setter
    public static class MethodSimulatorConfig {
        private Integer minDelaySeconds = 1;
        private Integer maxDelaySeconds = 5;
        private Integer successRate = 80;
    }

}
