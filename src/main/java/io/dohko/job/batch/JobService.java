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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.excalibur.core.execution.domain.Application;
import org.excalibur.core.execution.domain.ApplicationDescriptor;
import org.excalibur.core.execution.domain.JobStatus;
import org.excalibur.core.execution.domain.Precondition;
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
import org.excalibur.core.host.repository.PackageRepository;
import org.excalibur.core.util.concurrent.DynamicExecutors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;

import io.airlift.command.Command;
import io.airlift.command.ProcessCpuState;
import io.airlift.command.ProcessMemoryState;
import io.airlift.command.ProcessState;
import io.dohko.job.host.Package;
import io.dohko.job.host.PackageManagerType;
import job.flow.Flow;
import job.flow.Job;
import job.flow.Step;


import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.hash.Hashing.sha256;
import static io.airlift.command.Command.newBashCommand;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static  java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.excalibur.core.execution.domain.TaskStatusType.PENDING;

import static java.lang.Math.*;


@Service
public class JobService 
{
	private final JobRepository jobRepository;
	
	private final TaskRepository taskRepository;
	private final TaskStatusRepository taskStatusRepository;
	private final TaskCpuStatsRepository taskCpuStatsRepository;
	private final TaskMemoryStatsRepository taskMemoryStatsRepository;
	private final TaskOutputRepository taskOutputRepository;
	private final PackageRepository packageRepository;
	private final LocalShellJobLaucher localShellJobLaucher; 
	
	@Autowired
	public JobService (JobRepository jobRepository, TaskRepository taskRepository, TaskStatusRepository taskStatusRepository, TaskCpuStatsRepository taskCpuStatsRepository, TaskMemoryStatsRepository taskMemoryStatsRepository, TaskOutputRepository taskOutputRepository, PackageRepository packageRepository)
	{
		this.jobRepository = jobRepository;
		this.taskRepository = taskRepository;
		this.taskStatusRepository = taskStatusRepository;
		this.taskCpuStatsRepository = taskCpuStatsRepository;
		this.taskMemoryStatsRepository = taskMemoryStatsRepository;
		this.taskOutputRepository = taskOutputRepository;
		this.packageRepository = packageRepository;
		
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
		
		jobRepository.insert(job.setCreatedIn(now().atZone(UTC).toInstant().toEpochMilli()));
		
		List<Job> jobs = new ArrayList<>();
		
		jobs.add(configurePreconditions(result, job.preconditions()));
		jobs.add(configureApplications(result, job.applications()));
		
		job.blocks().forEach(b -> jobs.add(configureApplications(result, b.applications())));
		
		createApplications(job.applications());
		createTaskStatuses(result.statuses());
		
//		jobs.forEach(j -> localShellJobLaucher.run(j, new JobParameters()));
		localShellJobLaucher.run(jobs);
		
		return result;
	}
	
	private Job configurePreconditions(JobStatus status, List<Precondition> preconditions) 
	{
		Job job = new Job(format("preconditions-%s", status.getId()));
		
		preconditions.forEach(precondition -> 
		{
			Flow flow = new Flow(format("preconditions-%s-flow", status.getId()));
			List<Package> packages = new ArrayList<>();
			
			precondition.getPackages().forEach( name -> 
			{
				Optional<Package> pkg = packageRepository.findByName(name);
				
				if (pkg.isPresent())
				{
					packages.add(pkg.get());
				}
			});
			
			flow.add(new Step(flow.getName(), flow.getName() + "packages", PackageManagerType.command(packages)));
			job.add(flow);
		});
		
		return job;
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
			
			final Flow flow = new Flow(application.getName());
			final Step step = new Step(application.getId(), application.getName(),
					new Command(application.getId(), "bash", "-c", application.getCommandLine())
							.registerListeners(Collections.singletonList(JobService.this))
							.setTimeLimit(application.getTimeout() == null ? 3600 : application.getTimeout(), SECONDS));
			
			application.getFiles().forEach(f -> 
			{ 
				step.action().addEnvironment(f.getName(), f.getDest());
				
				java.util.Optional<URI> uri = f.getSourceURI();
				
				if (uri.isPresent() && uri.get().getScheme() != null)
				{
					switch(uri.get().getScheme())
					{
					case "http":
					case "https":
						step.addTaskLets(newBashCommand(randomUUID().toString(), format("mkdir -p %s", f.getDest())),
								         newBashCommand(randomUUID().toString(), format("cd %s && %s %s", f.getDest(), 
								        		 getProperty("org.excalibur.default.network.downloader", "wget --no-cookies --no-check-certificate"), f.getSource().trim())));
						break;
					}
				}
			});
			
			flow.add(step);
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
			
//			if (!result.getResult().getProcessStats().isEmpty())
//			{
//				taskStatusRepository.updateTaskPid(result.getId(), result.getResult().getProcessStats().get(0).getPid());
//			}
			
//			Optional<TaskStatus> running = taskStatusRepository.getStatusOfTask(result.getId(), TaskStatusType.RUNNING);
//			Optional<TaskStatus> finished = taskStatusRepository.getStatusOfTask(result.getId(), TaskStatusType.FINISHED);
//			long elapsed = finished.get().getDate().getTime() - running.get().getDate().getTime();
			
			taskCpuStatsRepository.insert(stats.getCpu());
			taskMemoryStatsRepository.insert(stats.getMemory());
			
			TaskOutput output = new TaskOutput()
					.setTaskId(result.getId())
					.setId(randomUUID().toString())
					.setType(TaskOutputType.SYSOUT)
					.setValue(result.getOutput())
					.setChecksum(sha256().hashString(result.getOutput(), UTF_8).toString());
			
			taskOutputRepository.insert(output);
		}
	}
	
	@Subscribe
	public void updateProcessState(ProcessState ps)
	{
		taskStatusRepository.updateTaskPid(ps.getId(), ps.getPid());
		
		if (ps.getCpuState() != null)
		{
			taskCpuStatsRepository.insert(ps.getCpuState());
		}
		
		if (ps.getMemoryState() != null)
		{
			ProcessMemoryState memory = ps.getMemoryState().clone()
					.setSize(ps.getMemoryState().getSize() / pow(1000, 2))
					.setResident(ps.getMemoryState().getResident() / pow(1000, 2))
					.setShare(ps.getMemoryState().getShare() / pow(1000, 2));
			
			taskMemoryStatsRepository.insert(memory);
		}
	}
	
	@Subscribe
	public void updateJobStatus(JobExecution jobExecution)
	{
		jobRepository.finished(jobExecution.getJob().getName(), now().atZone(UTC).toInstant().toEpochMilli(), jobExecution.getElapsedTime());
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
		final Optional<ApplicationDescriptor> descriptor = jobRepository.findByUUID(jobId);
		
		if (descriptor.isPresent())
		{
			final JobStatus js = new JobStatus()
					.setCpuTime(descriptor.get().getCpuTime())
					.setId(jobId)
					.setName(descriptor.get().getName());
			
			List<Application> tasks = taskRepository.findAllTasksOfJob(jobId);
			
			if (!tasks.isEmpty())
			{
				tasks.forEach(t -> 
				{
					Optional<TaskStatus> status = taskStatusRepository.getLastStatusOfTask(t.getId());
					js.addTaskStatus(status.orNull());
				});
				
				result = Optional.of(js);
			}
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
