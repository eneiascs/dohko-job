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

import org.excalibur.core.execution.domain.TaskStatus;
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
			TaskStatus runningTaskStatus = runningTaskStatus(step.id(), step.name());
			eventBus.post(runningTaskStatus);
			eventBus.post(new TaskMessage(runningTaskStatus.getTaskId(),runningTaskStatus.getTaskName(),runningTaskStatus.getType(),null,runningTaskStatus.getDate()));
			
			LOG.info("Executing the task [{},{}]", step.getId(), step.getName());

			result.setResult(new TaskExecutionResult(step.getId(), step.execute(executor)));
			Integer exitCode = isTimeout(result.getOutput()) ? 9 : Integer.parseInt(getExitCode(result.getOutput()));
			if (isError(result.getOutput())||isTimeout(result.getOutput())) {
				
				throw new CommandFailedException(step.getAction().build(), exitCode, null,
						result.getOutput());
			}

			LOG.info("Finished task [{},{}] with exitcode [{}]", step.getId(), step.getName(),
					exitCode);

			LOG.info("Task [{},{}]'s output is [{}]", step.getId(), step.getName(), result.getOutput());

			TaskStatus finishedStatus = newTaskStatus(step.id(), step.name(), FINISHED);
			eventBus.post(finishedStatus);
			eventBus.post(result.getResult());
			eventBus.post(new TaskMessage(finishedStatus.getTaskId(),finishedStatus.getTaskName(),finishedStatus.getType(),result.getResult().getOutput(),finishedStatus.getDate()));
			
			
		} catch (CommandTimeoutException cfe) {

			LOG.info("Task [{},{}] timeout", step.getId(), step.getName());
			TaskStatus taskStatus=newTaskStatus(step.id(), step.name(), FAILED);
			eventBus.post(taskStatus);
			eventBus.post(new TaskMessage(taskStatus.getTaskId(),taskStatus.getTaskName(),taskStatus.getType(),result.getResult().getOutput(),taskStatus.getDate()));

		}

		catch (CommandFailedException cfe) {
			
			LOG.info("Task [{},{}] failed with exitcode [{}]", step.getId(), step.getName(), cfe.getExitCode());
			result.setException(cfe);

			LOG.info(format("The reason is %s", cfe.getOutput()), cfe);
			TaskStatus taskStatus=newTaskStatus(step.id(), step.name(), FAILED);
			eventBus.post(taskStatus);
			eventBus.post(new TaskExecutionResult(step.getId(),
					new CommandResult(randomUUID().toString(),
							cfe.getPid() != null ? Long.valueOf(cfe.getPid().intValue()) : null, cfe.getExitCode(),
							cfe.getOutput(), 0L)));
			eventBus.post(new TaskMessage(taskStatus.getTaskId(),taskStatus.getTaskName(),taskStatus.getType(),cfe.getOutput(),taskStatus.getDate()));
		}

		return result;
	}

	
	private boolean isTimeout(String output) {
		final String timeoutRegex = "walltimelimit *(\\d+\\.?\\d*)";
		final Pattern timeoutPattern = Pattern.compile(timeoutRegex);
		final Matcher timeoutMatcher = timeoutPattern.matcher(step.getAction().build().getCommand().toString());
		Double timeout=new Double(3600);
		if (timeoutMatcher.find() && timeoutMatcher.groupCount()>0){
			timeout=new Double(timeoutMatcher.group(1));		
		}
		Double walltime=new Double(0);
		final String walltimeRegex = "walltime= *(\\d+\\.?\\d*)";
		final Pattern walltimePattern = Pattern.compile(walltimeRegex);
		final Matcher walltimeMatcher = walltimePattern.matcher(output);
		if (walltimeMatcher.find() && walltimeMatcher.groupCount()>0){
			walltime=new Double(walltimeMatcher.group(1));
			
		}
		LOG.info("Command: {}",step.getAction().build().getCommand().toString());
		LOG.info("Output: {}",output);
		LOG.info("Timeout: {}",timeout);
		LOG.info("walltime: {}",walltime);
		return walltime.compareTo(timeout)>0;
	}

	private String getExitCode(String output) {
		final String exitcodeRegex = "exitcode= *(\\d+\\.?\\d*)";
		final Pattern exitcodePattern = Pattern.compile(exitcodeRegex);
		final Matcher exitcodeMatcher = exitcodePattern.matcher(output);
		if (exitcodeMatcher.find() && exitcodeMatcher.groupCount() >0)
			return exitcodeMatcher.group(1);
		else return null;
		
	}

	
	private boolean isError(final String output) {
		
		return !"0".equals(getExitCode(output))||containsTerminationReason(output);
	}

	private boolean containsTerminationReason(String output) {
		final String exitcodeRegex = "terminationreason= *([A-z]*)";
		final Pattern exitcodePattern = Pattern.compile(exitcodeRegex);
		final Matcher exitcodeMatcher = exitcodePattern.matcher(output);
		
		return exitcodeMatcher.find();
	}

	public StepExecutionResult cancel() {
		StepExecutionResult result = new StepExecutionResult(step);

		LOG.info("Cancelling task [{},{}] ", step.getId(), step.getName());
		
		TaskStatus cancelledStatus = newTaskStatus(step.id(), step.name(), CANCELLED);
		eventBus.post(cancelledStatus);
		eventBus.post(new TaskMessage(cancelledStatus.getTaskId(),cancelledStatus.getTaskName(),cancelledStatus.getType(),null,cancelledStatus.getDate()));
		
		return result;
	}
}
