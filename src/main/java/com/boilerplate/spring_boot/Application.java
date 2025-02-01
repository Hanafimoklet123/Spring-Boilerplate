package com.boilerplate.spring_boot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Collections;

@SpringBootApplication
public class Application {

	private static final Logger logger = LoggerFactory.getLogger(Application.class);

	public static void main(String[] args) {
		int port = 80;

		SpringApplication app = new SpringApplication(Application.class);
		app.setDefaultProperties(Collections.singletonMap("server.port", port));

		String startWebServer = System.getenv("START_WEB_SERVER");

		if (startWebServer != null && startWebServer.equals("false")) {
			logger.info("Not starting the server because START_WEB_SERVER is {}", startWebServer);
			app.setWebApplicationType(WebApplicationType.NONE);
		} else {
			logger.info("Starting the server at port {}", port);
		}

		app.run(args);
	}

}
