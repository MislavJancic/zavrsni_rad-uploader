package com.mjancic.rad;

import com.mjancic.rad.database.DbConnection;
import com.mjancic.rad.youtube.Auth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class RadApplication {


	public static void main(String[] args) {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(RadApplication.class)
				.headless(false).run(args);
		MainDialog mainDialog = context.getBean(MainDialog.class);
		mainDialog.setVisible(true);

	}

}
