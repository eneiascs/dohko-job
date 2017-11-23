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

import com.google.common.collect.ImmutableList;

import io.airlift.command.Command;
import io.airlift.command.CommandFailedException;
import io.airlift.command.CommandResult;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.exec.OS;

public enum PackageManagerType 
{
	YUM("yum")
	{
		public String install(ImmutableList<Package> packages)
		{
			return doInstall("yum", packages);
		}
	},
	
	APT("apt-get") 
	{
		public String install(ImmutableList<Package> packages)
		{
			return doInstall("apt-get", packages);
		}
	}, 
	
	BREW("brew")
	{
		public String install(ImmutableList<Package> packages)
		{
			return doInstall("apt-get", packages);
		}
	};
	
	
	
	private String command;

	public abstract String install(ImmutableList<Package> packages);
	
	private PackageManagerType(String command)
	{
		this.command = command;
	}
	
	private static String doInstall(String name, List<Package> packages)
	{
		final StringBuilder result = new StringBuilder();
		
		if (packages != null)
		{
			try 
			{
				CommandResult commandResult = command(packages).execute(Executors.newSingleThreadExecutor());
				result.append(commandResult.getCommandOutput());
				
			} catch (CommandFailedException cfe) 
			{
				result.append(cfe.getOutput()).append("\n").append(cfe.getMessage());
			}
		}
		
		return result.toString();
	} 

	public static Command command(final List<Package> packages) 
	{
		PackageManagerType type = getOSPackageManager();
		
		StringBuilder commandline = new StringBuilder(format("%s install -y\n", requireNonNull(type.command)));
		
		packages.forEach(p -> 
		{
			commandline.append(format("%s\\\n", p.getName()));
		});
		
		return new Command("bash", "-c", commandline.toString())
				   .setTimeLimit(1, TimeUnit.HOURS);
	}

	private static PackageManagerType getOSPackageManager() 
	{
		if (OS.isFamilyMac())
		{
			return PackageManagerType.BREW;
		} 
		else if (OS.isFamilyUnix())
		{
			// is it redhat? or debian-based ?
			
			return (org.jsoftbiz.utils.OS.getOs().getPlatformName().toLowerCase().contains("centos") || org.jsoftbiz.utils.OS.getOs().getPlatformName().toLowerCase().contains("redhat")) 
					? PackageManagerType.YUM 
					: PackageManagerType.APT;
		}
		
		return PackageManagerType.YUM;
	}
}
