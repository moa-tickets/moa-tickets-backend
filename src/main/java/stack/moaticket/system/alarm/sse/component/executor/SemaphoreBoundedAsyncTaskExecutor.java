package stack.moaticket.system.alarm.sse.component.executor;

import org.springframework.core.task.AsyncTaskExecutor;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class SemaphoreBoundedAsyncTaskExecutor implements AsyncTaskExecutor {
    public enum Mode { BLOCK, FAIL_FAST }

    private final AsyncTaskExecutor delegate;
    private final Semaphore semaphore;
    private final Mode mode;
    private final long acquireTimeoutMillis;

    private final AtomicLong rejected = new AtomicLong(0L);

    public SemaphoreBoundedAsyncTaskExecutor(
            AsyncTaskExecutor delegate,
            int maxConcurrent,
            Mode mode,
            long acquireTimeoutMillis) {
        this.delegate = delegate;
        this.semaphore = new Semaphore(maxConcurrent, true);
        this.mode = mode;
        this.acquireTimeoutMillis = acquireTimeoutMillis;
    }

    private void acquire() {
        try {
            boolean ok;
            if(mode == Mode.BLOCK) {
                semaphore.acquire();
            } else {
                if(acquireTimeoutMillis <= 0) {
                    ok = semaphore.tryAcquire();
                } else {
                    ok = semaphore.tryAcquire(acquireTimeoutMillis, TimeUnit.MILLISECONDS);
                }
                if(!ok) {
                    rejected.incrementAndGet();
                    throw new RejectedExecutionException("Semaphore rejected");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RejectedExecutionException("Interrupted while acquiring semaphore", e);
        }
    }

    private Runnable wrap(Runnable task) {
        return () -> {
            try {
                task.run();
            } finally {
                semaphore.release();
            }
        };
    }

    private <T> Callable<T> wrap(Callable<T> task) {
        return () -> {
            try {
                return task.call();
            } finally {
                semaphore.release();
            }
        };
    }

    @Override
    public void execute(Runnable task) {
        acquire();
        delegate.execute(wrap(task));
    }

    @Override
    public Future<?> submit(Runnable task) {
        acquire();
        return delegate.submit(wrap(task));
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        acquire();
        return delegate.submit(wrap(task));
    }

    public long rejectCount() {
        return rejected.get();
    }

    public int availablePermits() {
        return semaphore.availablePermits();
    }
}
