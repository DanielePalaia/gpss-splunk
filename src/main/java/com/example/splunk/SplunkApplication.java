package com.example.splunk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import java.io.*;
import java.util.*;



@SpringBootApplication
public class SplunkApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(SplunkApplication.class, args);
	}

	@Override
	public void run(String[] args) throws Exception {
		// Loading ini file
		FileInputStream in;
		Properties kafkaProperties = new Properties();

		try {
			in = new FileInputStream("kafka.properties");
			kafkaProperties.load(in);
		}
		catch (Exception e)  {
			e.printStackTrace();
		}


		SplunkEngine mysplunk = new SplunkEngine(args, "search", kafkaProperties.getProperty("host"), kafkaProperties.getProperty("port"), kafkaProperties.getProperty("topic"));
		mysplunk.connect();
		mysplunk.searchEvents();
	}
}
