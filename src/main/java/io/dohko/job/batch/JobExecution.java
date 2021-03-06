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

import java.util.Objects;

import job.flow.Job;

public class JobExecution 
{
	private final Job job;
	private long elapsedTime;
	
	public JobExecution(Job job)
	{
		this.job = Objects.requireNonNull(job, () -> "job is null");
	}
	
	public JobExecution(Job job, long elapsedTime) 
	{
		this(job);
		this.elapsedTime = elapsedTime;
	}

	/**
	 * @return the elapsedTime
	 */
	public long getElapsedTime() 
	{
		return elapsedTime;
	}

	/**
	 * @param elapsedTime the elapsedTime to set
	 */
	public JobExecution setElapsedTime(long elapsedTime) 
	{
		this.elapsedTime = elapsedTime;
		return this;
	}

	/**
	 * @return the job
	 */
	public Job getJob() 
	{
		return job;
	}
}
