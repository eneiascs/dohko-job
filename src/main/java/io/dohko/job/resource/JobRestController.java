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
package io.dohko.job.resource;

import java.util.ArrayList;
import java.util.List;

import org.excalibur.core.domain.User;
import org.excalibur.core.execution.domain.Application;
import org.excalibur.core.execution.domain.ApplicationDescriptor;
import org.excalibur.core.execution.domain.JobStatus;
import org.excalibur.core.execution.domain.TaskOutput;
import org.excalibur.core.execution.domain.TaskStats;
import org.excalibur.core.execution.domain.TaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableList;

import io.dohko.job.batch.JobService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

@RestController
@RequestMapping(value = "/{username}/jobs")
@Api(value = "jobs", tags = "Job API")
public class JobRestController 
{
	private final JobService service;

	@Autowired
	public JobRestController(JobService service)
	{
		this.service = service;
	}
		
	@RequestMapping(method = RequestMethod.POST, produces = {"application/json"})
	@ResponseStatus(HttpStatus.CREATED)
	@ApiOperation(value = "Creates and schedules a new job based on a given job's description", response = JobStatus.class)
	public @ResponseBody JobStatus create(@PathVariable("username") String user, @RequestBody ApplicationDescriptor job)
	{
		requireNonNull(job.getUser(), "job's username is undefined");
		checkState(user.equals(job.getUser().getUsername()), "job's user and resource's user are different");
		JobStatus status = service.create(job);
		return status;
	}
	
	@RequestMapping(value = "{jobId}", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody ApplicationDescriptor get(@PathVariable("username") String user, @PathVariable("jobId") String jobId)
	{
		return service.getJob(user, jobId).orNull();
	}
	
	@RequestMapping(value = "/{jobId}/status", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody JobStatus getJobStatus(@PathVariable("username") String user, @PathVariable("jobId") String jobId)
	{
		return service.getJobTaskStatuses(jobId).or(new JobStatus().setId(jobId));
	}
	
	@RequestMapping(value = "/{jobId}/tasks", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody List<Application> jobTasklets(@PathVariable("username") String user, @PathVariable("jobId") String jobId)
	{
		return service.getTasksOfJob(jobId);
	}
	
	@RequestMapping(value = "/{jobId}/task/{taskId}/status", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody TaskStatus status(@PathVariable("username") String user, @PathVariable("jobId") String jobId, @PathVariable("taskId") final String taskId)
	{
		return service.lastTaskStatus(jobId, taskId).orNull();
	}
	
	@RequestMapping(value = "/{jobId}/task/{taskId}/stats",  method = RequestMethod.GET, produces = {"application/json"})
	@ResponseStatus(HttpStatus.OK)
    public @ResponseBody TaskStats stats(@PathVariable("username") String user, @PathVariable("jobId") String jobId, @PathVariable("taskId") final String taskId)
    {
    	return service.getTaskStats(taskId).or(new TaskStats(taskId, new ArrayList<>(), new ArrayList<>()));
    }
	
	@RequestMapping(value = "/{jobId}/task/{taskId}/output",  method = RequestMethod.GET, produces = {"application/json"})
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody ImmutableList<TaskOutput> output(@PathVariable("username") String user, @PathVariable("jobId") final String jobId, @PathVariable("taskId") final String taskId)
	{
		return service.getTaskOutput(jobId, taskId);
	}
	
	@RequestMapping(value = "/test",  method = RequestMethod.GET, produces = {"application/json"})
	public @ResponseBody ApplicationDescriptor application(@PathVariable("username") String user)
	{
		ApplicationDescriptor result = new ApplicationDescriptor()
				.setName("test")
				.setUser(new User().setUsername("vagrant"))
				.addApplication(new Application().setName("echo").setCommandLine("echo 'hello world'"));
		
		return result;
	}
}
