package org.aqr;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(title = "AQR API", version = "1.0"),
        security = {@SecurityRequirement(name = "bearerAuth")}
)
public class AqrApplication {

    public static void main(String[] args) {
        System.out.println("JAVA_TOOL_OPTIONS=" + System.getenv("JAVA_TOOL_OPTIONS"));
        System.out.println("JAVA_OPTS=" + System.getenv("JAVA_OPTS"));
        System.getProperties().stringPropertyNames().stream()
                .filter(k -> k.startsWith("com.sun.management.jmxremote"))
                .sorted()
                .forEach(k -> System.out.println(k + "=" + System.getProperty(k)));
        SpringApplication.run(AqrApplication.class, args);
    }

}
