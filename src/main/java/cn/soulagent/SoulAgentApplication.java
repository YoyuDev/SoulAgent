package cn.soulagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("cn.soulagent.mapper")
public class SoulAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(SoulAgentApplication.class, args);
    }

}
