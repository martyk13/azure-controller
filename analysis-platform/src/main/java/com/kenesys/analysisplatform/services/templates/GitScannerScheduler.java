package com.kenesys.analysisplatform.services.templates;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;

@Component
public class GitScannerScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitScannerScheduler.class);

    @Value( "${templates.gitscanner.gitdir}" )
    private String gitDirectory;

    @Value( "${templates.gitscanner.gituri}" )
    private String gitRepositoryUri;

    @Value( "${templates.gitscanner.gitbranch}" )
    public String gitBranch;

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

        // Make sure the correct branch is chacked out before parsing the files
        CheckoutCommand checkout = gitRepository.checkout();
        Ref branchRef = checkout.setName(gitBranch).call();

        // a RevWalk allows to walk over commits based on some filtering that is defined
        RevWalk walk = new RevWalk(gitRepository.getRepository());

        RevCommit commit = walk.parseCommit(branchRef.getObjectId());
        RevTree tree = commit.getTree();

        // now use a TreeWalk to iterate over all files in the Tree recursively
        // you can set Filters to narrow down the results if needed
        TreeWalk treeWalk = new TreeWalk(gitRepository.getRepository());
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);
        while (treeWalk.next()) {
            LOGGER.info("File found in git repository: " + treeWalk.getPathString());
        }
    }
}
