package cn.edu.sxu.museai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;


@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("cn.edu.sxu.museai.mapper")
public class MuseAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MuseAiApplication.class, args);
    }

}
