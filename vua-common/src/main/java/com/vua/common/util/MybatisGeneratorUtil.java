package com.vua.common.util;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 代码生成类
 * Created by liunian on 6/17/17.
 */
public class MybatisGeneratorUtil {

    // generatorConfig 模版路径
    private static String generatorConfig_vm = "/template/generatorConfig.vm";
    // Service 模版路径
    private static String service_vm = "/template/Service.vm";
    // ServiceMock 服务模拟 模版路径
    private static String serviceMock_vm = "/template/ServiceMock.vm";
    // ServiceImpl 模版路径
    private static String serviceImpl_vm = "/template/ServiceImpl.vm";

    /**
     * 根据模板生成generatorConfig.xml文件
     * @param jdbc_driver   驱动路径
     * @param jdbc_url      链接
     * @param jdbc_username 帐号
     * @param jdbc_password 密码
     * @param module        项目模块
     * @param database      数据库
     * @param table_prefix  表前缀
     * @param package_name  包名
     */
    public static void generator(
            String jdbc_driver,
            String jdbc_url,
            String jdbc_username,
            String jdbc_password,
            String module,
            String database,
            String table_prefix,
            String package_name,
            Map<String, String> lase_insert_id_tables) throws Exception {

        //绝对路径
        generatorConfig_vm = MybatisGeneratorUtil.class.getResource(generatorConfig_vm).getPath().replaceFirst("/", "/");
        service_vm = MybatisGeneratorUtil.class.getResource(service_vm).getPath().replaceFirst("/", "/");
        serviceMock_vm = MybatisGeneratorUtil.class.getResource(serviceMock_vm).getPath().replaceFirst("/", "/");
        serviceImpl_vm = MybatisGeneratorUtil.class.getResource(serviceImpl_vm).getPath().replaceFirst("/", "/");
        System.out.println("generatorConfig_vm : " + generatorConfig_vm + "service_vm : " + service_vm);

        //目标文件夹 module = vua-upms
        String targetProject = module + "/" + module + "-dao";
        //生产文件路径
        String configPath = module + "/" + module + "-dao/src/main/resources/generatorConfig.xml";
        String sql = "SELECT table_name FROM INFORMATION_SCHEMA.TABLES WHERE table_schema = '" + database + "' AND table_name LIKE '" + table_prefix + "_%';";

        System.out.println("========== 开始生成generatorConfig.xml文件 ==========");
        List<Map<String, Object>> tables = new ArrayList<>();
        try {
            VelocityContext context = new VelocityContext();
            Map<String, Object> table;

            JdbcUtil jdbcUtil = new JdbcUtil(jdbc_driver, jdbc_url, jdbc_username, jdbc_password);
            List<Map> result = jdbcUtil.selectByParams(sql, null);
            for (Map map : result) {
                System.out.println(map.get("TABLE_NAME"));
                table = new HashMap();
                table.put("table_name", map.get("TABLE_NAME"));
                table.put("model_name", lineToHump(ObjectUtils.toString(map.get("TABLE_NAME"))));
                tables.add(table);
            }
            jdbcUtil.release();

            String targetProjectSqlMap = module + "/" + module + "-rpc-service";
            context.put("tables", tables);
            context.put("generator_javaModelGenerator_targetPackage", package_name + ".dao.model");
            context.put("generator_sqlMapGenerator_targetPackage", package_name + ".dao.mapper");
            context.put("generator_javaClientGenerator_targetPackage", package_name + ".dao.mapper");
            context.put("targetProject", targetProject);
            context.put("targetProject_sqlMap", targetProjectSqlMap);
            context.put("generator_jdbc_password", jdbc_password);
            context.put("last_insert_id_tables", lase_insert_id_tables);
            //生产模板
            VelocityUtil.generate(generatorConfig_vm, configPath, context);

            deleteDir(new File(targetProject + "/" + "src" + "/" + "main" + "/" + "java" + "/" + package_name.replaceAll("\\.", "/") + "/" + "dao" + "/" + "model"));
            deleteDir(new File(targetProject + "/" + "src" + "/" + "main" + "/" + "java" + "/" + package_name.replaceAll("\\.", "/") + "/" + "dao" + "/" + "mapper"));
            //rpc-service dir
            deleteDir(new File(targetProjectSqlMap + "/" + "src" + "/" + "main" + "/" + "java" + "/" + package_name.replaceAll("\\.", "/") + "/" + "dao" + "/" + "mapper"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("=========== 结束生成generatorConfig.xml文件 ===========");

        System.out.println("========== 开始生成Service ==========");
        String ctime = new SimpleDateFormat("yyyy/M/d").format(new Date());
        String servicePath = module + "/" + module + "-rpc-api" + "/src/main/java/" + package_name.replaceAll("\\.", "/") + "/rpc/api";
        String serviceImplPath = module + "/" + module + "-rpc-service" + "/src/main/java/" + package_name.replaceAll("\\.", "/") + "/rpc/service/impl";
        for (int i = 0; i < tables.size(); i++) {
            String model = lineToHump(ObjectUtils.toString(tables.get(i).get("table_name")));
            //api path
            String service = servicePath + "/" + model + "Service.java";
            //mock path
            String serviceMock = servicePath + "/" + model + "ServiceMock.java";
            //impl path
            String serviceImpl = serviceImplPath + "/" + model + "ServiceImpl.java";

            //根据模板生成service
            File serviceFile = new File(service);
            if (!serviceFile.exists()) {
                VelocityContext context = new VelocityContext();
                context.put("package_name", package_name);
                context.put("model", model);
                context.put("ctime", ctime);
                VelocityUtil.generate(service_vm, service, context);
                System.out.println("generator : " + service);
            }
            //生成serviceMock
            File serviceMockFile = new File(serviceMock);
            if (!serviceMockFile.exists()) {
                VelocityContext context = new VelocityContext();
                context.put("package_name", package_name);
                context.put("model", model);
                context.put("ctime", ctime);
                VelocityUtil.generate(serviceMock_vm, serviceMock, context);
                System.out.println("generator : " + serviceMock);
            }
            //生成serviceImpl
            File serviceImplFile = new File(serviceImpl);
            if (!serviceImplFile.exists()) {
                VelocityContext context = new VelocityContext();
                context.put("package_name", package_name);
                context.put("model", model);
                context.put("mapper", StringUtils.uncapitalize(model));
                context.put("ctime", ctime);
                VelocityUtil.generate(serviceImpl_vm, serviceImpl, context);
                System.out.println("generator : " + serviceImpl);
            }
        }
        System.out.println("========== 结束生成Service ==========");

    }


    private static Pattern linePattern = Pattern.compile("_(\\w)");

    /**
     * 下划线转驼峰
     * @param str
     * @return
     */
    public static String lineToHump(String str) {
        if (null == str || "".equals(str)) {
            return str;
        }
        str = str.toLowerCase();
        Matcher matcher = linePattern.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
        }
        matcher.appendTail(sb);

        str = sb.toString();
        str = str.substring(0, 1).toUpperCase() + str.substring(1);

        return str;
    }

    public static void deleteDir(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                deleteDir(files[i]);
            }
        }
        dir.delete(); //是目录就递归
    }
}
