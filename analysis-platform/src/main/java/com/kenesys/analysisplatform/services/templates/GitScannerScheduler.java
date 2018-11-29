package com.kenesys.analysisplatform.services.templates;

import com.kenesys.analysisplatform.domain.Template;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.util.Date;

@Component
public class GitScannerScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitScannerScheduler.class);

    @Value( "${templates.gitscanner.gitdir}" )
    private String gitDirectory;

    @Value( "${templates.gitscanner.gituri}" )
    private String gitRepositoryUri;

    @Value( "${templates.gitscanner.gitbranch}" )
    public String gitBranch;

    @Autowired
    private TemplateService templateService;

    private Git gitRepository;

    @PostConstruct
    public void setupGitRepo() throws IOException, GitAPIException {
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

    @PreDestroy
    public void closeGitRepo(){
        LOGGER.info("Closing the git repository");
        gitRepository.close();
    }

    @Scheduled(fixedRateString  = "${templates.gitscanner.schedule}")
    public void scanGitRepository() throws IOException, GitAPIException {
        LOGGER.info("Beginning scan of git repository");

        // TODO: neeed to do a fetch here as well

        // Make sure the correct branch is chacked out before parsing the files
        CheckoutCommand checkout = gitRepository.checkout();
        Ref branchRef = checkout.setName(gitBranch).call();

        // a RevWalk allows to walk over commits based on some filtering that is defined
        try(RevWalk walk = new RevWalk(gitRepository.getRepository())) {

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
                RevCommit lastCommit = geLastCommitForFile(templateFilePath);
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
    }

    private RevCommit geLastCommitForFile(String templateFilePath) throws IOException {
        try( RevWalk revWalk = new RevWalk( gitRepository.getRepository() ) ) {
            Ref headRef = gitRepository.getRepository().exactRef( Constants.HEAD );
            RevCommit headCommit = revWalk.parseCommit( headRef.getObjectId() );
            revWalk.markStart( headCommit );
            revWalk.sort( RevSort.COMMIT_TIME_DESC );
            revWalk.setTreeFilter( AndTreeFilter.create( PathFilter.create( templateFilePath ), TreeFilter.ANY_DIFF ) );
            return revWalk.next();
        }

    }
}
