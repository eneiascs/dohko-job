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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableList;

import job.flow.Flow;

public class FlowExecutionResult 
{
	private final Map<String, StepExecutionResult> results = new LinkedHashMap<>();
	private final Flow flow;
	
	private boolean wasCancelled;
	
	public FlowExecutionResult(Flow flow)
	{
		this.flow = Objects.requireNonNull(flow, "flow is null");
	}

	public FlowExecutionResult addStepResult(StepExecutionResult result) 
	{
		results.put(result.step().name(), result);
		return this;
	}

	public ImmutableList<StepExecutionResult> stepsResults() 
	{
		return ImmutableList.copyOf(results.values());
	}

	public FlowExecutionResult setWasCancelled(boolean value) 
	{
		this.wasCancelled = value;
		return this;
	}

	/**
	 * @return <code>true</code> if the execution of the flow was cancelled; otherwise returns <code>false</code>.
	 */
	public boolean wasCancelled() 
	{
		return wasCancelled;
	}

	/**
	 * @return the flow
	 */
	public Flow flow() 
	{
		return flow;
	}
}