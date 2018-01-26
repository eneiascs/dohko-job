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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Tree<T> 
{
    private TreeNode<T> root;

    public Tree() 
    {
        super();
    }
    
    public Tree(TreeNode<T> root)
    {
    	this.root = root;
    }

    public TreeNode<T> getRoot() 
    {
        return this.root;
    }
    
    public TreeNode<T> root()
    {
    	return this.root;
    }

    public Tree<T> setRoot(TreeNode<T> root) 
    {
        this.root = root;
        
        return this;
    }

    public int getNumberOfNodes() 
    {
        int numberOfNodes = 0;

		if (root != null) 
        {
            numberOfNodes = auxiliaryGetNumberOfNodes(root) + 1; //1 for the root!
        }

        return numberOfNodes;
    }

    private int auxiliaryGetNumberOfNodes(TreeNode<T> node) 
    {
        int numberOfNodes = node.getNumberOfChildren();

        for (TreeNode<T> child : node.getChildren()) 
        {
            numberOfNodes += auxiliaryGetNumberOfNodes(child);
        }

        return numberOfNodes;
    }

    public boolean exists(T dataToFind) 
    {
		return find(dataToFind) != null;
    }

    public TreeNode<T> find(T dataToFind) 
    {
        TreeNode<T> returnNode = null;

        if(root != null) 
        {
            returnNode = auxiliaryFind(root, dataToFind);
        }

        return returnNode;
    }

    private TreeNode<T> auxiliaryFind(TreeNode<T> currentNode, T dataToFind) 
    {
        TreeNode<T> returnNode = null;
        int i = 0;

		if (currentNode.getData().equals(dataToFind)) 
        {
            returnNode = currentNode;
        }

        else if(currentNode.hasChildren()) 
        {
            i = 0;
            
			while (returnNode == null && i < currentNode.getNumberOfChildren()) 
			{
                returnNode = auxiliaryFind(currentNode.getChildAt(i), dataToFind);
                i++;
            }
        }

        return returnNode;
    }

    public boolean isEmpty() 
    {
        return root == null;
    }

    public List<TreeNode<T>> build(TreeTraversalOrderType traversalOrder) 
    {
        List<TreeNode<T>> returnList = null;
        
		if (root != null) 
        {
			returnList = build(root, traversalOrder);
        }

        return returnList;
    }

    public List<TreeNode<T>> build(TreeNode<T> node, TreeTraversalOrderType traversalOrder) 
    {
        List<TreeNode<T>> traversalResult = new ArrayList<TreeNode<T>>();
        
		switch (traversalOrder) 
		{
		
		case PRE_ORDER:
			buildPreOrder(node, traversalResult);
			break;
		case POST_ORDER:
			buildPostOrder(node, traversalResult);
			break;
		default:
			throw new IllegalStateException();
		}

        return traversalResult;
    }

    private void buildPreOrder(TreeNode<T> node, List<TreeNode<T>> traversalResult) 
    {
        traversalResult.add(node);

		for (TreeNode<T> child : node.getChildren()) 
        {
            buildPreOrder(child, traversalResult);
        }
    }

    private void buildPostOrder(TreeNode<T> node, List<TreeNode<T>> traversalResult) 
    {
		for (TreeNode<T> child : node.getChildren()) 
        {
            buildPostOrder(child, traversalResult);
        }

        traversalResult.add(node);
    }

    public Map<TreeNode<T>, Integer> buildWithDepth(TreeTraversalOrderType traversalOrder) 
    {
        Map<TreeNode<T>, Integer> returnMap = null;

		if (root != null) 
        {
            returnMap = buildWithDepth(root, traversalOrder);
        }

        return returnMap;
    }

    public Map<TreeNode<T>, Integer> buildWithDepth(TreeNode<T> node, TreeTraversalOrderType traversalOrder) 
    {
        Map<TreeNode<T>, Integer> traversalResult = new LinkedHashMap<TreeNode<T>, Integer>();

		if (traversalOrder == TreeTraversalOrderType.PRE_ORDER) 
        {
            buildPreOrderWithDepth(node, traversalResult, 0);
        }
		else if (traversalOrder == TreeTraversalOrderType.POST_ORDER) 
        {
            buildPostOrderWithDepth(node, traversalResult, 0);
        }

        return traversalResult;
    }

    private void buildPreOrderWithDepth(TreeNode<T> node, Map<TreeNode<T>, Integer> traversalResult, int depth) 
    {
        traversalResult.put(node, depth);

		for (TreeNode<T> child : node.getChildren()) 
		{
            buildPreOrderWithDepth(child, traversalResult, depth + 1);
        }
    }

    private void buildPostOrderWithDepth(TreeNode<T> node, Map<TreeNode<T>, Integer> traversalResult, int depth) 
    {
		for (TreeNode<T> child : node.getChildren()) 
		{
            buildPostOrderWithDepth(child, traversalResult, depth + 1);
        }

        traversalResult.put(node, depth);
    }

    public String toString() 
    {
        /*
        We're going to assume a pre-order traversal by default
         */

        String stringRepresentation = "";

        if(root != null) 
        {
            stringRepresentation = build(TreeTraversalOrderType.PRE_ORDER).toString();
        }

        return stringRepresentation;
    }

    public String toStringWithDepth() 
    {
        /*
        We're going to assume a pre-order traversal by default
         */

        String stringRepresentation = "";

        if(root != null) 
        {
            stringRepresentation = buildWithDepth(TreeTraversalOrderType.PRE_ORDER).toString();
        }

        return stringRepresentation;
    }
}
