package org.threadly.concurrent.wrapper.limiter;

import org.threadly.concurrent.SubmitterScheduler;
import org.threadly.concurrent.wrapper.traceability.ThreadRenamingSubmitterScheduler;
import org.threadly.util.StringUtils;

/**
 * This is a cross between the {@link org.threadly.concurrent.wrapper.KeyDistributedScheduler} and 
 * a {@link SubmitterSchedulerLimiter}.  This is designed to limit concurrency for a given 
 * thread, but permit more than one thread to run at a time for a given key.  If the desired 
 * effect is to have a single thread per key, 
 * {@link org.threadly.concurrent.wrapper.KeyDistributedScheduler} is a much better option.
 * <p>
 * The easiest way to use this class would be to have it distribute out schedulers through 
 * {@link #getSubmitterSchedulerForKey(Object)}.
 * 
 * @since 4.6.0 (since 4.3.0 at org.threadly.concurrent.limiter)
 */
public class KeyedSubmitterSchedulerLimiter extends AbstractKeyedSchedulerLimiter<SubmitterSchedulerLimiter> {
  /**
   * Construct a new {@link KeyedSubmitterSchedulerLimiter} providing only the backing scheduler 
   * and the maximum concurrency per unique key.  By default this will not rename threads for 
   * tasks executing.
   * 
   * @param scheduler Scheduler to execute and schedule tasks on
   * @param maxConcurrency Maximum concurrency allowed per task key
   */
  public KeyedSubmitterSchedulerLimiter(SubmitterScheduler scheduler, int maxConcurrency) {
    this(scheduler, maxConcurrency, null, false);
  }

  /**
   * Construct a new {@link KeyedSubmitterSchedulerLimiter} providing the backing scheduler, the maximum 
   * concurrency per unique key, and how keyed limiter threads should be named.
   * 
   * @param scheduler Scheduler to execute and schedule tasks on
   * @param maxConcurrency Maximum concurrency allowed per task key
   * @param subPoolName Name prefix for sub pools, {@code null} to not change thread names
   * @param addKeyToThreadName If {@code true} the key's .toString() will be added in the thread name
   */
  public KeyedSubmitterSchedulerLimiter(SubmitterScheduler scheduler, int maxConcurrency, 
                                        String subPoolName, boolean addKeyToThreadName) {
    this(scheduler, maxConcurrency, subPoolName, addKeyToThreadName, DEFAULT_LOCK_PARALISM);
  }

  /**
   * Construct a new {@link KeyedSubmitterSchedulerLimiter} providing the backing scheduler, the 
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
  public KeyedSubmitterSchedulerLimiter(SubmitterScheduler scheduler, int maxConcurrency, 
                                        String subPoolName, boolean addKeyToThreadName, 
                                        int expectedParallism) {
    super(scheduler, maxConcurrency, subPoolName, addKeyToThreadName, expectedParallism);
  }
  
  @Override
  protected SubmitterSchedulerLimiter makeLimiter(String limiterThreadName) {
    return new SubmitterSchedulerLimiter(StringUtils.isNullOrEmpty(limiterThreadName) ? 
                                           scheduler : new ThreadRenamingSubmitterScheduler(scheduler, 
                                                                                            limiterThreadName, 
                                                                                            false), 
                                         getMaxConcurrencyPerKey());
  }
  
  /**********
   * 
   * NO IMPLEMENTATION SHOULD EXIST HERE, THIS SHOULD ALL BE IN {@link AbstractKeyedSchedulerLimiter}
   * 
   **********/
}
