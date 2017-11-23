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
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "artifact")
@XmlType(name = "artifact", propOrder = { "name", "version", "description", "source",  "checksum", "hashingMethod", "mountpoint", "action", "dependencies" })
public class Artifact implements Serializable, Cloneable 
{
	/**
	 * Serial code version <code>serialVersionUID</code> for serialization.
	 */
	private static final long serialVersionUID = 2464068403209895188L;

	@XmlElement(name="name", required = true)
	private String name;
	
	@XmlElement(name="version", required = true)
	private String version;
	
	@XmlElement(name="description")
	private String description;
	
	@XmlElement(name="checksum")
	private String checksum;
	
	@XmlElement(name = "checksum_type", defaultValue = "sha256")
	private HashingMethod hashingMethod = HashingMethod.SHA256;
	
	@XmlElement(name = "source")
	private Source source;
	
	@XmlElement(name="mountpoint")
	private Mountpoint mountpoint;
	
	@XmlElement(name="action", required = true)
	private Action action;
	
	@XmlElement(name="dependencies", required = true)
	private final List<String> dependencies = new ArrayList<>();
	
	
	public Artifact ()
	{
		super();
	}
	
	
	public Artifact(String name, String version)
	{
		this.name = name;
		this.version = version;
	}
	
	
	public Artifact addDependency(final String dependency)
	{
		if (dependency != null && !dependency.trim().isEmpty())
		{
			dependencies.add(dependency.trim());
		}
		
		return this;
	}
	
	public ImmutableList<String> dependencies()
	{
		return ImmutableList.copyOf(dependencies);
	}

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
	public Artifact setName(String name) 
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
	public Artifact setVersion(String version) 
	{
		this.version = version;
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
	public Artifact setDescription(String description) 
	{
		this.description = description;
		return this;
	}


	/**
	 * @return the checksum
	 */
	public String getChecksum() 
	{
		return checksum;
	}

	/**
	 * @param checksum the checksum to set
	 */
	public Artifact setChecksum(String checksum) 
	{
		this.checksum = checksum;
		return this;
	}
	

	/**
	 * @return the hashingMethod
	 */
	public HashingMethod getHashingMethod() 
	{
		return hashingMethod;
	}


	/**
	 * @param hashingMethod the hashingMethod to set
	 */
	public Artifact setHashingMethod(HashingMethod hashingMethod) 
	{
		this.hashingMethod = hashingMethod;
		
		return this;
	}


	/**
	 * @return the source
	 */
	public Source getSource() 
	{
		return source;
	}

	/**
	 * @param source the source to set
	 */
	public Artifact setSource(Source source) 
	{
		this.source = source;
		return this;
	}

	/**
	 * @return the mountpoint
	 */
	public Mountpoint getMountpoint() 
	{
		return mountpoint;
	}

	/**
	 * @param mountpoint the mountpoint to set
	 */
	public Artifact setMountpoint(Mountpoint mountpoint) 
	{
		this.mountpoint = mountpoint;
		return this;
	}

	/**
	 * @return the action
	 */
	public Action getAction() 
	{
		return action;
	}

	/**
	 * @param action the action to set
	 */
	public Artifact setAction(Action action) 
	{
		this.action = action;
		return this;
	}

	/**
	 * @return the dependencies
	 */
	public ImmutableList<String> getDependencies() 
	{
		return ImmutableList.copyOf(dependencies);
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
		
		Artifact other = (Artifact)obj;
		
		return Objects.equals(getName(), other.getName()) &&
			   Objects.equals(getVersion(), other.getVersion());
	}
	
	@Override
	public int hashCode() 
	{
		return Objects.hash(getName(), getVersion());
	}
	
	@Override
	public String toString() 
	{
		return MoreObjects.toStringHelper(this)
				.add("name", getName())
				.add("version", getVersion())
				.add("description", getDescription())
				.add("checksum", getChecksum())
				.add("checksum_type", getHashingMethod())
				.add("source", getSource())
				.add("action", getAction())
				.add("mountpoint", getMountpoint())
				.add("dependencies", "[" + Joiner.on(",").join(dependencies) + "]")
				.omitNullValues()
				.toString();
	}
	
	@Override
	protected Artifact clone() 
	{
		Artifact clone;
		
		try 
		{
			clone = (Artifact) super.clone();
		} 
		catch (CloneNotSupportedException e) 
		{
			clone = new Artifact()
					.setAction(getAction())
					.setChecksum(getChecksum())
					.setHashingMethod(getHashingMethod())
					.setDescription(getDescription())
					.setMountpoint(getMountpoint() != null ? getMountpoint().clone() : null)
					.setName(getName())
					.setSource(getSource() != null ? getSource().clone() : null)
					.setVersion(getVersion());
		}
		
		return clone;
	}

}
