public class Gitlet {

	/**
	 * Main method controls the switch statements. It calls a GitHelper object
	 * that calls the CommitTree object that does all of the work. If none of
	 * the statements are hit, then you tell the user that he passed in invalid
	 * arguments. If that's not the case either, then the user passed in nothing
	 * and we give the appropriate message
	 */
	public static void main(String[] args) {
		if ((args != null) && (args.length != 0)) { // Make sure arguments are
													// given
			GitletHelper helper = new GitletHelper();
			switch (args[0]) {
			case "init":
				helper.createGitlet();
				break;

			case "add":
				if ((args.length >= 2) && (args[1] != null))
					helper.add(args[1]);
				break;

			case "commit":
				if ((args.length >= 2) && (args[1] != null)
						&& (args[1].trim().length() != 0)) { // args is not null
																// already.
					helper.commit(args[1]);
				} else
					System.out.println("Please enter a commit message.");
				break;

			case "rm":
				if ((args.length >= 2) && (args[1] != null)
						&& (args[1].trim().length() != 0)) {
					helper.remove(args[1]);
				} else
					System.out.println("No reason to remove the file.");
				break;

			case "log":
				helper.log();
				break;

			case "global-log":
				helper.globalLog();
				break;

			case "find":
				helper.find(args[1]);
				break;

			case "status":
				helper.status();
				break;

			case "checkout":
				helper.checkout(args);
				break;

			case "branch":
				helper.branch(args[1]);
				break;

			case "rm-branch":
				helper.remove(args[1]);
				break;

			case "reset":
				helper.reset(args[1]);
				break;

			case "merge":
				helper.merge(args[1]);
				break;

			case "rebase":
				helper.rebase(args[1]);
				break;

			case "i-rebase":
				helper.interactiveRebase(args[1]);
				break;
			default:
				System.out.println("Invalid argument(s)");
			}
		} else
			System.out
					.println("No argument was given.  Usage: java Gitelet [command] [parameters]");
	}
}
