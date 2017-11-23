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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.MoreObjects;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "mountpoint")
@XmlType(name = "mountpoint", propOrder = { "location", "exportName", "sudo" })
public class Mountpoint implements Cloneable, Serializable
{
	/**
	 * Serial code version <code>serialVersionUID</code> for serialization.
	 */
	private static final long serialVersionUID = 5355954737370157997L;

	@XmlElement(name = "location", required = true, nillable = false)
	private String location;
	
	@XmlElement(name = "export_name")
	private String exportName;
	
	@XmlElement(name = "sudo")
	private boolean sudo;
	
	
	/**
	 * @return the location
	 */
	public String getLocation() 
	{
		return location;
	}


	/**
	 * @param location the location to set
	 */
	public Mountpoint setLocation(String location) 
	{
		this.location = location;
		return this;
	}

	/**
	 * @return the exportName
	 */
	public String getExportName() 
	{
		return exportName;
	}


	/**
	 * @param exportName the exportName to set
	 */
	public Mountpoint setExportName(String exportName) 
	{
		this.exportName = exportName;
		return this;
	}


	/**
	 * @return the sudo
	 */
	public boolean isSudo() 
	{
		return sudo;
	}


	/**
	 * @param sudo the sudo to set
	 */
	public Mountpoint setSudo(boolean sudo) 
	{
		this.sudo = sudo;
		return this;
	}

	
	@Override
	public String toString() 
	{
		return MoreObjects.toStringHelper(this)
				.add("location", getLocation())
				.add("export_name", getExportName())
				.add("sudo", isSudo())
				.omitNullValues()
				.toString();
	}


	@Override
	public Mountpoint clone() 
	{
		Mountpoint clone;
		
		try 
		{
			clone = (Mountpoint) super.clone();
		} 
		catch (CloneNotSupportedException e) 
		{
			clone = new Mountpoint()
					.setExportName(getExportName())
					.setLocation(getLocation())
					.setSudo(isSudo());
		}
		
		return clone;
	}

}
