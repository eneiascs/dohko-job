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

import static java.util.Objects.requireNonNull;

import java.util.concurrent.Executor;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.MoreObjects;

import io.airlift.command.Command;
import io.airlift.command.CommandFailedException;
import io.airlift.command.CommandResult;

@Immutable
public class Step 
{
	private final String id;
	private final String name;
	private final Command action;
	
	public Step(String id, String name, Command action)
	{
		this.id = requireNonNull(id, "Steps's id is null");
		this.name = requireNonNull(name, "Step's name is null");
		this.action = action;
	}

	/**
	 * @return the name
	 */
	public String getName() 
	{
		return name;
	}
	
	public String name()
	{
		return this.getName();
	}
	
	/**
	 * @return the id
	 */
	public String getId() 
	{
		return id;
	}
	
	/**
	 * @return the id
	 */
	public String id() 
	{
		return getId();
	}


	public CommandResult execute(Executor executor) throws CommandFailedException
	{
		return action.execute(executor);
	}
	
	@Override
	public String toString() 
	{
		return MoreObjects.toStringHelper(this)
				.add("name", getName())
				.add("command", action)
				.omitNullValues()
				.toString();
	}
}
