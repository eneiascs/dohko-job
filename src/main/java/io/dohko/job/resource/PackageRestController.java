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
package io.dohko.job.resource;

import org.excalibur.core.host.repository.PackageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import io.dohko.job.host.Package;

@RestController
@RequestMapping(value = "/{username}/packages")
@Api(value = "packages", tags = "Packages Management API")
public class PackageRestController 
{	
	@Autowired
	private PackageRepository packageRepository;
	
	@RequestMapping(method = RequestMethod.POST, produces = {"application/json"})
	@ResponseStatus(HttpStatus.CREATED)
	@ApiOperation(value = "Add a new package in the knowledge base", response = io.dohko.job.host.Package.class)
	public @ResponseBody void add(@PathVariable("username") String user, @RequestBody Package pkg)
	{
		packageRepository.insert(pkg);
	}
	
	@RequestMapping(value = "{name}", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Returns the package with the given name", response = io.dohko.job.host.Package.class)
	public @ResponseBody io.dohko.job.host.Package get(@PathVariable("username") String user, @PathVariable("name") String name)
	{
		return packageRepository.findByName(name).orNull();
	}
}
