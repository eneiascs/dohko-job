/**
 *     Copyright (C) 2013-2017  the original author or authors.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.excalibur.core.util.concurrent.Futures2;
import org.excalibur.core.util.concurrent.SerialExecutor;
import org.springframework.batch.core.JobParameters;

import com.google.common.base.Optional;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import io.dohko.job.batch.tree.Tree;
import io.dohko.job.batch.tree.TreeNode;
import job.flow.Job;
import job.flow.Step;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import static org.excalibur.core.util.concurrent.DynamicExecutors.*;

public class LocalShellJobLaucher implements JobLauncher 
{
	private final EventBus subscribers;
	private final Map<String, Future<?>> futures = new HashMap<>();
	
	private final ListeningExecutorService executor;
	private final Executor eventBusExecutor;
	
	public LocalShellJobLaucher(ExecutorService executor)
	{
		this.executor = MoreExecutors.listeningDecorator(requireNonNull(executor, "executor is null"));
		eventBusExecutor = new SerialExecutor(Executors.newFixedThreadPool(1));
		subscribers = new AsyncEventBus("localjoblaucher", eventBusExecutor);
	}

	@Override
	public <T> void registerListener(T listener) 
	{
		if (listener != null)
		{
			subscribers.register(listener);
		}
	}
	
	public void submitJobs (List<Tree<Step>> jobs)
	{
		jobs.forEach(job -> 
		{
			submitStep(job);
		});
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void submitStep(Tree<Step> job) 
	{
		ListeningExecutorService executor = newListeningDynamicScalingThreadPool("job-executor");
		
		Futures2.addCallback(executor.submit(() -> 
		{
			execute(job.getRoot());
		}), new FutureCallback() 
		{
			@Override
			public void onSuccess(Object result) 
			{
				executor.shutdownNow();
			}

			@Override
			public void onFailure(Throwable t) 
			{
				executor.shutdownNow();
			}
		});
	}
	
	void execute(TreeNode<Step> task) 
	{
		final Step step = task.getData();
		ListeningExecutorService executor = newListeningDynamicScalingThreadPool(format("step-executor-%s", step.getName()));
		ListenableFuture<StepExecutionResult> handle = executor.submit(new Callable<StepExecutionResult>() 
		{
			@Override
			public StepExecutionResult call() throws Exception 
			{
				return new StepExecutor(step, executor)
						.registerListener(LocalShellJobLaucher.this)
						.execute();
			}
		});
		
		Futures2.addCallback(handle, new FutureCallback<StepExecutionResult>() 
		{
			@Override
			public void onSuccess(StepExecutionResult result) 
			{
				if (result.isSuccessfully())
				{
					for (TreeNode<Step> child: task.children())
					{
						execute(child);
					}
				}
				
				executor.shutdown();
			}

			@Override
			public void onFailure(Throwable t) 
			{
				executor.shutdown();
			}
		});
	}

	public void run(final Iterable<Job> jobs)
	{
		//TODO include one callback to update job status when all tasks have been finished
		executor.submit(() -> 
		{
			for (Job job : jobs)
			{
				execute(job);
			}
		});
	}

	@Override
	public Optional<JobExecution> run(final Job job, final JobParameters parameters) 
	{
		executor.submit(() -> 
		{
			execute(job);
		});
		
//		Futures2.addCallback(futures, callback)
		
		return Optional.absent();
	}

	private void execute(final Job job) 
	{
		List<ListenableFuture<?>> flowHandles = new ArrayList<>(); 
		
		final ListeningExecutorService flowExecutorService = newListeningDynamicScalingThreadPool(format("job-%s-flows-executor", job.getName()));
		
		// Flows can be executed in parallel, whereas their steps are executed sequentially.
		job.flows().forEach(flow -> 
		{
			ListenableFuture<?> flowHandle = flowExecutorService.submit(() -> 
			{
				FlowExecutionResult result = new FlowExecutor(flow, newListeningDynamicScalingThreadPool(flow.getName()))
				      .registerListener(LocalShellJobLaucher.this)
				      .execute();
				
				subscribers.post(result);
			});
			
			flowHandles.add(flowHandle);
		});
	}
	
	@Subscribe
	protected void postEvent(final Object event)
	{
		subscribers.post(event);
	}
	
	
	/**
	 * Cancel the execution the execution of all scheduled jobs.
	 */
	public void cancel()
	{
		futures.values().forEach(f -> f.cancel(true));
		executor.shutdownNow();
	}

	public void submitBlocksToExecution(List<Tree<BlockAdapter>> trees) 
	{
		trees.forEach(tree -> 
		{
			TreeNode<BlockAdapter> root = tree.root();
			scheduleBlockExecution(root);
		});
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void scheduleBlockExecution(TreeNode<BlockAdapter> node)
	{
		ListeningExecutorService executor = newListeningDynamicScalingThreadPool("block-executor");
		
		ListenableFuture<?> future = executor.submit(() -> 
		{
			handleBlock(node.getData(), executor);
		});
		
		Futures2.addCallback(future, new FutureCallback() 
		{
			@Override
			public void onSuccess(Object result) 
			{
				executor.shutdown();
			}

			@Override
			public void onFailure(Throwable t) 
			{
				executor.shutdown();
			}
		});
		
//		return future;
		
	}
	
	void handleBlock(BlockAdapter block, ListeningExecutorService executor)
	{
		for (int i = 0; i < block.getBlock().getRepeat(); i++)
		{
			handle(block.getApplicationTree().root());
		}
	}
	
	StepExecutionResult handle(TreeNode<Step> node)
	{
		StepExecutionResult result = new StepExecutor(node.getData(), executor)
				.registerListener(this)
				.execute();
		
		if (result.isSuccessfully())
		{
			for (TreeNode<Step> child: node.children())
			{
				handle(child);
			}
		}
		
		return result;
	}
}
