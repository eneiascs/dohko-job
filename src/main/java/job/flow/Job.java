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

import com.google.common.collect.ImmutableList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class Job 
{
	private final Map<String, Flow> flows = new LinkedHashMap<>();
	private final String name;
	
	public Job(String name)
	{
		this.name = requireNonNull(name);
	}
	
	public Job(String name, List<Flow> flows)
	{
		this(name);
		
		if (flows != null)
		{
			flows.forEach(this::add);
		}
	}
	
	public Job add(Flow flow)
	{
		if (flow != null)
		{
			flows.put(flow.getName(), flow);
		}
		
		return this;
	}
	
	public ImmutableList<Flow> flows()
	{
		return ImmutableList.copyOf(flows.values());
	}
	
	public Optional<Flow> getFlow(String name)
	{
		return Optional.of(flows.get(name));
	}

	/**
	 * @return the name
	 */
	public String getName() 
	{
		return name;
	}
}
