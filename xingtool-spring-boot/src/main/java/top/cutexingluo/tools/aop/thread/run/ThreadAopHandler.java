package top.cutexingluo.tools.aop.thread.run;

import cn.hutool.extra.spring.SpringUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.jetbrains.annotations.Nullable;
import org.springframework.lang.NonNull;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import top.cutexingluo.tools.aop.thread.MainThread;
import top.cutexingluo.tools.aop.thread.SonThread;
import top.cutexingluo.tools.aop.thread.policy.ThreadAopFactory;
import top.cutexingluo.tools.aop.transactional.TransactionHandler;
import top.cutexingluo.tools.aop.transactional.TransactionMeta;
import top.cutexingluo.tools.designtools.juc.lock.handler.XTLockHandler;
import top.cutexingluo.tools.designtools.juc.lockAop.XTLockMeta;
import top.cutexingluo.tools.designtools.juc.thread.XTThreadPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Thread Aop处理器
 *
 * @author XingTian
 * @version 1.0.0
 * @date 2023/10/2 13:28
 */
public interface ThreadAopHandler {

    default Vector<Exception> newExceptionVector() {
        return new Vector<>();
    }

    default AtomicInteger newInteger(int value) {
        return new AtomicInteger(value);
    }

    default AtomicBoolean newBoolean(boolean value) {
        return new AtomicBoolean(value);
    }

    /**
     * 域
     */
    default CountDownLatch newCountDownLatch(int count) {
        return new CountDownLatch(count);
    }

    default String getMainThreadNameInMain() {
        return ThreadAopFactory.getMainThreadNameInMain();
    }

    default String getMainThreadNameInSon(ProceedingJoinPoint joinPoint, boolean lastArgThread) {
        return ThreadAopFactory.getMainThreadNameInSon(joinPoint, lastArgThread);
    }

    /**
     * 是否需要外部的map和transactionManager
     */
    boolean needAutowired();

    /**
     * 事务
     */
    void setTransactionManager(@NonNull PlatformTransactionManager transactionManager);

    @NonNull
    PlatformTransactionManager getTransactionManager();

    /**
     * 数据存储
     */
    void setMap(@NonNull ConcurrentHashMap<String, Object> map);

    /**
     * 数据存储
     */
    @NonNull
    ConcurrentHashMap<String, Object> getMap();

    /**
     * 子线程数量
     */
    default <T extends Number> void setSonCount(String key, T sonCount) {
        getMap().put(key, sonCount);
    }

    default <T extends Number> T getSonCount(String key, Class<T> clazz) {
        return (T) getMap().get(key);
    }

    default <T extends Number> T removeSonCount(String key) {
        return (T) getMap().remove(key);
    }

    default ThreadTimePolicy getThreadTimePolicy(MainThread mainThread) {
        return ThreadAopFactory.getRightTimePolicy(mainThread);
    }

    /**
     * 获得结果
     */
    default List<Object> getResultList(String threadName) {
        return (List<Object>) getMap().get(threadName + ":results");
    }

    /**
     * 设置结果
     */
    default void setResultList(String threadName, List<Object> list) {
        getMap().put(threadName + ":results", list);
    }

    /**
     * 添加结果
     */
    default void addResult(String threadName, Object result) {
        if (getResultList(threadName) == null) {
            setResultList(threadName, new ArrayList<>());
        }
        getResultList(threadName).add(result);
    }

    /**
     * 移除结果
     */
    default void removeResultList(String threadName) {
        getMap().remove(threadName + ":results");
    }


    default ExecutorService getExecutorService(SonThread sonThread) {
        ExecutorService bean = SpringUtil.getBean(sonThread.threadPoolName());
        if (bean == null) {
            bean = SpringUtil.getBean(ExecutorService.class);
            if (bean == null) {
                bean = XTThreadPool.getInstance().getThreadPool();
            }
        }
        return bean;
    }

    /**
     * 新建任务列表
     */
    default <V> void newTaskList(String key) {
        ConcurrentHashMap<String, Object> map = getMap();
        map.put(key, new ArrayList<Callable<V>>());
    }

    /**
     * 删除任务列表
     */
    default <V> void removeTaskList(String key) {
        ConcurrentHashMap<String, Object> map = getMap();
        map.remove(key);
    }

    /**
     * 添加任务
     */
    default <V> void addTask(String key, Callable<V> task) {
        ConcurrentHashMap<String, Object> map = getMap();
        if (map.containsKey(key)) {
            ArrayList<Callable<V>> tasks = (ArrayList<Callable<V>>) map.get(key);
            tasks.add(task);
        } else {
            ArrayList<Callable<V>> tasks = new ArrayList<>();
            tasks.add(task);
            getMap().put(key, tasks);
        }
    }

    /**
     * 获得任务列表
     */
    default <V> ArrayList<Callable<V>> getTaskList(String key) {
        ConcurrentHashMap<String, Object> map = getMap();
        if (map.containsKey(key)) {
            return (ArrayList<Callable<V>>) getMap().get(key);
        } else {
            return new ArrayList<>();
        }
    }


    /**
     * 1.获得任务
     */
    default <V> Callable<V> getTask(ProceedingJoinPoint joinPoint, Consumer<Exception> inCatch) {
        return () -> {
            V result = null;
            try {
                result = (V) getTask(joinPoint).call();
            } catch (Exception e) {
                if (inCatch != null) inCatch.accept(e);
            }
            return result;
        };
    }

    /**
     * 1.获得任务
     */
    default <V> Callable<V> getTask(ProceedingJoinPoint joinPoint) {
        return () -> {
            V result = null;
            try {
                if (joinPoint != null) result = (V) joinPoint.proceed();
            } catch (Exception e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            return result;
        };
    }

    /**
     * 1.获得任务, 如果不允许执行就跳过
     */
    default <V> Callable<V> getTask(ProceedingJoinPoint joinPoint, Supplier<Boolean> canRunTask) {
        return () -> {
            if (canRunTask != null && !canRunTask.get()) {
                return null;
            }
            V result = null;
            try {
                if (joinPoint != null) result = (V) joinPoint.proceed();
            } catch (Exception e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            return result;
        };
    }

    /**
     * 2.任务加事务
     */
    default <T> Callable<T> addTransaction(Callable<T> task) {
        TransactionHandler transactionHandler = new TransactionHandler(true,
                getTransactionManager(), TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionHandler.decorate(task);
    }

    /**
     * 2.任务加事务
     */
    default <T> Callable<T> addTransaction(Callable<T> task, @Nullable BiConsumer<T, TransactionMeta> inTry, @Nullable BiConsumer<Exception, TransactionMeta> inCatch) {
        TransactionHandler transactionHandler = new TransactionHandler(true,
                getTransactionManager(), TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionHandler.decorate(task, inTry, inCatch);
    }

    /**
     * 任务加锁
     */
    default <T> Callable<T> addLock(Callable<T> task, SonThread sonThread) {
        XTLockMeta meta = new XTLockMeta(sonThread.lockName(), sonThread.isFair(), sonThread.lockType(), sonThread.tryTimeout());
        return addLock(task, meta, sonThread.redisson());
    }

    /**
     * 任务加锁
     */
    default <T> Callable<T> addLock(Callable<T> task, XTLockMeta meta, boolean openRedissonClient) {
        XTLockHandler lockHandler = new XTLockHandler(meta, openRedissonClient);
        return lockHandler.decorate(task);
    }


    /**
     * 在Main 执行的方法
     */
    Object runInMainAop(ProceedingJoinPoint joinPoint, MainThread mainThread) throws Exception;

    /**
     * 在Son 执行的方法
     */
    Object runInSonAop(ProceedingJoinPoint joinPoint, SonThread sonThread) throws Exception;
}
