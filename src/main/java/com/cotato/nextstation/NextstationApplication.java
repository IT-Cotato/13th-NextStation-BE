package com.cotato.nextstation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

//TODO: swagger 정상 동작 확인 후 @SpringBootApplication로 수정
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class NextstationApplication {

	public static void main(String[] args) {
		SpringApplication.run(NextstationApplication.class, args);
	}
}
