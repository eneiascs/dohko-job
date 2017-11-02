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
package io.dohko.job.batch;

import java.util.concurrent.TimeUnit;

import org.excalibur.core.execution.domain.Application;

import com.google.common.base.Function;

import io.airlift.command.Command;

public class ConvertApplication2Command implements Function<Application, Command>, java.util.function.Function<Application, Command>
{
	@Override
	public Command apply(Application input) 
	{
		return new Command(input.getCommandLine()).setTimeLimit(input.getTimeout(), TimeUnit.MILLISECONDS);
	}
}
