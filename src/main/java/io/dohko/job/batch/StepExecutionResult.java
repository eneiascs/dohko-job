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

import io.airlift.command.CommandFailedException;
import io.airlift.command.CommandResult;
import job.flow.Step;

import static java.util.Objects.requireNonNull;

public class StepExecutionResult 
{
	private final Step step;
	private CommandResult result;
	private CommandFailedException exception;

	public StepExecutionResult(Step step) 
	{
		this.step = requireNonNull(step, "step is null");
	}

	public Step step() 
	{
		return step;
	}
	
	public boolean isSuccessfully()
	{
		return result != null && exception == null;
	}

	public StepExecutionResult setResult(CommandResult result) 
	{
		this.result = result;
		return this;
	}

	public StepExecutionResult setException(CommandFailedException cfe) 
	{
		this.exception = cfe;
		return this;
	}

	/**
	 * @return the result
	 */
	public CommandResult getResult() 
	{
		return result;
	}

	/**
	 * @return the exception
	 */
	public CommandFailedException getException() 
	{
		return exception;
	}
}