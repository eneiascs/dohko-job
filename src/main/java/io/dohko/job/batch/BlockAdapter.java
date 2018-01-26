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


import org.excalibur.core.execution.domain.Block;

import io.dohko.job.batch.tree.Tree;
import job.flow.Step;

import static java.util.Objects.*;

public class BlockAdapter 
{
	private final Block block;
	private final Tree<Step> applicationTree;
	
	public BlockAdapter(Block block, Tree<Step> applicationTree) 
	{
		this.block = requireNonNull(block);
		this.applicationTree = requireNonNull(applicationTree);
	}
	
	/**
	 * @return the block
	 */
	public Block getBlock() 
	{
		return block;
	}

	/**
	 * @return the applicationTree
	 */
	public Tree<Step> getApplicationTree() 
	{
		return applicationTree;
	}
}
