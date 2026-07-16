package top.sharpcaterpillar.campusecho;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CampusEchoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusEchoApplication.class, args);
    }

}
