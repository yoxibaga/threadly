package org.threadly.concurrent.future;

/**
 * Callback for accepting the results of a future once the future has completed.
 * 
 * @since 1.2.0
 * @param <T> The result object type returned by this future
 */
public interface FutureCallback<T> {
  /**
   * Called once a result was produced successfully.
   * 
   * @param result Result that was provided from the future.
   */
  public void handleResult(T result);
  
  /**
   * Called once a future has completed, but completed with either a failure or a cancellation.  
   * If the original task threw an exception (and thus the future threw an ExecutionException), 
   * that original cause is provided here.
   * <p>
   * If the future was canceled then a CancellationException will be provided.
   * 
   * @param t Throwable representing the future failure.
   */
  public void handleFailure(Throwable t);
}
