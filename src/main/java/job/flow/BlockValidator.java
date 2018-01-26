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
package job.flow;

import org.excalibur.core.deployment.validation.ValidationContext;
import org.excalibur.core.execution.domain.ApplicationDescriptor;
import org.excalibur.core.execution.domain.Block;
import org.excalibur.core.validator.ValidationResult;
import org.excalibur.core.validator.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.String.*;
import static org.excalibur.core.deployment.validation.ValidationContext.*;

public class BlockValidator implements Validator<ApplicationDescriptor, ValidationResult<ValidationContext>> 
{
	@Override
	public ValidationResult<ValidationContext> validate(ApplicationDescriptor descriptor) 
	{
		final ValidationResult<ValidationContext> result = new ValidationResult<ValidationContext>(new ValidationContext());
		result.get().put("blocks", descriptor.getBlocksMap());
		
		List<String> ids = new ArrayList<>();
		
		descriptor.getBlocks().forEach(b -> 
		{
			if (ids.contains(b.id()))
			{
				result.get().addError(format("Duplicated block's id %s", b.id()));
			} 
			else
			{
				ids.add(b.id());
			}
			
			validateParents(result.get(), b);
		});
		
		return result;
	}

	@SuppressWarnings("unchecked")
	private void validateParents(final ValidationContext context, final Block block) 
	{
		if (isRegistered(block))
		{
			context.cyclic();
			return;
		}
		
		register(block);
		
		try
		{
			block.getParents().forEach(p -> 
			{
				Block parent = ((Map<String, Block>) context.getData("blocks")).get(p);
				
				if (parent != null)
				{
					validateParents(context, parent);
				} 
				else
				{
					context.addError(format("Unknown parent %s of block %s", p, block.getId()));
				}
			});
			
		}
		finally
		{
			unregister(block);
		}
	}
}
