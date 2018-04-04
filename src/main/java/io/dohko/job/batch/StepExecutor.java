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

import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

import io.airlift.command.CommandFailedException;
import io.airlift.command.CommandTimeoutException;
import io.airlift.command.CommandResult;
import job.flow.Step;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;
import static org.excalibur.core.execution.domain.TaskStatus.newTaskStatus;
import static org.excalibur.core.execution.domain.TaskStatus.runningTaskStatus;
import static org.excalibur.core.execution.domain.TaskStatusType.FAILED;
import static org.excalibur.core.execution.domain.TaskStatusType.FINISHED;
import static org.excalibur.core.execution.domain.TaskStatusType.CANCELLED;

public class StepExecutor {
	private static final Logger LOG = LoggerFactory.getLogger(StepExecutor.class);

	private final Step step;
	private final Executor executor;
	private final EventBus eventBus;
	// private final AtomicBoolean isExecuting = new AtomicBoolean(false);

	public StepExecutor(Step task, Executor executor) {
		this(requireNonNull(task, () -> "step to execute is null"), executor,
				new EventBus(format("step-%s-event-bus", task.name())));
	}

	public StepExecutor(Step task, Executor executor, EventBus eventDispatcher) {
		this.step = requireNonNull(task, () -> "step to execute is null");
		this.eventBus = requireNonNull(eventDispatcher, () -> "Event dispatcher is null");
		this.executor = requireNonNull(executor, () -> "step's executor is null");
		;
	}

	public <L> StepExecutor registerListener(L listener) {
		if (listener != null) {
			this.eventBus.register(listener);
		}

		return this;
	}

	public StepExecutionResult execute() {
		StepExecutionResult result = new StepExecutionResult(step);

		try {
			eventBus.post(runningTaskStatus(step.id(), step.name()));

			LOG.info("Executing the task [{},{}]", step.getId(), step.getName());

			result.setResult(new TaskExecutionResult(step.getId(), step.execute(executor)));

			final String regex = "exitcode= *(\\d+\\.?\\d*)";

			final Pattern pattern = Pattern.compile(regex);
			final Matcher matcher = pattern.matcher(result.getOutput());
			if (matcher.find() && matcher.groupCount() >= 1 && !"0".equals(matcher.group(1))) {
				
				throw new CommandFailedException(step.getAction().build(), Integer.parseInt(matcher.group(1)), null,
						result.getOutput());
			}

			LOG.info("Finished task [{},{}] with exitcode [{}]", step.getId(), step.getName(),
					result.getResult().getExitCode());

			LOG.info("Task [{},{}]'s output is [{}]", step.getId(), step.getName(), result.getOutput());

			eventBus.post(newTaskStatus(step.id(), step.name(), FINISHED));
			eventBus.post(result.getResult());
		} catch (CommandTimeoutException cfe) {

			LOG.info("Task [{},{}] timeout", step.getId(), step.getName());
			eventBus.post(newTaskStatus(step.id(), step.name(), FAILED));

		}

		catch (CommandFailedException cfe) {

			LOG.info("Task [{},{}] failed with exitcode [{}]", step.getId(), step.getName(), result.getExitCode());
			result.setException(cfe);

			LOG.info(format("The reason is %s", cfe.getOutput()), cfe);

			eventBus.post(newTaskStatus(step.id(), step.name(), FAILED));
			eventBus.post(new TaskExecutionResult(step.getId(),
					new CommandResult(randomUUID().toString(),
							cfe.getPid() != null ? Long.valueOf(cfe.getPid().intValue()) : null, cfe.getExitCode(),
							cfe.getOutput(), 0L)));
		}

		return result;
	}

	public StepExecutionResult cancel() {
		StepExecutionResult result = new StepExecutionResult(step);

		LOG.info("Cancelling task [{},{}] ", step.getId(), step.getName());

		eventBus.post(newTaskStatus(step.id(), step.name(), CANCELLED));

		return result;
	}
}
