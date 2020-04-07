package com.lagou.edu.aop;

import com.lagou.edu.annotation.MyTransactional;
import com.lagou.edu.factory.BeanFactory;
import com.lagou.edu.utils.TransactionManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.sql.SQLException;

@Aspect
public class AopHandler {

    TransactionManager transactionUtils;

    private ProceedingJoinPoint proceedingJoinPoint;

    @Pointcut("execution(* com.lagou.edu.service.impl.TransferServiceImpl.transfer(..))")
    public void transactionHandler() {}

    @Around("transactionHandler()")
    public void transactionHandlerAround (ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        System.out.println("======");

        transactionUtils = (TransactionManager) BeanFactory.getBean("transactionManager");

        if (hasTransaction(proceedingJoinPoint)) {
            transactionUtils.beginTransaction();
        }

        proceedingJoinPoint.proceed();

        if (hasTransaction(proceedingJoinPoint)) {
            transactionUtils.commit();
        }
    }

    /**
     * 判断切入点是否标注了@MyTransactional注解
     * @param proceedingJoinPoint
     */
    private boolean hasTransaction(ProceedingJoinPoint proceedingJoinPoint) throws NoSuchMethodException {
        this.proceedingJoinPoint = proceedingJoinPoint;
        //获取方法名
        String methodName = proceedingJoinPoint.getSignature().getName();
        //获取方法所在类的class对象
        Class clazz = proceedingJoinPoint.getSignature().getDeclaringType();
        //获取参数列表类型
        Class[] parameterTypes = ((MethodSignature) proceedingJoinPoint.getSignature()).getParameterTypes();
        //根据方法名和方法参列各参数类型可定位类中唯一方法
        Method method = clazz.getMethod(methodName, parameterTypes);
        //根据方法对象获取方法上的注解信息
        MyTransactional myTransactional = method.getAnnotation(MyTransactional.class);
        return myTransactional == null ? false : true;
    }

    @AfterThrowing("transactionHandler()")
    public void handleTransactionRollback() throws NoSuchMethodException, SQLException {
        transactionUtils = (TransactionManager) BeanFactory.getBean("transactionManager");
        if (hasTransaction(proceedingJoinPoint)) {
            //获取当前事务并回滚
            transactionUtils.rollback();
        }
    }
}
