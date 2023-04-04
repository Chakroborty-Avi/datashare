package org.icij.datashare.tasks;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public interface TaskManager extends TaskRepository {
    TaskView<Void> startTask(Runnable task);
    <V> TaskView<V> startTask(Callable<V> task, Runnable callback);
    <V> TaskView<V> startTask(Callable<V> task, Map<String, Object> properties);
    <V> TaskView<V> startTask(Callable<V> task);
    <V> TaskView<V> takeTask() throws InterruptedException;

    boolean stopTask(String taskName);
    <V> TaskView<?> clearTask(String taskName);
    boolean shutdownAndAwaitTermination(int timeout, TimeUnit timeUnit) throws InterruptedException;
}
