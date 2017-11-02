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
import java.util.List;
import java.util.Objects;

import javax.annotation.concurrent.Immutable;

import org.excalibur.core.execution.domain.TaskStats;

import com.google.common.base.MoreObjects;

import io.airlift.command.CommandResult;
import io.airlift.command.ProcessCpuState;
import io.airlift.command.ProcessMemoryState;

@Immutable
public class TaskExecutionResult 
{
	private final String id;
	private final CommandResult result;
	
	public TaskExecutionResult(String id, CommandResult result)
	{
		this.id = Objects.requireNonNull(id, "task's id is null");
		this.result = Objects.requireNonNull(result, "task's result is null");
	}
	
	/**
	 * @return the id
	 */
	public String getId() 
	{
		return id;
	}
	
	/**
	 * @return the result
	 */
	public CommandResult getResult() 
	{
		return result;
	}
	
	public TaskStats stats()
	{
		List<ProcessCpuState> cpus = new ArrayList<>();
		List<ProcessMemoryState> mems = new ArrayList<>();
		
		result.getProcessStats().forEach(s -> 
		{
			cpus.add(s.getCpuState());
			mems.add(s.getMemoryState());
		});
		
		return new TaskStats(id, cpus, mems);
	}
	
	
	@Override
	public boolean equals(Object obj) 
	{
		if (this == obj)
		{
			return true;
		}
		
		if (obj == null || getClass() != obj.getClass())
		{
			return false;
		}
		
		TaskExecutionResult other = (TaskExecutionResult) obj;
		return Objects.equals(getId(), other.getId());
	}
	
	@Override
	public int hashCode() 
	{
		return Objects.hashCode(getId());
	}
	
	@Override
	public String toString() 
	{
		return MoreObjects.toStringHelper(this)
				.add("id", getId())
				.add("exitCode", result.getExitCode())
				.add("output", result.getCommandOutput())
				.omitNullValues()
				.toString();
	}

	public int getExitCode() 
	{
		return result.getExitCode();
	}
	
	public String getOutput()
	{
		return this.result.getCommandOutput();
	}
}
