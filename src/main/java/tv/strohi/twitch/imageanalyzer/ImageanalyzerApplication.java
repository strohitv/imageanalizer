package tv.strohi.twitch.imageanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ImageanalyzerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ImageanalyzerApplication.class, args);
	}

}
