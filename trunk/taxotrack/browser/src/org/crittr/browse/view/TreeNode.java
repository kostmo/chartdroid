package org.crittr.browse.view;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class TreeNode implements Iterable<TreeNode> {

	private List<TreeNode> children;

	public TreeNode() {
		children = new ArrayList<TreeNode>();
	}

	public List<TreeNode> getChildren() {
		return children;
	}
	
	public int getChildCount() {
		return children.size();
	}
	
	public boolean addChild(TreeNode n) {
		return children.add(n);
	}

	public boolean removeChild(TreeNode n) {
		return children.remove(n);
	}

	public Iterator<TreeNode> iterator() {
		return children.iterator();
	}
}
