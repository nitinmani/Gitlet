import java.io.File;
import java.util.Scanner;

public class GitletHelper {

	/**
	 * Called when "init" is passed in. The method creates a new .gitlet
	 * directory if it does not exist in the current directory. In addition, it
	 * creates the commitTree that stores all the branches/commitNodes
	 * CommitTree is then serialized (saved) Else it prints the statement that
	 * notifies the user that a version control system already exists
	 */
	public void createGitlet() {
		// Check if .gitlet directory exists in the given location
		String gitletParent = System.getProperty("user.dir");
		// Current Directory is the Repository Location
		File x = new File(gitletParent);
		File f = new File(gitletParent + "/.gitlet");
		if (f.exists() && f.isDirectory()) {
			// .gitlet already exists in this location
			System.out
					.println("A gitlet version control system already exists in the current directory.");
		} else {
			try {
				f.mkdir(); // Create the directory
				// Now .gitlet directory is created. Create a CommitTree and
				// store in the repository location
				CommitTree ctree = new CommitTree(x.getAbsolutePath());
				// Create Commit tree with an initial commit
				ctree.save(); // Serialize the CommitTree
			} catch (Exception e) {
				System.out.println("Could not create directory: "
						+ gitletParent + "/.gitlet");
			}
		}
	}

	/**
	 * A fileToAdd is created with the name of the file passed. If the file does
	 * not exist or if it's a directory, then the file doesn't exist Else, you
	 * load (deserialize) the commit tree and you invoke its add method Then
	 * serialize the tree
	 */
	public void add(String argPassed) {
		// Check if the file exists in the working directory
		String fileName = argPassed.trim();
		File fileToAdd = new File(fileName);
		if (!fileToAdd.exists() || fileToAdd.isDirectory()) {
			System.out.println("File does not exist.");
			return;
		}
		CommitTree ctree = CommitTree.loadCommitTree(System
				.getProperty("user.dir") + "/.gitlet/ctree.ser");
		if (ctree != null) {
			ctree.getCurrentBranch().add(fileName);
			ctree.save(); // Store the tree as the job is done.
		}
	}

	/**
	 * Load CommitTree, then if it's not null, you invoke the printCommitLog()
	 * method.
	 */
	public void log() {
		// Load the CommitTree from the .gitlet subdirectory
		CommitTree ctree = CommitTree.loadCommitTree(System
				.getProperty("user.dir") + "/.gitlet/ctree.ser");
		if (ctree != null) {
			ctree.getCurrentBranch().printCommitLog();
		}
	}

	/**
	 * Same as log, but invokes globalLog() method
	 */
	public void globalLog() {
		CommitTree ctree = CommitTree.loadCommitTree(System
				.getProperty("user.dir") + "/.gitlet/ctree.ser");
		if (ctree != null) {
			ctree.globalLog();
		}
	}

	/**
	 * Same as before, but invokes showStatus() method
	 */
	public void status() {
		CommitTree ctree = CommitTree.loadCommitTree(System
				.getProperty("user.dir") + "/.gitlet/ctree.ser");
		if (ctree != null) {
			ctree.showStatus();
		}
	}

	/**
	 * Checks if msg is null, and if it is, there is obviously no commit with
	 * that message. Then returns. Else, CommitTree is loaded, and if not null,
	 * invokes find(msg) method
	 */
	public void find(String msg) {
		if (msg == null) {
			System.out.println("Found no commit with that message.");
			return;
		}
		CommitTree ctree = CommitTree.loadCommitTree(System
				.getProperty("user.dir") + "/.gitlet/ctree.ser");
		if (ctree != null) {
			ctree.find(msg);
		}
	}

	/**
	 * Some methods are dangerous. We prompt the user with this message and
	 * return true if the user inputs a "yes" as their userResponse, false
	 * otherwise.
	 */
	private boolean promptDanger(Scanner sc) {
		System.out
				.println("Warning: The command you entered may alter the files in your working directory.\n "
						+ "Uncommitted changes may be lost. Are you sure you want to continue? (yes/no)");
		String userResponse = sc.nextLine();
		// TODO: Check if the answer has to be exactly "yes" or can it have
		// spaces before and after.
		userResponse = userResponse.trim();
		if (!"yes".equals(userResponse)) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Same as find(msg), but we do so with a branch instead, and invoke
	 * CommitTree's addBranch(branch) method. Since the tree is changed, we have
	 * to serailize it again.
	 */
	public void branch(String branch) {
		if (branch == null) {
			System.out.println("No branch name given.");
			return;
		} else {
			CommitTree ctree = CommitTree.loadCommitTree(System
					.getProperty("user.dir") + "/.gitlet/ctree.ser");
			if (ctree != null) {
				ctree.addBranch(branch);
				ctree.save();
			}
		}
	}

	/**
	 * Same as branch, except it's called from a static context, so a CommitTree
	 * is passed in as well
	 */
	public static void removeBranch(CommitTree ctree, String branch) {
		if (branch == null) {
			System.out.println("No branch name given.");
			return;
		} else {
			if (ctree != null) {
				ctree.removeBranch(branch);
				ctree.save();
			}
		}
	}

	/**
	 * First we have to check if the branch we rebase to is not null. If so, we
	 * give the user the appropriate message and return. Else, we prompt the
	 * user, letting him/her know that the method is dangerous, and load the
	 * commitTree and invoke the rebase method with the boolean value true, to
	 * denote that it is interactive. We then serialize the tree.
	 */
	public void interactiveRebase(String toBranch) {
		CommitTree ctree = null;
		if (toBranch == null) {
			System.out.println("No branch is given.");
			return;
		} else {
			Scanner scanner = new Scanner(System.in);
			if (promptDanger(scanner)) {
				ctree = CommitTree.loadCommitTree(System
						.getProperty("user.dir") + "/.gitlet/ctree.ser");
				if (ctree != null) {
					// on interactive mode
					ctree.rebase(toBranch, true);
					ctree.save();
				}
			}
		}
	}

	/**
	 * Same as interactiveRebase, but the boolean value is false, to denote that
	 * it is NOT interactive
	 */
	public void rebase(String toBranch) {
		if (toBranch == null) {
			System.out.println("No branch is given.");
			return;
		}
		Scanner scanner = new Scanner(System.in);
		if (promptDanger(scanner)) {
			CommitTree ctree = CommitTree.loadCommitTree(System
					.getProperty("user.dir") + "/.gitlet/ctree.ser");
			if (ctree != null) {
				// on interactive mode
				ctree.rebase(toBranch, false);
				ctree.save();
			}
		}
	}

	/**
	 * Merge is also dangerous and so we check for the null case, and if that
	 * does not return, we prompt the User. We then load the CommitTree object
	 * and invoke its merge method on the argument passed in
	 */
	public void merge(String branch) {
		if (branch == null) {
			System.out.println("No branch is given.");
			return;
		} else {
			Scanner scanner = new Scanner(System.in);
			if (promptDanger(scanner)) {
				CommitTree ctree = CommitTree.loadCommitTree(System
						.getProperty("user.dir") + "/.gitlet/ctree.ser");
				if (ctree != null) {
					ctree.merge(branch);
					ctree.save();
				}
			}
		}
	}

	/**
	 * We load the commitTree object, and if it is not null, we pass in the
	 * commit message and invoke the commit(msg) method of the CommitTree Then,
	 * serialize the tree.
	 */
	public void commit(String msg) {
		CommitTree ctree = CommitTree.loadCommitTree(System
				.getProperty("user.dir") + "/.gitlet/ctree.ser");
		if (ctree != null) {
			ctree.getCurrentBranch().commit(msg);
			ctree.save(); // Store the tree as the job is done.
		}
	}

	/**
	 * Same as before except invoke the remove(name) method of CommitTree
	 */
	public void remove(String name) {
		CommitTree ctree = CommitTree.loadCommitTree(System
				.getProperty("user.dir") + "/.gitlet/ctree.ser");
		if (ctree != null) {
			ctree.getCurrentBranch().remove(name);
			ctree.save(); // Store the tree as the job is done.
		}
	}

	/**
	 * Checkout has many cases to take care of. So, we prompt the user as it is
	 * a dangerous method, then load the commitTree, and if it not null, we
	 * check two cases. If two args are passed in, you are given a commitID and
	 * fileName and we invoke the respective checkout method in CommitTree.
	 * Else, we invoke the one arg method (fileName or branchName passed in)
	 */
	public void checkout(String[] args) {
		Scanner scanner = new Scanner(System.in);
		if (promptDanger(scanner)) {
			CommitTree ctree = CommitTree.loadCommitTree(System
					.getProperty("user.dir") + "/.gitlet/ctree.ser");
			if (ctree != null) {
				if (args.length > 2) {
					ctree.checkout(args[1], args[2]);
				} else {
					ctree.checkout(args[1]);
				}
				ctree.save();
			}
		}
	}

	/**
	 * Reset needs a commitID to reset to. If it's null, give user appropriate
	 * message and return. Else, prompt the user since it is a dangerous method
	 * and load the commitTree. If it is not null, then you invoke the reset
	 * method of the CommitTree
	 */
	public void reset(String commitIdStr) {
		if (commitIdStr == null) {
			System.out.println("No commit ID is given.");
			return;
		} else {
			Scanner scanner = new Scanner(System.in);
			if (promptDanger(scanner)) {
				CommitTree ctree = CommitTree.loadCommitTree(System
						.getProperty("user.dir") + "/.gitlet/ctree.ser");
				if (ctree != null) {
					ctree.reset(commitIdStr);
					ctree.save();
				}
			}
		}
	}
}
