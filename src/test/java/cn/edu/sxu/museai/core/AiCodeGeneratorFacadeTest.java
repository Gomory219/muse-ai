package cn.edu.sxu.museai.core;

import cn.hutool.core.io.FileUtil;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;

import static cn.edu.sxu.museai.model.enums.CodeGenTypeEnum.MULTI_FILE;
import static cn.edu.sxu.museai.model.enums.CodeGenTypeEnum.VUE;


@SpringBootTest
class AiCodeGeneratorFacadeTest {
    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;


    @Test
    void testGenerateCodeAndSave() {
        File file = aiCodeGeneratorFacade.generateCodeAndSave("请帮我生成一个个人博客网站，单个文件代码不要超过300行", MULTI_FILE, 1L);
        assert file != null;
    }

    @Test
    void generateCodeAndSaveStreaming() {
        Flux<String> stringFlux = aiCodeGeneratorFacade.generateCodeAndSaveStreaming("请帮我生成一个个人博客网站，单个文件代码不要超过300行", MULTI_FILE, 2L, 1L);
        stringFlux.doOnNext(System.out::print).doOnComplete(System.out::println).blockLast();
    }

    @Test
    void testGenerateCodeAndSaveStreaming() {
        Flux<String> stringFlux = aiCodeGeneratorFacade.generateCodeAndSaveStreaming("请帮我生成一个博客网站，代码量不要超过200行，在你调用工具时，一次尽量调用多个工具", VUE, 60L,6L);
        StringBuilder sb = new StringBuilder();
        stringFlux.doOnNext((s) -> {
            sb.append(s);
            System.out.println(s);
        }).blockLast();
        File file = FileUtil.writeUtf8String(sb.toString(), "output.txt");
        System.out.println(file.getAbsoluteFile());
    }
}