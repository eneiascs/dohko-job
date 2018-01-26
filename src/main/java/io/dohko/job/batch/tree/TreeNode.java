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
package io.dohko.job.batch.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;

public class TreeNode<T> 
{
    private T data;
    private List<TreeNode<T>> children;
    private TreeNode<T> parent;

    public TreeNode() 
    {
        super();
        children = new ArrayList<TreeNode<T>>();
    }

    public TreeNode(T data) 
    {
        this();
        setData(data);
    }

    public TreeNode<T> getParent() 
    {
        return this.parent;
    }

    public ImmutableList<TreeNode<T>> getChildren() 
    {
        return ImmutableList.copyOf(this.children);
    }
    
    public ImmutableList<TreeNode<T>> children()
    {
    	return getChildren();
    }

    public int getNumberOfChildren() 
    {
        return getChildren().size();
    }

    public boolean hasChildren() 
    {
        return getNumberOfChildren() > 0;
    }

    public void setChildren(List<TreeNode<T>> children) 
    {
        for(TreeNode<T> child : children) 
        {
           child.parent = this;
        }

        this.children = children;
    }

    public TreeNode<T> addChild(TreeNode<T> child) 
    {
        child.parent = this;
        children.add(child);
        
        return this;
    }
    
    public TreeNode<T> addChildren(Iterable<TreeNode<T>> children) 
    {
    	children.forEach(this::addChild);
    	return this;
	}

    public void addChildAt(int index, TreeNode<T> child) throws IndexOutOfBoundsException 
    {
        child.parent = this;
        children.add(index, child);
    }

    public void removeChildren() 
    {
        this.children = new ArrayList<TreeNode<T>>();
    }

    public void removeChildAt(int index) throws IndexOutOfBoundsException 
    {
        children.remove(index);
    }

    public TreeNode<T> getChildAt(int index) throws IndexOutOfBoundsException 
    {
        return children.get(index);
    }

    public T getData() 
    {
        return this.data;
    }

    public void setData(T data) 
    {
        this.data = data;
    }

    public String toString() 
    {
        return getData().toString();
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
        
        TreeNode<?> other = (TreeNode<?>) obj;
        return Objects.equals(data, other.data);
    }

    @Override
    public int hashCode() 
    {
    	return Objects.hash(data);
    }

    public String toStringVerbose() 
    {
        String stringRepresentation = getData().toString() + ":[";

        for (TreeNode<T> node : getChildren()) 
        {
            stringRepresentation += node.getData().toString() + ", ";
        }

        //Pattern.DOTALL causes ^ and $ to match. Otherwise it won't. It's retarded.
        Pattern pattern = Pattern.compile(", $", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(stringRepresentation);

        stringRepresentation = matcher.replaceFirst("");
        stringRepresentation += "]";

        return stringRepresentation;
    }

	
}

