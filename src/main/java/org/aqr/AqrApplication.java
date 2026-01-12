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
        SpringApplication.run(AqrApplication.class, args);
    }

}
