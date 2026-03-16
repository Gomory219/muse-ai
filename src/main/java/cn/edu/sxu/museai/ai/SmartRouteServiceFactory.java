package cn.edu.sxu.museai.ai;

import cn.edu.sxu.museai.utils.SpringContextUtil;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class SmartRouteServiceFactory {

    @Bean
    @Scope("prototype")
    public SmartRouteService smartRouteService() {

        ChatModel model = SpringContextUtil.getBean("glmChatModelPrototype", ChatModel.class);

        return AiServices.builder(SmartRouteService.class)
                .chatModel(model)
                .build();
    }

}
