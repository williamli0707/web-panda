package com.github.williamli0707.webpanda;

import com.github.williamli0707.webpanda.api.RunestoneAPI;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

import java.io.IOException;
import java.util.TimerTask;

/**
 * The entry point of the Spring Boot application.
 * <p>
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 */
@SpringBootApplication(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@Theme(value = "my-app")
@NpmPackage(value = "line-awesome", version = "1.3.0")
@Push
public class WebPandaApplication implements AppShellConfigurator {

	public static void main(String[] args) {
		SpringApplication.run(WebPandaApplication.class, args);

		//Reset cookie every hour to prevent session from expiring
		new Thread(() -> {
			while(true) {
				try {
					Thread.sleep(1000*60*60);
					RunestoneAPI.resetCookie();
				} catch (InterruptedException ignored) {
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}).start();
	}
}
