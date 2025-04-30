package org.icij.datashare.tasks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.icij.datashare.PropertiesProvider;
import org.icij.datashare.asynctasks.TaskRepository;

import java.util.concurrent.CountDownLatch;


@Singleton
public class TaskManagerMemory extends org.icij.datashare.asynctasks.TaskManagerMemory {
    private final int terminationPollingIntervalMs;

    @Inject
    public TaskManagerMemory(DatashareTaskFactory taskFactory, TaskRepository taskRepository, PropertiesProvider propertiesProvider) {
        this(taskFactory, taskRepository, propertiesProvider, new CountDownLatch(1));
    }

    TaskManagerMemory(DatashareTaskFactory taskFactory, TaskRepository taskRepository, PropertiesProvider propertiesProvider, CountDownLatch latch) {
        super(taskFactory, taskRepository, propertiesProvider, latch);
        terminationPollingIntervalMs = Integer.parseInt(propertiesProvider.get("terminationPollingIntervalMs").orElse("1000"));
    }

    @Override
    public int getTerminationPollingInterval() { return terminationPollingIntervalMs; }
}
