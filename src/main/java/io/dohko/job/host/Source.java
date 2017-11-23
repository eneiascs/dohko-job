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

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "source")
@XmlType(name = "source", propOrder = { "url", "params" })
public class Source implements Serializable, Cloneable
{
	/**
	 * Serial code version <code>serialVersionUID</code> for serialization.
	 */
	private static final long serialVersionUID = 6656912050569646239L;

	@XmlElement(name = "url", required = true, nillable = false)
	private String url;
	
	@XmlElement(name = "params")
	private final List<String> params = new ArrayList<>();
	
	public Source() 
	{
		super();
	}
	
	public Source addParam(String param)
	{
		if (!Strings.isNullOrEmpty(param))
		{
			params.add(param);
		}
		
		return this;
	}
	
	
	/**
	 * @return the url
	 */
	public String getUrl() 
	{
		return url;
	}



	/**
	 * @param url the url to set
	 */
	public Source setUrl(String url) 
	{
		this.url = url;
		return this;
	}


	/**
	 * @return the params
	 */
	public ImmutableList<String> getParams() 
	{
		return ImmutableList.copyOf(params);
	}
	
	public ImmutableList<String> params()
	{
		return getParams();
	}


	@Override
	public Source clone() 
	{
		Source clone;
		
		try 
		{
			clone = (Source) super.clone();
		} 
		catch (CloneNotSupportedException e) 
		{
			clone = new Source().setUrl(getUrl());
			getParams().forEach(clone::addParam);
		}
		
		return clone;
	}
	
	@Override
	public String toString() 
	{
		return MoreObjects.toStringHelper(this)
				.add("url", getUrl())
				.add("params", "[" + Joiner.on(",").join(params()) + "]")
				.omitNullValues()
				.toString();
	}
}
