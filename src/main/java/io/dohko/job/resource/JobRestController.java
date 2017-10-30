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

import java.util.List;

import org.excalibur.core.execution.domain.ApplicationDescriptor;
import org.excalibur.core.execution.domain.TaskStats;
import org.excalibur.core.execution.domain.TaskStatus;
import org.excalibur.core.execution.domain.repository.TaskCpuStatsRepository;
import org.excalibur.core.execution.domain.repository.TaskMemoryStatsRepository;
import org.excalibur.core.execution.domain.repository.TaskStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Optional;

import io.airlift.command.ProcessCpuState;
import io.airlift.command.ProcessMemoryState;

@RestController
@RequestMapping(value = "/dohko/v1/tasks")
public class JobRestController 
{
	private final TaskStatusRepository taskStatusRepository;
	private final TaskCpuStatsRepository taskCpuStatsRepository;
	private final TaskMemoryStatsRepository taskMemoryStatsRepository;
	

	@Autowired
	public JobRestController(TaskStatusRepository taskStatusRepository, TaskCpuStatsRepository taskCpuStatsRepository, TaskMemoryStatsRepository taskMemoryStatsRepository)
	{
		this.taskStatusRepository = taskStatusRepository;
		this.taskCpuStatsRepository = taskCpuStatsRepository;
		this.taskMemoryStatsRepository = taskMemoryStatsRepository;
	}
		
	@RequestMapping(value = "", 
			method = RequestMethod.POST,
			consumes = {"application/json"},
            produces = {"application/json"})
	@ResponseStatus(HttpStatus.CREATED)
	public ResponseEntity<String> deploy(@RequestBody ApplicationDescriptor descriptor)
	{
		return null;
	}
	
	@RequestMapping(value = "/{taskId}/status", 
			method = RequestMethod.GET, 
			produces = {"application/json"})
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody TaskStatus status(@PathVariable("taskId") final String taskId)
	{
		Optional<TaskStatus> status = taskStatusRepository.getLastStatusOfTask(taskId);
		return status.orNull();
	}
	
	@RequestMapping(value = "/{id}/stats", 
			method = RequestMethod.GET, 
			produces = {"application/json"})
	@ResponseStatus(HttpStatus.OK)
    public @ResponseBody TaskStats stats(@PathVariable("id") final String id)
    {
		List<ProcessCpuState> cpu = taskCpuStatsRepository.getStatsOfTask(id);
		List<ProcessMemoryState> mem = taskMemoryStatsRepository.getStatsOfTask(id);
		
    	return new TaskStats(id, cpu, mem);
    }
}
