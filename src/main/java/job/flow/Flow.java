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
package job.flow;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

public class Flow
{
	private final Map<String, Step> steps = new LinkedHashMap<>();
	private final String name;
	
	public Flow(String name)
	{
		this.name = name;
	}
	
	public Flow add(Step step)
	{
		if (step != null)
		{
			steps.put(step.getName(), step);
		}
		
		return this;
	}

	/**
	 * @return the steps
	 */
	public ImmutableList<Step> steps() 
	{
		return ImmutableList.copyOf(steps.values());
	}

	/**
	 * @return the name
	 */
	public String getName() 
	{
		return name;
	}
	
	@Override
	public String toString() 
	{
		return MoreObjects.toStringHelper(this)
				.add("name", getName())
				.add("nr. steps", steps.size())
				.omitNullValues()
				.toString();
	}
}
