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

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.UUID.randomUUID;
import static org.excalibur.core.execution.domain.TaskStatusType.PENDING;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.excalibur.core.execution.domain.Application;
import org.excalibur.core.execution.domain.ApplicationDescriptor;
import org.excalibur.core.execution.domain.JobStatus;
import org.excalibur.core.execution.domain.TaskOutput;
import org.excalibur.core.execution.domain.TaskOutputType;
import org.excalibur.core.execution.domain.TaskStats;
import org.excalibur.core.execution.domain.TaskStatus;
import org.excalibur.core.execution.domain.repository.JobRepository;
import org.excalibur.core.execution.domain.repository.TaskCpuStatsRepository;
import org.excalibur.core.execution.domain.repository.TaskMemoryStatsRepository;
import org.excalibur.core.execution.domain.repository.TaskOutputRepository;
import org.excalibur.core.execution.domain.repository.TaskRepository;
import org.excalibur.core.execution.domain.repository.TaskStatusRepository;
import org.excalibur.core.util.concurrent.DynamicExecutors;
import org.springframework.batch.core.JobParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;

import io.airlift.command.Command;
import io.airlift.command.ProcessCpuState;
import io.airlift.command.ProcessMemoryState;
import job.flow.Flow;
import job.flow.Job;
import job.flow.Step;

@Service
public class JobService 
{
//	private final EventBus bus = new EventBus("jobservice");
	
	private final JobRepository jobRepository;
	
	private final TaskRepository taskRepository;
	private final TaskStatusRepository taskStatusRepository;
	private final TaskCpuStatsRepository taskCpuStatsRepository;
	private final TaskMemoryStatsRepository taskMemoryStatsRepository;
	private final TaskOutputRepository taskOutputRepository;
	
	
	private final LocalShellJobLaucher localShellJobLaucher; 
	
	@Autowired
	public JobService (JobRepository jobRepository, TaskRepository taskRepository, TaskStatusRepository taskStatusRepository, TaskCpuStatsRepository taskCpuStatsRepository, TaskMemoryStatsRepository taskMemoryStatsRepository, TaskOutputRepository taskOutputRepository)
	{
		this.jobRepository = jobRepository;
		this.taskRepository = taskRepository;
		this.taskStatusRepository = taskStatusRepository;
		this.taskCpuStatsRepository = taskCpuStatsRepository;
		this.taskMemoryStatsRepository = taskMemoryStatsRepository;
		this.taskOutputRepository = taskOutputRepository;
		
		localShellJobLaucher = new LocalShellJobLaucher(DynamicExecutors.newListeningDynamicScalingThreadPool("job-executors"));
		localShellJobLaucher.registerListener(this);
	}
	
	@Transactional
	public JobStatus create(final ApplicationDescriptor job)
	{
		if (isNullOrEmpty(job.getId()))
		{ 
			job.setId(randomUUID().toString());
		}
		
		JobStatus result = new JobStatus(job.getId(), job.getName());
		
		jobRepository.insert(job.setCreatedIn(Instant.now().atZone(ZoneOffset.UTC).toInstant().toEpochMilli()));
		
		List<Job> jobs = new ArrayList<>();
		
		jobs.add(configureApplications(result, job.applications()));
		
		job.blocks().forEach(b -> jobs.add(configureApplications(result, b.applications())));
		
		createApplications(job.applications());
		createTaskStatuses(result.statuses());
		
		jobs.forEach(j -> localShellJobLaucher.run(j, new JobParameters()));
		
		return result;
	}
	
	protected Job configureApplications(final JobStatus jobStatus, final Iterable<Application> applications)
	{
		Job job = new Job(jobStatus.getId());
		
		applications.forEach(application -> 
		{
			if (isNullOrEmpty(application.getId()))
			{
				application.setId(randomUUID().toString());
			}
			
			application.setJobId(jobStatus.getId());
			jobStatus.addTaskStatus(
					new TaskStatus()
					  .setDate(new Date())
					  .setTaskId(application.getId())
					  .setTaskName(application.getName())
					  .setType(PENDING));
			
			Flow flow = new Flow(application.getName());
			flow.add(new Step(application.getId(), application.getName(),
					new Command("bash", "-c", application.getCommandLine())
					    .setTimeLimit(application.getTimeout() == null ? Integer.MAX_VALUE : application.getTimeout(), TimeUnit.HOURS)));
			
			job.add(flow);
		});
		
		return job;
	}
	
	
	@Transactional
	public void createApplications(Iterable<Application> apps)
	{
		taskRepository.insert(apps);
	}
	
	public void createTaskStatuses(Iterable<TaskStatus> statuses)
	{
		taskStatusRepository.insert(statuses);
	}
	
	@Subscribe
	public void createTaskStatus(final TaskStatus status)
	{
		if (status != null)
		{
			taskStatusRepository.insert(status);
		}
	}
	
	@Subscribe
	public void updateExecutionResult(TaskExecutionResult result)
	{
		if (result != null)
		{
			TaskStats stats = result.stats();
			
			taskCpuStatsRepository.insert(stats.getCpu());
			taskMemoryStatsRepository.insert(stats.getMemory());
			
			TaskOutput output = new TaskOutput()
					.setTaskId(result.getId())
					.setId(randomUUID().toString())
					.setType(TaskOutputType.SYSOUT)
					.setValue(result.getResult().getCommandOutput());
			
			taskOutputRepository.insert(output);
		}
	}
	
	
	public Optional<TaskStatus> lastTaskStatus(String taskId)
	{
		return taskStatusRepository.getLastStatusOfTask(taskId);
	}
	
	public Optional<TaskStatus> lastTaskStatus(String jobId, String taskId) 
	{
		return this.taskStatusRepository.getLastStatusOfTask(taskId);
	}

	
	public Optional<JobStatus> getJobTaskStatuses(String jobId)
	{
		Optional<JobStatus> result = Optional.absent();
		
		List<Application> tasks = taskRepository.findAllTasksOfJob(jobId);
		
		if (!tasks.isEmpty())
		{
			final JobStatus js = new JobStatus().setId(jobId);
			tasks.forEach(t -> 
			{
				Optional<TaskStatus> status = taskStatusRepository.getLastStatusOfTask(t.getId());
				js.addTaskStatus(status.orNull());
			});
			
			result = Optional.of(js);
		}
		
		return result;
	}
	
	public Optional<TaskStats> getTaskStats(final String taskId)
	{
		Optional<TaskStats> stats = Optional.absent();
		
		if (!isNullOrEmpty(taskId))
		{
			List<ProcessCpuState> cpu = taskCpuStatsRepository.getStatsOfTask(taskId);
			List<ProcessMemoryState> mem = taskMemoryStatsRepository.getStatsOfTask(taskId);
			
			if (!cpu.isEmpty() || !mem.isEmpty())
			{
				stats = Optional.of(new TaskStats(taskId, cpu, mem));
			}
		}
		return stats;
	}
	
	public List<Application> getTasksOfJob(String jobId) 
	{
		return ImmutableList.copyOf(taskRepository.findAllTasksOfJob(jobId));
	}
	

	public Optional<ApplicationDescriptor> getJob(String user, String jobId) 
	{
		Optional<ApplicationDescriptor> job = jobRepository.findByUUID(jobId);
		
		if (job.isPresent())
		{
			job.get().addApplications(getTasksOfJob(jobId));
		}
		
		return job;
	}

	public ImmutableList<TaskOutput> getTaskOutput(String jobId, String taskId) 
	{
		return ImmutableList.copyOf(this.taskOutputRepository.getAllOutputsOfTask(taskId));
	}
}
