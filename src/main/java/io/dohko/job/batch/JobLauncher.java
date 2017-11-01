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

import org.springframework.batch.core.JobParameters;

import com.google.common.base.Optional;

import job.flow.Job;

public interface JobLauncher 
{
	/**
	 * Registers a listener to receive events. All subscriber methods on {@code listener}
	 * are registered.
	 * @param listener listener whose subscriber methods should be registered.
	 * @param <T> the type of the listener
	 */
	<T> void registerListener(T listener);


	Optional<JobExecution> run(Job job, JobParameters parameters);
}
