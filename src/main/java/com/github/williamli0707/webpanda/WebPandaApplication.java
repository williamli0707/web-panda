package com.github.williamli0707.webpanda;

import com.github.williamli0707.webpanda.api.RunestoneAPI;
import com.github.williamli0707.webpanda.db.ItemRepository;
import com.github.williamli0707.webpanda.db.MongoManager;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.io.IOException;
import java.util.prefs.Preferences;

/**
 * The entry point of the Spring Boot application.
 * <p>
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 */
@SpringBootApplication()
@Theme(value = "my-app")
@NpmPackage(value = "line-awesome", version = "1.3.0")
@Push
@EnableMongoRepositories
public class WebPandaApplication implements CommandLineRunner, AppShellConfigurator {

	@Autowired
	ItemRepository codescanRepo;

	@Value("${build.version}")
	private String tmp;
	public static String version;

	public static Preferences preferences = Preferences.userNodeForPackage(WebPandaApplication.class);

	public static void main(String[] args) throws IOException {

		SpringApplication.run(WebPandaApplication.class, args);

		//Reset cookie every hour to prevent session from expiring
		new Thread(() -> {
			while(true) {
				try {
					Thread.sleep(1000*60*60);
					RunestoneAPI.reset();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}).start();
		//TODO scans might be broken during resetting, fix this later
	}

	public void run(String... args) {
		version = tmp;
//		codescanRepo.deleteAll();
		MongoManager.repository = codescanRepo;
		RunestoneAPI.timeDiffSensitivity = preferences.getInt("timeDiffSensitivity", 50);
		RunestoneAPI.largeEditSensitivity = preferences.getInt("largeEditSensitivity", 50);
		System.out.println("Time diff sensitivity: " + RunestoneAPI.timeDiffSensitivity);
		System.out.println("Large edit sensitivity: " + RunestoneAPI.largeEditSensitivity);
//		codescanRepo.save(new CodescanRecord(new ArrayList<>(), new ArrayList<>(), new String[] {"1", "2"}, new HashMap<>()));
	}
}
