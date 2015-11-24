import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;

/**
 * Stores information about a file that is being committed.
 */
public class StoredFile implements Serializable {
	private static final long serialVersionUID = 1L;
	private String originalFileName;
	private String absFileName;
	private String storedFileName;
	private Date timestamp;
	private long lastUsedFileNo = 0;

	/**
	 * construct StoredFile object
	 */
	public StoredFile(String fileName) {
		this.originalFileName = fileName;
		File f = new File(fileName);
		this.absFileName = f.getAbsolutePath();
	}

	/**
	 * Access originalFileName
	 */
	public String getOriginalFileName() {
		return originalFileName;
	}

	/**
	 * Access timeStamp
	 */
	public Date getTimeStamp() {
		return this.timestamp;
	}

	/**
	 * Access absolute fileName
	 */
	public String getAbsoluteFileName() {
		return this.absFileName;
	}

	/**
	 * Access storedFileName
	 */
	public String getStoredFileName() {
		return this.storedFileName;
	}

	/**
	 * For merge, if a file is conflicted, copy stored file to original file
	 * with .conflicted at end
	 */
	public void restoreConflictedFile() {
		String currDir = System.getProperty("user.dir");
		try {
			// Copy the stored file in .gitlet dir to the original file
			File srcFile = new File(currDir + "/.gitlet/" + this.storedFileName);
			File destFile = new File(this.originalFileName + ".conflicted");
			Files.copy(srcFile.toPath(), destFile.toPath(),
					StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			return;
		}
	}

	/**
	 * Generate random file with tree and store that in .gitlet
	 */
	public boolean storeFile(String repoLoc, long commitId,
			FileNameGenerator fng) {
		// Generate new file name to store
		// Take the parent dir
		String currDir = System.getProperty("user.dir");
		try {
			// Copy the original file to .gitlet dir under the storedFileName
			File srcFile = new File(this.originalFileName);
			this.storedFileName = fng.getUniqueFileName("" + commitId);
			File destFile = new File(currDir + "/.gitlet/"
					+ this.storedFileName);
			Files.copy(srcFile.toPath(), destFile.toPath());

			// Copy the original file's timestamp
			this.timestamp = new Date(srcFile.lastModified());
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * Copy the stored file in .gitlet dir to the original file
	 */
	public boolean restoreFile() {
		String currDir = System.getProperty("user.dir");
		try {
			File srcFile = new File(currDir + "/.gitlet/" + this.storedFileName);
			File destFile = new File(this.originalFileName);
			Files.copy(srcFile.toPath(), destFile.toPath(),
					StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * Returns the position of StoredFile object for the given original file
	 * name, if it exists
	 */
	public static int getStoredFile(ArrayList<StoredFile> fInfoList,
			String fileName) {
		if (fInfoList == null)
			return -1;
		File f = new File(fileName);
		String fullFileName = f.getAbsolutePath();
		for (int i = 0; i < fInfoList.size(); ++i) {
			if (fInfoList.get(i).getOriginalFileName().equals(fullFileName))
				return i;
		}
		return -1;
	}

	/**
	 * Get the time of the timestamp to find out if a file has been changed
	 */
	public long getLastModified() {
		return this.timestamp.getTime();
	}

	/**
	 * increment the fileNumber to get the nextFileNo
	 */
	public long getNextFileNo() {
		return ++lastUsedFileNo;
	}
}
