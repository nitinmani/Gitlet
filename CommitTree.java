import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Stack;
import java.util.TreeSet;

/**
 * CommitTree stores CommitNode objects and branches. Adding instance variables
 * and data structures, and comments in this method provide an explanation
 *
 */
public class CommitTree implements Serializable, FileNameGenerator {
	private static final long serialVersionUID = 1L;
	// Name of the file n which this CommitTree is serialized and stored
	private final String REPOSITORY_FILE_NAME = "ctree.ser";
	private String repoParent; // Repository's parent directory
	private String repoDir; // The .gitlet directory
	private long lastUsedCommitId = 0; // Remembers last used commit id to
										// generate unique commit ids
	private long lastUsedFileNo = 0;
	private Branch master; // Points to the Master node in the commit tree
	private ArrayList<Branch> branches; // Stores all branches, including
										// master, in this array
	private Branch currBranch = master; // Pointer to current branch

	// To keep track of all commits by id and message
	// Store all maps with msg or id as key
	private HashMap<String, ArrayList<CommitNode>> allCommitsByMsg = new HashMap<String, ArrayList<CommitNode>>();
	private HashMap<Long, CommitNode> allCommitsById = new HashMap<Long, CommitNode>();

	/**
	 * Initializing the CommitTree object. Repository directory is the .gitlet
	 * folder, and we also create an arraylist of branches, setting the initial
	 * branch to be the "master" branch. We add that into branches. We also
	 * create an initial commit and add as the head node of the master branch.
	 * We are also tracking this commitNode, so that we can know how it changes
	 * in the future given certain messages. The current branch of the
	 * commitTree is the master branch upon initialization.
	 */
	public CommitTree(String repositoryParentDir) {
		this.repoParent = repositoryParentDir;
		this.repoDir = this.repoParent + "/.gitlet";
		this.branches = new ArrayList<Branch>();

		// Create initial commit node and add it to the CommitTree
		// Increment and use lastUsedCommitId
		CommitNode cnode = new CommitNode(++lastUsedCommitId);
		String initialMsg = "initial commit";
		cnode.setCommitMessage(initialMsg);
		// Remember the master branch
		this.master = new Branch("master", this, cnode);

		this.trackCommit(cnode);

		// Add master to the list of branches
		this.branches.add(this.master);

		// Keep a pointer to the currently used branch
		this.currBranch = master;

	}

	// --------------------------------------------------------------------------------------------
	/**
	 * This method retrieves a CommitTree from the given serialized file.
	 * 
	 * @param ctreeFileName
	 *            - Name of the file in which a serialized version of CommitTree
	 *            is stored
	 * @return The CommitTree
	 */
	public static CommitTree loadCommitTree(String ctreeFileName) {
		FileInputStream fIn;
		ObjectInputStream objIn;
		CommitTree ctree = null;
		try {
			fIn = new FileInputStream(ctreeFileName);
			objIn = new ObjectInputStream(fIn);

			ctree = (CommitTree) objIn.readObject();

			objIn.close();
			fIn.close();
		} catch (Exception e) {
			System.out.println("Could not read CommitTree to file: "
					+ ctreeFileName);
		}
		return ctree;
	}

	/**
	 * Writes the CommitTree to a file.
	 */
	public void save() {
		FileOutputStream fOut;
		ObjectOutputStream objOut;
		try {
			fOut = new FileOutputStream(repoDir + "/" + REPOSITORY_FILE_NAME);
			objOut = new ObjectOutputStream(fOut);
			objOut.writeObject(this);

			objOut.flush();
			fOut.flush();
			objOut.close();
			fOut.close();
		} catch (Exception e) {
			System.out.println("Could not write CommitTree to file: " + repoDir
					+ "/" + REPOSITORY_FILE_NAME);
		}
	}

	/**
	 * Gets repository location
	 */
	public String getLocation() {
		return this.repoDir;
	}

	/**
	 * Invokes the printCommitLog method of Branch.java
	 */
	public void log() {
		this.currBranch.printCommitLog();
	}

	/**
	 * Invokes printLog() method of CommitNode. Finds all CommitNodes through
	 * their ID
	 */
	public void globalLog() {
		for (CommitNode cnode : allCommitsById.values()) {
			cnode.printLog();
			System.out.println("");
		}
	}

	/**
	 * Search through all the commits with the same commitMsg Null implies that
	 * there are no commits with that message Else, you print out the commitID
	 * of each commitNode
	 */
	public void find(String commitMsg) {
		ArrayList<CommitNode> cnodes = this.allCommitsByMsg.get(commitMsg);
		if (cnodes == null) {
			System.out.println("Found no commit with that message.");
			return;
		}
		for (CommitNode c : cnodes) {
			System.out.println("Commit Id: " + c.getCommitId());
		}
	}

	/**
	 * Shows current state of gitlet. Current branch marked by asterisk. Staged
	 * files (files to be added) printed Files marked for removal also printed
	 */
	public void showStatus() {
		System.out.println("=== Branches ===");
		for (Branch b : branches) {
			if (b == currBranch) {
				System.out.print("*");
			}
			System.out.println(b.getName());
		}
		System.out.println();

		System.out.println("=== Staged Files ===");
		this.currBranch.printStagedFiles();
		System.out.println();

		System.out.println("=== Files Marked for Removal ===");
		this.currBranch.showFilesMarkedForRemoval();
		System.out.println();
	}

	/**
	 * Trivial case of checking out from same branch taken care of.
	 * 
	 * @param result
	 *            is set equal to the return value of checking out from that
	 *            branch If it's not in branches, it's a file name, and check
	 *            that out from current branch If all else fails, print out the
	 *            error message
	 */
	public void checkout(String name) {
		// First check if there is a branch with the given name
		if (currBranch.getName().equals(name)) {
			System.out.println("No need to checkout the current branch.");
			return;
		}
		boolean result = false;
		for (Branch b : branches) {
			if (b.getName().equals(name)) {
				result = true;
				boolean checkedOut = b.checkout();
				if (checkedOut) {
					this.currBranch = b;
					break;
				}
			}
		}
		if (!result) {
			// Checkout the given file name from current branch
			result = this.currBranch.checkout(name);
		}
		if (!result) {
			System.out
					.println("File does not exist in the most recent commit, or no such branch exists.");
		}
	}

	/**
	 * Two argument checkout: you have commitID and fileName if no commit with
	 * that ID exists, print error message or if commit cannot checkout the file
	 * with fileName, print error message
	 */
	public void checkout(String commitIdStr, String fileName) {
		Long commitId = Long.parseLong(commitIdStr);
		CommitNode cnode = this.allCommitsById.get(commitId);
		if (cnode != null) {
			if (!cnode.checkout(fileName)) {
				System.out.println("File does not exist in that commit.");
			}
		} else {
			System.out.println("No commit with that id exists.");
		}
	}

	/**
	 * Adding branch to arraylist of branches if it's name does not already
	 * exist
	 */
	public void addBranch(String branchName) {
		for (Branch b : branches) {
			if (b.getName().equals(branchName)) {
				System.out.println("A branch with that name already exists.");
				return;
			}
		}
		Branch toAdd = new Branch(branchName, this, currBranch.getHead());
		branches.add(toAdd);
	}

	/**
	 * Remove branch from arraylist of branches if it's not current Branch and
	 * it exists. Else print error message
	 */
	public void removeBranch(String branchName) {
		if (currBranch.getName().equals(branchName)) {
			System.out.println("Cannot remove the current branch.");
			return;
		}
		for (Branch b : branches) {
			if (b.getName().equals(branchName)) {
				b.setHead(null);
				branches.remove(b);
				return;
			}
		}
		System.out.println("A branch with that name does not exist.");

	}

	/**
	 * CommitTree gets next commitID by incrementing it
	 */
	public long getNextCommitId() {
		return ++this.lastUsedCommitId;
	}

	/**
	 * Access repository parent directory
	 */
	public String getRepositoryParentDir() {
		return this.repoParent;
	}

	/**
	 * Add commit into allCommitsByID and allCommitsByMsg. Bookkeeping purposes
	 */
	public void recordCommit(CommitNode aCommit) {
		// Add this commit to the list of all commits
		allCommitsById.put(new Long(aCommit.getCommitId()), aCommit);

		ArrayList<CommitNode> tempList = allCommitsByMsg.get(aCommit
				.getCommitMessage());
		if (tempList == null) {
			tempList = new ArrayList<CommitNode>();
		}
		tempList.add(aCommit);
		allCommitsByMsg.put(aCommit.getCommitMessage(), tempList);

	}

	/**
	 * Access current Branch
	 */
	public Branch getCurrentBranch() {
		return this.currBranch;
	}

	@Override
	/**
	 * CommitTree implements FileNameGenerator. Gives random fileName that represents the file in .gitlet
	 */
	public String getUniqueFileName(String extn) {
		return "FILE" + (++lastUsedFileNo) + "." + extn;
	}

	/**
	 * Gets repository fileName
	 */
	public String getRepoFileName() {
		return repoDir + "/" + REPOSITORY_FILE_NAME;
	}

	/**
	 * Resetting currBranch's head to the commit with that commitID Then
	 * checking out
	 */
	public void reset(String commitID) {

		CommitNode toResetTo = this.allCommitsById
				.get(Long.parseLong(commitID));
		currBranch.setHead(toResetTo);
		currBranch.getHead().checkout();
	}

	/**
	 * Finds the earliest common ancestor of the branches firstBr and secondBr
	 * 
	 * @param first
	 * @param second
	 *            -- could be currBr
	 * @return
	 */
	private CommitNode findEarliestCommonAncestor(Branch firstBr,
			Branch secondBr) {
		// add the commit nodes of the branch toRebase in a treeset
		// finding the split point of the two branches
		TreeSet<Long> firstHistory = firstBr.getHead().getCommitHistory();
		// Stack<CommitNode> copiedNodes = new Stack<CommitNode>();
		CommitNode temp = secondBr.getHead();
		while (temp != null) {
			if (!firstHistory.contains(temp.getCommitId())) {
				temp = temp.getParent();
			} else { // temp is common ancestor
				return temp;
			}
		}
		return null; // It should not come here.
	}

	/**
	 * New Merge method Merges the given branch to the current branch.
	 */
	public void merge(String branchName) {
		if (branchName.equals(currBranch.getName())) {
			System.out.println("Cannot merge a branch with itself");
			return;
		}
		CommitNode givenIPC = null;
		Branch givenBr = null;
		for (Branch b : branches) {
			if (branchName.equals(b.getName())) {
				// create IPC to find out what happened in the given branch
				givenBr = b;
				givenIPC = new CommitNode(b.getHead(), -2);
				break;
			}
		}
		if (givenBr == null) {
			System.out.println("A branch with that name does not exist");
			return;
		}

		// Files in given branch, not in currBranch copied to currBranch
		// create an IPC to calculate what happened in the current head
		CommitNode currIPC = new CommitNode(currBranch.getHead(), -1);
		// first find out what is not in the currIPC
		for (StoredFile fInfo : givenIPC.getOldFiles()) {
			int pos = StoredFile.getStoredFile(currIPC.getOldFiles(),
					fInfo.getOriginalFileName());
			if (pos == -1) { // The file in giveBr, not currBr. Add it
				currBranch.getHead().addOrReplace(fInfo);
				// it is not removed subsequently. (Because it might have just
				// been scheduled to be deleted in currBranch head.)
				currBranch.getHead().removeFromDeleteList(
						fInfo.getOriginalFileName());
			}
		}

		CommitNode ancestor = this.findEarliestCommonAncestor(givenBr,
				this.currBranch);
		CommitNode ancestorIPC = new CommitNode(ancestor, -2);
		HashMap<String, StoredFile> modifiedInGiven = ancestorIPC
				.getModifiedFiles(givenIPC);
		HashMap<String, StoredFile> modifiedInCurr = ancestorIPC
				.getModifiedFiles(currIPC);
		// Files modified in given but not modified in curr, should be copied
		for (String modGivenFn : modifiedInGiven.keySet()) {
			StoredFile modInCurrFinfo = modifiedInCurr.get(modGivenFn);
			if (modInCurrFinfo == null) {
				// the file is modified in given but it is not in current. It
				// should be copied to current.
				StoredFile theStFile = modifiedInGiven.get(modGivenFn);
				currBranch.getHead().addOrReplace(theStFile);
				currBranch.getHead().removeFromDeleteList(
						theStFile.getOriginalFileName());
			}
		}
		for (String modCurrFn : modifiedInCurr.keySet()) {
			StoredFile modInGivenFinfo = modifiedInGiven.get(modCurrFn);
			if (modInGivenFinfo == null) {
				// the file is modified in current but it is not in given. It
				// should stay as is in current.
			}
		}
		for (StoredFile fInfo : modifiedInGiven.values()) {
			StoredFile currFileInfo = modifiedInCurr.get(fInfo
					.getOriginalFileName());
			if (currFileInfo != null) { // The file is modified in both (after
										// the common ancestor point)
				if (currFileInfo.getTimeStamp() != fInfo.getTimeStamp()) {
					// there is a conflict. Copy the file from the givenIPC to
					// the FileSystem with .conflicted
					fInfo.restoreConflictedFile();
				}
			}
		}
		this.checkout(givenBr.getName());
		this.checkout(currBranch.getName());
	}

	/**
	 * Add commit node into allCommitsByID and allCommitsByMsg
	 */
	private void trackCommit(CommitNode cnode) {
		allCommitsById.put(new Long(cnode.getCommitId()), cnode);
		ArrayList<CommitNode> tempList = new ArrayList<CommitNode>();
		tempList.add(cnode);
		allCommitsByMsg.put(cnode.getCommitMessage(), tempList);
	}

	/**
	 * We need to take care of the special case in rebase. Added and modified
	 * files are added to an array list The commit nodes that are shifted use
	 * the list to propogate changes
	 */
	private ArrayList<StoredFile> addToftp(CommitNode ancestor,
			CommitNode given, CommitNode curr) {
		CommitNode ancestorIPC = new CommitNode(ancestor, -2);
		CommitNode givenIPC = new CommitNode(given, -3);
		CommitNode currIPC = new CommitNode(curr, -4);
		HashMap<String, StoredFile> modifiedInGiven = ancestorIPC
				.getModifiedFiles(givenIPC);
		HashMap<String, StoredFile> modifiedInCurr = ancestorIPC
				.getModifiedFiles(currIPC);
		ArrayList<StoredFile> filesToPropogate = new ArrayList<StoredFile>();
		// find out the files in modifiedInGiven but not in the modifiedInCurr
		for (String fileName : modifiedInGiven.keySet()) {
			StoredFile tempSF = modifiedInCurr.get(fileName);
			if (tempSF == null) {
				// the file is not modified in current. Propogate it
				filesToPropogate.add(modifiedInGiven.get(fileName));
			}
		}
		for (StoredFile fInfo : givenIPC.getOldFiles()) {
			// now we need to check if the file is added in the head of the
			// givenBranch.
			int i = StoredFile.getStoredFile(ancestorIPC.getOldFiles(),
					fInfo.getAbsoluteFileName());
			if (i == -1) {
				filesToPropogate.add(fInfo);
			}
		}
		return filesToPropogate;
	}

	/**
	 * Copying the commits from the current branch to the given branchName First
	 * we are taking care of the fail
	 */
	public void rebase(String branchName, boolean interactive) {
		Branch toRebase = null;
		CommitNode commonAncestor = null;
		if (branchName.equals(currBranch.getName())) {
			// you can't rebase a branch onto itself
			System.out.println("Cannot rebase a branch onto itself.");
			return;
		}
		for (Branch b : branches) {
			if (branchName.equals(b.getName())) {
				toRebase = b;
				break;
			}
		}
		if (toRebase == null) { // there's no such branch
			System.out.println("A branch with that name does not exist.");
			return;
		}
		if (toRebase.inHistory(currBranch)) {
			// reset head of the currBranch to the toRebase head
			currBranch.setHead(toRebase.getHead());
			currBranch.checkout();
			return;
		} else if (currBranch.inHistory(toRebase)) {
			// if toRebase is in history of currBranch, already up to date
			System.out.println("Already up-to-date.");
			return;
		} else { // add the commit nodes of the branch toRebase in a treeset
			// finding the split point of the two branches
			TreeSet<Long> rebaseHistory = toRebase.getHead().getCommitHistory();
			Stack<CommitNode> copiedNodes = new Stack<CommitNode>();
			CommitNode temp = currBranch.getHead();
			while (temp != null) {
				if (!rebaseHistory.contains(temp.getCommitId())) {
					copiedNodes.push(temp);
					temp = temp.getParent();
				} else { // temp is common ancestor
					commonAncestor = temp;
					break;
				}
			}
			ArrayList<StoredFile> filesToPropogate = this.addToftp(
					commonAncestor, toRebase.getHead(), currBranch.getHead());
			// add the stack nodes to toRebase. Attaching replayed nodes
			currBranch.setHead(toRebase.getHead());
			boolean toProceed = true;
			while (!copiedNodes.empty()) {
				CommitNode poppedNode = copiedNodes.pop();
				toProceed = true; // assuming that we will proceed
				Object userResponse = null;
				Boolean newMsgGivenByUser = false;
				// if the stack is empty, it is head node. User is not prompted
				// the initial commit should not be skipped or modified either
				if (interactive) {
					Boolean allowSkip = true;
					if (copiedNodes.isEmpty() || poppedNode.getParent() == null) {
						// extreme nodes. Initial or final commit
						allowSkip = false;
					}
					userResponse = confirmReplay(poppedNode, allowSkip);
					if (userResponse instanceof Boolean) {
						toProceed = ((Boolean) userResponse).booleanValue();
						newMsgGivenByUser = false;
					} else {
						toProceed = true;
						newMsgGivenByUser = true;
					}
				}
				if (toProceed) { // invoke replay method
					CommitNode replayed = poppedNode.replay(this);
					if (newMsgGivenByUser)
						replayed.setCommitMessage((String) userResponse);
					replayed.getOldFiles().addAll(filesToPropogate);
					replayed.setParent(currBranch.getHead());
					currBranch.setHead(replayed);
					this.trackCommit(replayed);
				}
			}
			currBranch.checkout();
		}
	}

	/**
	 * This method gets the confirmation from the user to replay a node. If the
	 * user says "c", then the method returns Boolean object True If the user
	 * says "s", the method returns Boolean object False If the user says "m",
	 * the method returns String with new message
	 */
	private Object confirmReplay(CommitNode repNode, boolean canSkip) {
		// prompting user for their input
		Object returnVal = null;
		boolean looping = true;
		while (looping) {
			System.out.println("Currently replaying:");
			repNode.printLog();
			System.out
					.println("Would you like to (c)ontinue, (s)kip this commit, or change this commit's (m)essage? ");
			Scanner scanner = new Scanner(System.in);
			String answer = scanner.nextLine();
			switch (answer) {
			case "s":
				if (!canSkip)
					break; // you cannot skip the initial or final nodes
				returnVal = new Boolean(false);
				looping = false;
				break;

			case "c":
				returnVal = new Boolean(true);
				looping = false;
				break;

			case "m":
				System.out
						.println("Please enter a new message for this commit. ");
				Scanner forMsg = new Scanner(System.in);
				String newMsg = forMsg.nextLine();
				returnVal = new String(newMsg);
				looping = false;
				break;
			default:
			}
		}
		return returnVal;
	}
}
