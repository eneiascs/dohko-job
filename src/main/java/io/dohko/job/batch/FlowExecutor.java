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
import static org.excalibur.core.execution.domain.TaskStatus.newTaskStatus;
import static org.excalibur.core.execution.domain.TaskStatus.runningTaskStatus;
import static org.excalibur.core.execution.domain.TaskStatusType.FAILED;
import static org.excalibur.core.execution.domain.TaskStatusType.FINISHED;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

import io.airlift.command.CommandFailedException;
import job.flow.Flow;


public class FlowExecutor 
{
	private static final Logger LOG = LoggerFactory.getLogger(FlowExecutor.class);
	
	private final Flow flow;
	private final Executor executor;
	private final AtomicBoolean wasCancelled = new AtomicBoolean(false);
	private final EventBus eventBus;
	
	public FlowExecutor(Flow flow, Executor executor) 
	{
		this(flow, executor, new EventBus(format("%s-event-bus", flow.getName())));
	}
	
	public FlowExecutor(Flow flow, Executor executor, EventBus eventDispatcher)
	{
		this.flow = requireNonNull(flow, "flow is null");
		this.executor = requireNonNull(executor, "step's executor is null");
		eventBus = requireNonNull(eventDispatcher, "Event dispatcher is null");
	}
	
	public <T> FlowExecutor registerListener(T listener)
	{
		this.eventBus.register(requireNonNull(listener, "listener is null"));
		return this;
	}

	public FlowExecutionResult execute() 
	{
		final FlowExecutionResult flowResult = new FlowExecutionResult(flow());
		flow.steps().forEach(step -> 
		{
			if (!wasCancelled())
			{
				StepExecutionResult stepExecutionResult = new StepExecutionResult(step);

				try 
				{
					eventBus.post(runningTaskStatus(step.id(), step.name()));
					
					LOG.info("Executing the task [{},{}]", step.getId(), step.getName());
					
					stepExecutionResult.setResult(new TaskExecutionResult(step.getId(), step.execute(executor)));
					
					LOG.info("Finished task [{},{}] with exitcode [{}]", step.getId(), step.getName(), stepExecutionResult.getResult().getExitCode());
					
					if (LOG.isDebugEnabled())
					{
						LOG.debug("Task [{},{}]'s output is [{}]", step.getId(), step.getName(), stepExecutionResult.getResult().getOutput());
					}
					
					eventBus.post(newTaskStatus(step.id(), step.name(), FINISHED));
					
					eventBus.post(stepExecutionResult.getResult());
				} 
				catch (CommandFailedException cfe) 
				{
					if (LOG.isDebugEnabled())
					{
						LOG.debug("The reason for task [{}, {}] is", step.getId(), step.getName(), cfe);
					}
					
					stepExecutionResult.setException(cfe);
					eventBus.post(newTaskStatus(step.id(), step.name(), FAILED));
				}
				finally
				{
					flowResult.addStepResult(stepExecutionResult);
				}
			} 
			else 
			{
				flowResult.setWasCancelled(true);
			}
		});
		
		eventBus.post(flowResult);

		return flowResult;
	}
	
	public FlowExecutor cancel()
	{
		if (wasCancelled.compareAndSet(false, true))
		{
			LOG.info("Flow [{}] was successfully cancelled", flow.getName());
		}
		
		return this;
	}
	
	public Flow flow() 
	{
		return this.flow;
	}
	
	public boolean wasCancelled()
	{
		return this.wasCancelled.get();
	}
}