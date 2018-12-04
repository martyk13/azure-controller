package com.amplify.ap.services.templates;

import com.amplify.ap.domain.Template;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the connection to the git repository containing the {@link Template} files
 * and adds/creates any metadata in the DB associated with these
 */
public class GitScanner implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitScanner.class);

    private String gitDirectory;

    private String gitRepositoryUri;

    private String gitBranch;

    private TemplateService templateService;

    private Git gitRepository;

    public GitScanner(String gitDirectory, String gitRepositoryUri, String gitBranch, TemplateService templateService) {
        this.gitDirectory = gitDirectory;
        this.gitRepositoryUri = gitRepositoryUri;
        this.gitBranch = gitBranch;
        this.templateService = templateService;
    }

    /**
     * Open the git repository ready to be scanned
     * if the repository doesn't currently exist then clone it
     *
     * @throws IOException
     * @throws GitAPIException
     */
    public void openGitRepo() throws IOException, GitAPIException {
        try {
            LOGGER.info("Opening the git repository {}", gitDirectory);
            gitRepository = Git.open(new File(gitDirectory));

        } catch (RepositoryNotFoundException e) {
            // Need to clone the repo first
            LOGGER.info("Cloning the git repository {}", gitRepositoryUri);
            gitRepository = Git.cloneRepository()
                    .setURI(gitRepositoryUri)
                    .setDirectory(new File(gitDirectory))
                    .call();
        }
    }

    /**
     * Close the connection to the git repository in a controlled way
     */
    public void closeGitRepo() {
        LOGGER.info("Closing the git repository");
        gitRepository.close();
    }

    @Override
    public void run() {
        scanGitRepository();
    }

    /**
     * Scan the git repository for any template files.
     * When a template is found this is checked against the
     * existing database metadata and any additional information
     * or templates detected since the last scan are added.
     */
    public void scanGitRepository() {
        try {
            openGitRepo();

            LOGGER.info("Fetching latest updates from git");
            gitFetch();

            LOGGER.info("Beginning scan of git repository");
            // Make sure the correct branch is chacked out before parsing the files
            CheckoutCommand checkout = gitRepository.checkout();
            Ref branchRef = checkout.setName(gitBranch).call();

            // HARD reset the branch to ensure it matches the remote
            gitRepository.reset().setMode(ResetCommand.ResetType.HARD).setRef("origin/" + gitBranch).call();

            // a RevWalk allows to walk over commits based on some filtering that is defined
            try (RevWalk walk = new RevWalk(gitRepository.getRepository())) {

                RevCommit commit = walk.parseCommit(branchRef.getObjectId());
                RevTree tree = commit.getTree();

                // now use a TreeWalk to iterate over all files in the Tree recursively
                // you can set Filters to narrow down the results if needed
                TreeWalk treeWalk = new TreeWalk(gitRepository.getRepository());
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                while (treeWalk.next()) {
                    String templateFilePath = treeWalk.getPathString();
                    LOGGER.info("File found in git repository: " + templateFilePath);
                    RevCommit lastCommit = getLastCommitForFile(templateFilePath);
                    Template nextTemplate = new Template(templateFilePath,
                            null,
                            lastCommit.getAuthorIdent().getName(),
                            lastCommit.getAuthorIdent().getEmailAddress(),
                            lastCommit.getCommitterIdent().getName(),
                            lastCommit.getCommitterIdent().getEmailAddress(),
                            lastCommit.getFullMessage(),
                            lastCommit.getCommitTime());
                    templateService.updateTemplate(nextTemplate);
                }
            }
        } catch (IOException | GitAPIException e) {
            LOGGER.error("Error scanning git repository: " + e.getMessage());
        } finally {
            closeGitRepo();
        }
    }

    /**
     * Get the latest {@link RevCommit} from the git repository for the provided file path
     *
     * @param templateFilePath {@link String} containing the file path
     * @return {@link RevCommit}  the latest commit information for the file
     * @throws IOException
     */
    private RevCommit getLastCommitForFile(String templateFilePath) throws IOException {
        try (RevWalk revWalk = new RevWalk(gitRepository.getRepository())) {
            Ref headRef = gitRepository.getRepository().exactRef(Constants.HEAD);
            RevCommit headCommit = revWalk.parseCommit(headRef.getObjectId());
            revWalk.markStart(headCommit);
            revWalk.sort(RevSort.COMMIT_TIME_DESC);
            revWalk.setTreeFilter(AndTreeFilter.create(PathFilter.create(templateFilePath), TreeFilter.ANY_DIFF));
            return revWalk.next();
        }
    }

    /**
     * Perform a fetch on the git repo to get all the latest commits
     *
     * @throws {@link GitAPIException}
     */
    private void gitFetch() throws GitAPIException {
        FetchCommand fetch = gitRepository.fetch();
        List<RefSpec> specs = new ArrayList<>();
        specs.add(new RefSpec("+refs/heads/*:refs/remotes/origin/*"));
        specs.add(new RefSpec("+refs/tags/*:refs/tags/*"));
        specs.add(new RefSpec("+refs/notes/*:refs/notes/*"));
        fetch.setRefSpecs(specs);
        fetch.call();
    }


}
