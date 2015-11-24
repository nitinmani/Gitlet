import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeSet;

/**
 * This class represents a CommitNode.
 *
 */
public class CommitNode implements Serializable {
	private static final long serialVersionUID = 1L;
	private long id; // Commit id
	private String msg = ""; // Commit message
	private Date time; // Commit date and time

	// To store inherited Files
	private ArrayList<StoredFile> oldFiles = new ArrayList<StoredFile>();
	private ArrayList<StoredFile> addedFiles = new ArrayList<StoredFile>();
	private ArrayList<String> deletedFiles = new ArrayList<String>();
	// HashMap of <absolute file name, user typed file name>
	private HashMap<String, String> toAdd = new HashMap<String, String>();
	// HashMap of <absolute file name, user typed file name>
	private HashMap<String, String> toRemove = new HashMap<String, String>();
	private CommitNode parent; // Parent of this CommitNode

	private static final SimpleDateFormat dtFormat = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	/**
	 * Construct CommitNode
	 */
	public CommitNode(long commitId) {
		this.id = commitId;
		this.parent = null;
		this.time = new Date();
	}

	/**
	 * Construct commitNode with given parent and commitID
	 */
	public CommitNode(CommitNode parentCommit, long commitId) {
		this(commitId); // Initialize the CommitNode
		this.parent = parentCommit;

		// Copy the content from parent
		if (parentCommit != null) {
			// Copy the "oldFiles" and the "addedFiles" from parent to node.
			this.oldFiles.addAll(parentCommit.oldFiles);
			// Add/replace all added files to the old files list from previous
			// commit
			for (StoredFile added : parentCommit.addedFiles) {
				int pos = StoredFile.getStoredFile(this.oldFiles,
						added.getOriginalFileName());
				if (pos != -1) {
					this.oldFiles.set(pos, added);
				} else {
					this.oldFiles.add(added);
				}
			}
			// Delete removed files from the old files list of prev commit
			for (String deleted : parentCommit.deletedFiles) {
				int pos = StoredFile.getStoredFile(this.oldFiles, deleted);
				if (pos != -1) {
					this.oldFiles.remove(pos);
				}
			}
		}
	}

	/**
	 * Access oldFiles
	 */
	public ArrayList<StoredFile> getOldFiles() {
		return oldFiles;
	}

	/**
	 * Access addedFiles
	 */
	public ArrayList<StoredFile> getAddedFiles() {
		return addedFiles;
	}

	/**
	 * Access id of commitNode
	 */
	public long getCommitId() {
		return this.id;
	}

	/**
	 * Change commit message of commitNode
	 */
	public void setCommitMessage(String newMsg) {
		this.msg = newMsg;
	}

	/**
	 * Access commit message of commitNode
	 */
	public String getCommitMessage() {
		return this.msg;
	}

	/**
	 * Access time of commit
	 */
	public Date getCommitTime() {
		return this.time;
	}

	/**
	 * Access parent of commit
	 */
	public CommitNode getParent() {
		return this.parent;
	}

	/**
	 * Change parent of commit to given commitNode
	 */
	public void setParent(CommitNode c) {
		this.parent = c;
	}

	/**
	 * Documentation is provided on a step-by-step basis within the method
	 */
	public void addFile(String fileToAdd) {
		File theNewFile = new File(fileToAdd);
		// Delete this file from 'to remove' list, if exists
		this.toRemove.remove(theNewFile.getAbsolutePath());

		// Check if the file is modified after it was last committed in the
		// repository (multiple steps)
		// Step 1. Get the old file details.
		// (All previously committed files are in the oldFiles array)
		int pos = StoredFile.getStoredFile(this.oldFiles,
				theNewFile.getAbsolutePath());
		// Search in the "oldFiles" list
		/**
		 * Step 2. If the file exists in the commit tree, compare the committed
		 * file timestamp with the new file timestamp in the working directory.
		 */
		if ((pos != -1)
				&& (theNewFile.lastModified() == this.oldFiles.get(pos)
						.getLastModified())) {
			// 3. If they match, the file is not modified
			// display an error.
			System.out
					.println("File has not been modified since the last commit.");
		} else {
			// This file is to be staged for addition
			this.toAdd.put(theNewFile.getAbsolutePath(), fileToAdd);
			// Full file path is stored as key
		}
	}

	/**
	 * Commit files. If they are in toAdd, put them in addedFiles Else, if they
	 * are in toRemove, put them in deletedFiles Then, set commit message
	 */
	public boolean commitFiles(String repoLoc, String commitMsg,
			FileNameGenerator fng) {
		if ((toAdd.size() == 0) && (toRemove.size() == 0)) {
			System.out.println("No changes added to the commit.");
			return false;
		}
		// Copy each file staged in 'toAdd' list
		for (String origFileName : toAdd.keySet()) {
			StoredFile fInfo = new StoredFile(origFileName);
			// Full file path is stored in StoredFile
			if (fInfo.storeFile(repoLoc, this.id, fng)) {
				this.addedFiles.add(fInfo);
			}
		}
		// Delete each file staged in 'toRemove' list
		for (String origFileName : toRemove.keySet()) {
			this.deletedFiles.add(origFileName);
		}
		// Set the commit message
		this.msg = commitMsg;
		this.time = new Date();
		return true;
	}

	/**
	 * Removes an old file. Also removes it from staging list, if it is staged.
	 */
	public void removeFile(String fileToRemove) {
		File theFile = new File(fileToRemove);
		// Does it exist in oldFiles list?
		int pos = StoredFile.getStoredFile(this.oldFiles,
				theFile.getAbsolutePath());
		if (pos != -1) {
			this.toRemove.put(theFile.getAbsolutePath(), fileToRemove);
			// Full file path is stored as key
		} 
		// If it is staged for addition, remove from the list
		this.toRemove.remove(fileToRemove);
	}

	/**
	 * Print id, time, and message of commit
	 */
	public void printLog() {
		System.out.println("====");
		System.out.println("Commit " + this.id + ".");
		System.out.println(dtFormat.format(this.time));
		System.out.println(this.msg);
	}

	/**
	 * Set time to be time given in date
	 */
	public void setTime(Date date) {
		this.time = date;
	}

	/**
	 * Print all files in toAdd
	 */
	public void printFilesToAdd() {
		for (String fn : this.toAdd.values()) {
			System.out.println(fn);
		}
	}

	/**
	 * Print all files in toRemove
	 */
	public void printFilesToRemove() {
		for (String fn : this.toRemove.values()) {
			System.out.println(fn);
		}
	}

	/**
	 * Checks out all files in the node. Restore all oldFiles into StoredFiles
	 */
	public boolean checkout() {
		// Create a new temp node to consolidate actions specified in this node
		CommitNode consolidated = new CommitNode(this, -1);
		// This id of -1 is not used.
		for (StoredFile fInfo : consolidated.oldFiles) {
			fInfo.restoreFile();
		}
		return true;
	}

	/**
	 * Restore file if it exists. Else, return false
	 */
	public boolean checkout(String fileName) {
		/**
		 * Create a new temp node to consolidate actions specified in this node
		 * oldFile, addedFiles, removedFiles consolidations
		 */
		CommitNode consolidated = new CommitNode(this, -1); // id not used.
		File toRestore = new File(fileName);
		String fullFileName = toRestore.getAbsolutePath();
		for (StoredFile fInfo : consolidated.oldFiles) {
			if (fInfo.getAbsoluteFileName().equals(fullFileName)) {
				fInfo.restoreFile();
				return true;
			}
		}
		return false; // File does not exist
	}

	/**
	 * Return the history of the commit starting from this node
	 * 
	 * @return
	 */
	public TreeSet<Long> getCommitHistory() {
		TreeSet<Long> commitIDs = new TreeSet<Long>();
		CommitNode temp = this;
		while (temp != null) {
			commitIDs.add(temp.id);
			temp = temp.getParent();
		}
		return commitIDs;
	}

	/**
	 * Gives the "replay" copy of the commit node
	 */
	public CommitNode replay(CommitTree t) {
		CommitNode replayed = new CommitNode(t.getNextCommitId());
		replayed.msg = this.msg;
		replayed.time = new Date();
		replayed.oldFiles = new ArrayList<StoredFile>();
		replayed.oldFiles.addAll(this.oldFiles);
		replayed.addedFiles = new ArrayList<StoredFile>();
		replayed.addedFiles.addAll(this.addedFiles);
		replayed.deletedFiles = new ArrayList<String>();
		replayed.deletedFiles.addAll(this.deletedFiles);
		replayed.parent = null;
		return replayed;
	}

	/**
	 * Returns a HashMap<absFn, storedFile> of modified files after this commit
	 * node It is assumed that added and removed files are already taken care of
	 * in this node (assumed that the IPC is given)
	 */
	public HashMap<String, StoredFile> getModifiedFiles(CommitNode otherCommit) {
		HashMap<String, StoredFile> toReturn = new HashMap<String, StoredFile>();
		int pos;
		for (StoredFile fInfo : this.oldFiles) {
			pos = StoredFile.getStoredFile(otherCommit.oldFiles,
					fInfo.getAbsoluteFileName());
			if (pos != -1) {
				// file exists in the other commit, so compare the timestamps
				StoredFile otherInfo = otherCommit.oldFiles.get(pos);
				if (fInfo.getLastModified() != otherInfo.getLastModified()) {
					toReturn.put(fInfo.getAbsoluteFileName(), otherInfo);
				}
			}
		}
		return toReturn;
	}

	public void removeFromDeleteList(String fileName) {
		this.deletedFiles.remove(fileName);

	}

	/**
	 * Adds or replaces an existing StoredFile in the oldFiles list
	 * 
	 * @param fInfo
	 */
	public void addOrReplace(StoredFile fInfo) {
		if (fInfo == null)
			return;
		int existingPos = StoredFile.getStoredFile(this.oldFiles,
				fInfo.getOriginalFileName());
		if (existingPos != -1) {
			this.oldFiles.remove(existingPos);
		}
		this.oldFiles.add(fInfo);
	}
}
