package top.cutexingluo.tools.designtools.juc.async;

import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * ExecutorCompletionService 的 工具
 *
 * @author XingTian
 * @version 1.0.0
 * @date 2023/10/5 15:27
 */
//--------------CompletionService--------------
public class XTCompletionService<V> extends ExecutorCompletionService<V> {

    public XTCompletionService(Executor executor) {
        super(executor);
    }

    public XTCompletionService(Executor executor, BlockingQueue<Future<V>> completionQueue) {
        super(executor, completionQueue);
    }

    public static <T> XTCompletionService<T> newInstance(Executor executor) {
        return new XTCompletionService<>(executor);
    }

    public static <T> XTCompletionService<T> newInstance(Executor executor, BlockingQueue<Future<T>> completionQueue) {
        return new XTCompletionService<>(executor, completionQueue);
    }

    /**
     * 执行全部
     */
    public ArrayList<Future<V>> submitAll(@NonNull Collection<Callable<V>> tasks) {
        ArrayList<Future<V>> futures = new ArrayList<>();
        for (Callable<V> task : tasks) {
            if (task == null) continue;
            futures.add(submit(task));
        }
        return futures;
    }

    /**
     * 执行全部
     */
    public ArrayList<Future<V>> submitAll(@NonNull List<Runnable> tasks, @NonNull List<V> results) {
        ArrayList<Future<V>> futures = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i) == null) continue;
            V result = i < results.size() ? results.get(i) : null;
            futures.add(submit(tasks.get(i), result));
        }
        return futures;
    }


}