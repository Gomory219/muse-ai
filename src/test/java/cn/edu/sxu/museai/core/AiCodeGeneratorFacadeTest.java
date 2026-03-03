package cn.edu.sxu.museai.core;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;

import static cn.edu.sxu.museai.model.enums.CodeGenTypeEnum.MULTI_FILE;


@SpringBootTest
class AiCodeGeneratorFacadeTest {
    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;


    @Test
    void testGenerateCodeAndSave() {
        File file = aiCodeGeneratorFacade.generateCodeAndSave("请帮我生成一个个人博客网站，单个文件代码不要超过300行", MULTI_FILE);
        assert file != null;
    }

    @Test
    void generateCodeAndSaveStreaming() {
        Flux<String> stringFlux = aiCodeGeneratorFacade.generateCodeAndSaveStreaming("请帮我生成一个个人博客网站，单个文件代码不要超过300行", MULTI_FILE);
        stringFlux.doOnNext(System.out::print).doOnComplete(System.out::println).blockLast();
    }
}