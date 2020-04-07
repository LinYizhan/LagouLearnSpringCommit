package com.lagou.edu.servlet;

import com.alibaba.druid.util.StringUtils;
import com.lagou.edu.annotation.MyAutowired;
import com.lagou.edu.annotation.MyService;
import com.lagou.edu.factory.BeanFactory;
import com.lagou.edu.factory.ProxyFactory;
import com.lagou.edu.service.impl.TransferServiceImpl;
import com.lagou.edu.utils.JsonUtils;
import com.lagou.edu.pojo.Result;
import com.lagou.edu.service.TransferService;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 应癫
 */
@WebServlet(name="transferServlet",urlPatterns = "/transferServlet")
public class TransferServlet extends HttpServlet {

    private String projectPath=this.getClass().getResource("/").getPath();//项目路径
    // 1. 实例化service层对象
    //private TransferService transferService = new TransferServiceImpl();
    //private TransferService transferService = (TransferService) BeanFactory.getBean("transferService");

    // 从工厂获取委托对象（委托对象是增强了事务控制的功能）

    // 首先从BeanFactory获取到proxyFactory代理工厂的实例化对象
//    private ProxyFactory proxyFactory = (ProxyFactory) BeanFactory.getBean("proxyFactory");
//    private TransferService transferService = (TransferService) proxyFactory.getJdkProxy(BeanFactory.getBean("transferService")) ;

    @MyAutowired
    TransferService transferService;

    List<String> classNames = new ArrayList();

    Map<String, Object> beans = new HashMap<>();
    Map<String, Object> handlerMap = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // 设置请求体的字符编码
        req.setCharacterEncoding("UTF-8");

        String fromCardNo = req.getParameter("fromCardNo");
        String toCardNo = req.getParameter("toCardNo");
        String moneyStr = req.getParameter("money");
        int money = Integer.parseInt(moneyStr);

        Result result = new Result();

        try {

            // 2. 调用service层方法
            transferService.transfer(fromCardNo,toCardNo,money);
            result.setStatus("200");
        } catch (Exception e) {
            e.printStackTrace();
            result.setStatus("201");
            result.setMessage(e.toString());
        }

        // 响应
        resp.setContentType("application/json;charset=utf-8");
        resp.getWriter().print(JsonUtils.object2Json(result));
    }

    @Override
    public void init() throws ServletException {
        //scanPackage("com.lagou.edu"); //扫描
        getFileName(projectPath);
        doInstance(); //实例
        try {
            doIoc(); //注入
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void doIoc() throws IllegalAccessException {
        if (beans.entrySet().size() <= 0) {
            System.out.println("no beans ===========");
        }
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Object instance = entry.getValue();
            System.out.println("=-=-=-=-=-=" + instance);
            Class<?> clazz = instance.getClass();
            if (clazz.isAnnotationPresent(WebServlet.class)) {
                //获得所有声明的参数 得到参数数组
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    System.out.println(field);
                    if (field.isAnnotationPresent(MyAutowired.class)) {
                        MyAutowired autowired = field.getAnnotation(MyAutowired.class);
                        String key = autowired.value();
                        if(StringUtils.isEmpty(key)){
                            key = field.getName().toLowerCase() + "impl";
                            System.out.println(key);
                        }
                        Object o  = beans.get(key);
                        //打开私有属性的权限修改
                        field.setAccessible(true);
                        //给变量重新设值
                        field.set(this, o);
                    } else {
                        continue;
                    }
                }
            } else if(clazz.isAnnotationPresent(MyService.class)){
                //获得所有声明的参数 得到参数数组
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    System.out.println(field);
                    if (field.isAnnotationPresent(MyAutowired.class)) {
                        MyAutowired autowired = field.getAnnotation(MyAutowired.class);
                        String key = autowired.value();
                        if(StringUtils.isEmpty(key)){
                            key = field.getName().toLowerCase() + "impl";
                            System.out.println(key);
                        }
                        Object o  = beans.get(key);
                        //Object a = ProxyFactory.getCglibProxy(o);
                        //打开私有属性的权限修改
                        field.setAccessible(true);
                        //给变量重新设值
                        field.set(instance, o);
                    } else {
                        continue;
                    }
            }} else {
                continue;
            }
        }

        System.out.println(beans);
    }

    private void doInstance() {
        if (classNames.isEmpty()) {
            System.out.println("class扫描失败.......");
        }
        for (String className : classNames) {
            //去除多余的后缀名
            String cName = className.replace(".class", "");
            cName = cName.replace("/Users/anyuzhi/workspaces/learning/lagou/homework/LagouLearnSpringCommit/lagou-transfer/target/classes/", "");
            cName = cName.replace("/", ".");
            try {
                Class<?> clazz = Class.forName(cName);
                if (clazz.isAnnotationPresent(MyService.class)) {
                    MyService myService = clazz.getAnnotation(MyService.class);
                    Object instance = clazz.newInstance();

                    if(StringUtils.isEmpty(myService.value())){
                        beans.put(clazz.getName().toLowerCase(), instance);
                    } else {
                        beans.put(myService.value().toLowerCase(), instance);
                    }
                } else if (clazz.isAnnotationPresent(WebServlet.class)){
                    Object instance = clazz.newInstance();
                    WebServlet webServlet = clazz.getAnnotation(WebServlet.class);
                    beans.put(webServlet.name(), instance);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void scanPackage(String s) {
        URL url = this.getClass().getClassLoader().getResource("../../src/main/java/" + s.replaceAll("\\.", "/"));
        System.out.println(url);
        String fileStr = url.getFile();
        File file = new File(fileStr);
        String fileArray[] = file.list();
        for (String path : fileArray) {
            File filePath = new File(fileStr + path);
            //递归
            if (filePath.isDirectory()) {
                scanPackage(s + "/" + path);
            } else {
                classNames.add(s + "." + filePath.getName());
            }

        }
    }
    public List<String> getFileName(String packgePath){
        List<String> filePaths=new ArrayList<>();
        String filePath=packgePath;
        File file=new File(filePath);
        System.out.println(file);
        //判断是否为目录
        if(file.isDirectory()){
            //得到包下所有文件
            File files[]=file.listFiles();
            for (File file2 : files) {
                //判断是否为目录
                if(file2.isDirectory()){
                    //递归调用
                    filePaths.addAll(getFileName(file2.getPath()));
                }else{
                    //如果后缀为class则把该文件路径放入集合
                    if(file2.getName().substring(file2.getName().lastIndexOf(".")+1).equals("class")){
                        filePaths.add(file2.getPath());
                        classNames.add(file2.getPath());
                    }
                }
            }
        }
        return filePaths;
    }

    //返回所有java文件的class
    public List<Class> getFileClass(List<String> filePath) throws ClassNotFoundException{
        List<Class> list=new ArrayList<Class>();
        for (String tempFileName : filePath) {
            //从项目路径之后开始截取java文件名
            String tempClassName=tempFileName.substring(projectPath.length()-1);
            //把路径中的“\”替换成“.”例如“com\test\test.java”替换后“com.test.test.java”
            tempClassName=tempClassName.replaceAll("\\\\",".");
            //再把后面的“.java”截取掉 然后使用Class.forName得到该类的class,并放入集合
            list.add(Class.forName(tempClassName.substring(0,tempClassName.lastIndexOf("."))));
        }
        return list;
    }

}
