package cn.edu.sxu.museai.generator;

import cn.hutool.core.lang.Dict;
import cn.hutool.setting.yaml.YamlUtil;
import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.Map;

public class Codegen {

    private static String[] generateTables = new String[]{"user"};

    public static void main(String[] args) {
        Dict dict = YamlUtil.loadByPath("application.yaml");
        Map<String, Object> datasourceDict = dict.getByPath("spring.datasource");
        String username = datasourceDict.get("username").toString();
        String url = datasourceDict.get("url").toString();
        String password = System.getenv("MYSQL_PASSWORD");

        //配置数据源
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        GlobalConfig globalConfig = createConfig();

        //通过 datasource 和 globalConfig 创建代码生成器
        Generator generator = new Generator(dataSource, globalConfig);

        //生成代码
        generator.generate();
    }

    public static GlobalConfig createConfig() {
        //创建配置内容
        GlobalConfig globalConfig = new GlobalConfig();

        //设置根包
        globalConfig.setBasePackage("cn.edu.sxu.museai.gen");

//        globalConfig.setSchema("public");
        //设置表前缀和只生成哪些表
        globalConfig.setGenerateTable(generateTables);

        //设置生成 entity 并启用 Lombok
        globalConfig.setEntityGenerateEnable(true);
        globalConfig.setEntityWithLombok(true);
        //设置项目的JDK版本，项目的JDK为14及以上时建议设置该项，小于14则可以不设置
        globalConfig.setEntityJdkVersion(21);

        //设置生成 mapper
        globalConfig.setMapperGenerateEnable(true);
        globalConfig.setMapperXmlGenerateEnable(true);

        //设置生成service
        globalConfig.setServiceGenerateEnable(true);
        globalConfig.setServiceImplGenerateEnable(true);

        //设置生成controller
        globalConfig.setControllerGenerateEnable(true);
        globalConfig.setLogicDeleteColumn("isDelete");

        //设置Javadoc配置
        globalConfig.getJavadocConfig().setAuthor("OneFish");

        return globalConfig;
    }

}