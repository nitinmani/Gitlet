import java.io.Serializable;

/**
 * Represents a Branch.
 *
 */
public class Branch implements Serializable {
	private static final long serialVersionUID = 1L;

	private CommitTree parentTree = null; // Every branch has a reference to the
											// parent tree
	private String name = ""; // Name of the branch
	private CommitNode head = null;
	private CommitNode inProgressCommit = null; // The currently progressing
												// commit in this branch

	/**
	 * Construct a branch
	 */
	public Branch(String name, CommitTree parent, CommitNode headNode) {
		this.name = name;
		this.parentTree = parent;
		this.head = headNode;
	}

	/**
	 * Access name of branch
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Access head node of branch
	 */
	public CommitNode getHead() {
		return this.head;
	}

	/**
	 * Change the head of the branch to newHead
	 */
	public void setHead(CommitNode newHead) {
		this.head = newHead;
	}

	/**
	 * Print each commit's commitLog in branch
	 */
	public void printCommitLog() {
		CommitNode cnode = this.head;
		while (cnode != null) {
			cnode.printLog();
			System.out.println();
			cnode = cnode.getParent();
		}
	}

	/**
	 * Checkout from branch's head
	 */
	public boolean checkout() {
		return this.head.checkout();
	}

	/**
	 * Checkout from head with parameter fileName
	 */
	public boolean checkout(String fileName) {
		return this.head.checkout(fileName);
	}

	/**
	 * Check if the branch is in the path of the other branch If it is return
	 * true, else false
	 */
	public boolean inHistory(Branch otherBranch) {
		CommitNode head = this.getHead();
		CommitNode otherHead = otherBranch.getHead();
		while (head.getParent() != null) {
			if (head.getCommitId() == otherHead.getCommitId()) {
				return true;
			} else {
				head = head.getParent();
			}
		}
		return false;
	}

	/**
	 * Stages a file to a new Commit. Absolute path of the file is given.
	 * Precondition: The file with 'fileName' exists
	 */
	public void add(String fileName) {
		// If there is no inProgressCommit, initialize it.
		if (this.inProgressCommit == null) {
			// Create a new in progress commit node from the current branch's
			// head node
			this.inProgressCommit = new CommitNode(this.head,
					parentTree.getNextCommitId());
		}
		// Add this file
		this.inProgressCommit.addFile(fileName);
	}

	/**
	 * Saves the files.
	 */
	public void commit(String msg) {
		if (inProgressCommit == null) {
			System.out.println("No changes added to the commit.");
			return;
		}
		if (inProgressCommit.commitFiles(parentTree.getRepositoryParentDir(),
				msg, this.parentTree)) { // To generate unique file names
			// Move the head pointer in the current branch
			this.head = inProgressCommit;
			inProgressCommit = null;
			parentTree.recordCommit(this.head);
		}
	}

	/**
	 * remove file from inProgressCommit with that file
	 */
	public void remove(String fileName) {
		if (inProgressCommit == null) {
			// Create a new in progress commit node from the current branch's
			// head node
			this.inProgressCommit = new CommitNode(this.head,
					parentTree.getNextCommitId());
		}
		// Remove this file
		this.inProgressCommit.removeFile(fileName);
	}

	/**
	 * print files that are to be added
	 */
	public void printStagedFiles() {
		if (this.inProgressCommit != null) {
			this.inProgressCommit.printFilesToAdd();
		}
	}

	/**
	 * print files that are to be removed
	 */
	public void showFilesMarkedForRemoval() {
		if (this.inProgressCommit != null) {
			this.inProgressCommit.printFilesToRemove();
		}
	}
}
