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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.collect.ImmutableList;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "package")
@XmlType(name = "package", propOrder = { "name", "version", "architecture", "platform", "description", "dependencies" })
public class Package implements Serializable, Cloneable 
{
	/**
	 * Serial code version <code>serialVersionUID</code> for serialization. 
	 */
	private static final long serialVersionUID = 1764614527140465419L;
	
	@XmlElement(name = "name", required = true)
	private String name;
	
	@XmlElement(name = "version", required = true)
	private String version;
	
	@XmlElement(name = "architecture")
	private String architecture;
	
	@XmlElement(name = "description")
	private String description;
	
	@XmlElement(name = "platform")
	private Platform platform;

	private final List<Package> dependencies = new ArrayList<>();

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
	public Package setName(String name) 
	{
		this.name = name;
		return this;
	}

	/**
	 * @return the version
	 */
	public String getVersion() 
	{
		return version;
	}

	/**
	 * @param version the version to set
	 */
	public Package setVersion(String version) 
	{
		this.version = version;
		return this;
	}

	/**
	 * @return the architecture
	 */
	public String getArchitecture() 
	{
		return architecture;
	}

	/**
	 * @param architecture the architecture to set
	 */
	public Package setArchitecture(String architecture) 
	{
		this.architecture = architecture;
		return this;
	}

	/**
	 * @return the platform
	 */
	public Platform getPlatform() 
	{
		return platform;
	}

	/**
	 * @param platform the platform to set
	 */
	public Package setPlatform(Platform platform) 
	{
		this.platform = platform;
		return this;
	}

	/**
	 * @return the description
	 */
	public String getDescription() 
	{
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public Package setDescription(String description) 
	{
		this.description = description;
		return this;
	}

	/**
	 * @return the dependencies
	 */
	public ImmutableList<Package> getDependencies() 
	{
		return ImmutableList.copyOf(dependencies);
	}
}
