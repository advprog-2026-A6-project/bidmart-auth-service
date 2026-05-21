package id.ac.ui.cs.advprog.bidmartauthservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BidmartAuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BidmartAuthServiceApplication.class, args);
    }

}
