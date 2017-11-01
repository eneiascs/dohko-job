/**
 *     Copyright (C) 2013-2014  the original author or authors.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License,
 *     any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package io.dohko.job.batch;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.excalibur.core.util.concurrent.DynamicExecutors;
import org.excalibur.core.util.concurrent.SerialExecutor;
import org.springframework.batch.core.JobParameters;

import com.google.common.base.Optional;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import job.flow.Job;

public class LocalShellJobLaucher implements JobLauncher 
{
	private final EventBus listeners;
	private final Map<String, Future<?>> futures = new HashMap<>();
	
	private final ListeningExecutorService executor;
	private final Executor eventBusExecutor;
	
	public LocalShellJobLaucher(ExecutorService executor)
	{
		this.executor = MoreExecutors.listeningDecorator(requireNonNull(executor, "executor is null"));
		eventBusExecutor = new SerialExecutor(Executors.newFixedThreadPool(1));
		listeners = new AsyncEventBus("localjoblaucher", eventBusExecutor);
	}

	@Override
	public <T> void registerListener(T listener) 
	{
		if (listener != null)
		{
			listeners.register(listener);
		}
	}

	@Override
	public Optional<JobExecution> run(final Job job, final JobParameters parameters) 
	{
		executor.submit(() -> 
		{
			List<ListenableFuture<?>> flowHandles = new ArrayList<>(); 
			
			final ListeningExecutorService flowExecutorService = DynamicExecutors.newListeningDynamicScalingThreadPool(format("job-%s-flows-executor", job.getName()));
			
			// Flows can be executed in parallel, whereas their steps are executed sequentially.
			job.flows().forEach(flow -> 
			{
				ListenableFuture<?> flowHandle = flowExecutorService.submit(() -> 
				{
					
					FlowExecutionResult result = new FlowExecutor(flow, DynamicExecutors.newListeningDynamicScalingThreadPool(flow.getName()))
					      .registerListener(LocalShellJobLaucher.this)
					      .execute();
					
					listeners.post(result);
				});
				
				flowHandles.add(flowHandle);
			});
		});
		
//		Futures2.addCallback(futures, callback)
		
		return Optional.absent();
	}
	
	@Subscribe
	protected void postEvent(final Object event)
	{
		listeners.post(event);
	}
	
	
	/**
	 * Cancel the execution the execution of all scheduled jobs.
	 */
	public void cancel()
	{
		futures.values().forEach(f -> f.cancel(true));
		executor.shutdownNow();
	}
}
