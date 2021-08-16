package sk.bsmk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class ResourceServerApp {

    public static final String PATH = "/secured-resource";
    public static final String DATA = "some-secured-data";

    @GetMapping(PATH)
    String securedResource() {
        return DATA;
    }

    public static void main(String[] args) {
        SpringApplication.run(ResourceServerApp.class, args);
    }

}
