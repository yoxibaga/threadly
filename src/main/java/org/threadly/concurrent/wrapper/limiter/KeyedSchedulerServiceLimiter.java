package org.threadly.concurrent.wrapper.limiter;

import java.util.concurrent.Callable;

import org.threadly.concurrent.SchedulerService;
import org.threadly.concurrent.wrapper.traceability.ThreadRenamingSchedulerService;
import org.threadly.util.StringUtils;

/**
 * This is a cross between the {@link org.threadly.concurrent.wrapper.KeyDistributedScheduler} and 
 * a {@link SchedulerServiceLimiter}.  This is designed to limit concurrency for a given thread, 
 * but permit more than one thread to run at a time for a given key.  If the desired effect is to 
 * have a single thread per key, {@link org.threadly.concurrent.wrapper.KeyDistributedScheduler} 
 * is a much better option.
 * <p>
 * The easiest way to use this class would be to have it distribute out schedulers through 
 * {@link #getSubmitterSchedulerForKey(Object)}.
 * 
 * @since 4.6.0 (since 4.3.0 at org.threadly.concurrent.limiter)
 */
public class KeyedSchedulerServiceLimiter extends AbstractKeyedSchedulerLimiter<SchedulerServiceLimiter> {
  protected final SchedulerService scheduler;
  
  /**
   * Construct a new {@link KeyedSchedulerServiceLimiter} providing only the backing scheduler 
   * and the maximum concurrency per unique key.  By default this will not rename threads for 
   * tasks executing.
   * 
   * @param scheduler Scheduler to execute and schedule tasks on
   * @param maxConcurrency Maximum concurrency allowed per task key
   */
  public KeyedSchedulerServiceLimiter(SchedulerService scheduler, int maxConcurrency) {
    this(scheduler, maxConcurrency, null, false);
  }

  /**
   * Construct a new {@link KeyedSchedulerServiceLimiter} providing the backing scheduler, the maximum 
   * concurrency per unique key, and how keyed limiter threads should be named.
   * 
   * @param scheduler Scheduler to execute and schedule tasks on
   * @param maxConcurrency Maximum concurrency allowed per task key
   * @param subPoolName Name prefix for sub pools, {@code null} to not change thread names
   * @param addKeyToThreadName If {@code true} the key's .toString() will be added in the thread name
   */
  public KeyedSchedulerServiceLimiter(SchedulerService scheduler, int maxConcurrency, 
                                      String subPoolName, boolean addKeyToThreadName) {
    this(scheduler, maxConcurrency, subPoolName, addKeyToThreadName, DEFAULT_LOCK_PARALISM);
  }

  /**
   * Construct a new {@link KeyedSchedulerServiceLimiter} providing the backing scheduler, the 
   * maximum concurrency per unique key, and how keyed limiter threads should be named.
   * <p>
   * The parallelism value should be a factor of how many keys are submitted to the pool during any 
   * given period of time.  Depending on task execution duration, and quantity of threads executing 
   * tasks this value may be able to be smaller than expected.  Higher values result in less lock 
   * contention, but more memory usage.  Most systems will run fine with this anywhere from 4 to 64.
   * 
   * @param scheduler Scheduler to execute and schedule tasks on
   * @param maxConcurrency Maximum concurrency allowed per task key
   * @param subPoolName Name prefix for sub pools, {@code null} to not change thread names
   * @param addKeyToThreadName If {@code true} the key's .toString() will be added in the thread name
   * @param expectedParallism Expected concurrent task addition access, used for performance tuning
   */
  public KeyedSchedulerServiceLimiter(SchedulerService scheduler, int maxConcurrency, 
                                      String subPoolName, boolean addKeyToThreadName, 
                                      int expectedParallism) {
    super(scheduler, maxConcurrency, subPoolName, addKeyToThreadName, expectedParallism);
    
    this.scheduler = scheduler;
  }
  
  @Override
  protected SchedulerServiceLimiter makeLimiter(String limiterThreadName) {
    return new SchedulerServiceLimiter(StringUtils.isNullOrEmpty(limiterThreadName) ? 
                                         scheduler : new ThreadRenamingSchedulerService(scheduler, 
                                                                                        limiterThreadName, 
                                                                                        false), 
                                       getMaxConcurrencyPerKey());
  }

  /**
   * Removes the runnable task from the execution queue.  It is possible for the runnable to still 
   * run until this call has returned.
   * <p>
   * See also: {@link SchedulerService#remove(Runnable)}
   * 
   * @param task The original task provided to the executor
   * @return {@code true} if the task was found and removed
   */
  public boolean remove(Runnable task) {
    for (LimiterContainer limiter : currentLimiters.values()) {
      if (limiter.limiter.remove(task)) {
        limiter.handlingTasks.decrementAndGet();
        return true;
      }
    }
    return false;
  }

  /**
   * Removes the runnable task from the execution queue.  It is possible for the runnable to still 
   * run until this call has returned.
   * <p>
   * See also: {@link SchedulerService#remove(Callable)}
   * 
   * @param task The original task provided to the executor
   * @return {@code true} if the task was found and removed
   */
  public boolean remove(Callable<?> task) {
    for (LimiterContainer limiter : currentLimiters.values()) {
      if (limiter.limiter.remove(task)) {
        limiter.handlingTasks.decrementAndGet();
        return true;
      }
    }
    return false;
  }

  /**
   * Call to check how many tasks are currently being executed in this scheduler.
   * <p>
   * See also: {@link SchedulerService#getActiveTaskCount()}
   * 
   * @return current number of running tasks
   */
  public int getActiveTaskCount() {
    return scheduler.getActiveTaskCount();
  }

  /**
   * Returns how many tasks are either waiting to be executed, or are scheduled to be executed at 
   * a future point.  Because this does not lock state can be modified during the calculation of 
   * this result.  Ultimately resulting in an inaccurate number.
   * <p>
   * See also: {@link SchedulerService#getQueuedTaskCount()}
   * 
   * @return quantity of tasks waiting execution or scheduled to be executed later
   */
  public int getQueuedTaskCount() {
    int result = 0;
    for (LimiterContainer limiter : currentLimiters.values()) {
      result += limiter.limiter.waitingTasks.size();
    }
    return result + scheduler.getQueuedTaskCount();
  }

  /**
   * Function to check if the thread pool is currently accepting and handling tasks.
   * <p>
   * See also: {@link SchedulerService#isShutdown()}
   * 
   * @return {@code true} if thread pool is running
   */
  public boolean isShutdown() {
    return scheduler.isShutdown();
  }
}
