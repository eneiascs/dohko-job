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
package io.dohko.job.host;

import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.MoreObjects;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="platform")
@XmlType(name = "platform", propOrder = { "name", "runtime", "buildtime" })
public class Platform 
{
	private String name;
	private String runtime;
	private String buildtime;
	/**
	 * @return the name
	 */
	public String getName() 
	{
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public Platform setName(String name) 
	{
		this.name = name;
		return this;
	}
	/**
	 * @return the runtime
	 */
	public String getRuntime() 
	{
		return runtime;
	}
	/**
	 * @param runtime the runtime to set
	 */
	public Platform setRuntime(String runtime) 
	{
		this.runtime = runtime;
		return this;
	}
	/**
	 * @return the buildtime
	 */
	public String getBuildtime() 
	{
		return buildtime;
	}
	/**
	 * @param buildtime the buildtime to set
	 */
	public Platform setBuildtime(String buildtime) 
	{
		this.buildtime = buildtime;
		return this;
	}
	
	
	@Override
	public String toString() 
	{
		return MoreObjects.toStringHelper(this)
				.add("name", getName())
				.add("buildtime", getBuildtime())
				.add("runtime", getRuntime())
				.omitNullValues()
				.toString();
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
		
		Platform other = (Platform) obj;
		
		return Objects.equals(getName(), other.getName()) && 
			   Objects.equals(getBuildtime(), other.getBuildtime()) &&
			   Objects.equals(getRuntime(), other.getRuntime());
	}
	
	@Override
	public int hashCode() 
	{
		return Objects.hash(getName(), getBuildtime(), getBuildtime());
	}
	
	@Override
	protected Platform clone() 
	{
		Platform clone;
		
		try 
		{
			clone = (Platform) super.clone();
		} 
		catch (CloneNotSupportedException e) 
		{
			clone = new Platform().setBuildtime(getBuildtime()).setName(getName()).setRuntime(getRuntime());
		}
		
		return clone;
	}

}
