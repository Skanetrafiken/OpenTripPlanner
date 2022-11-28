# Release Policy
1. Branch off from `resesok-develop-otp`. Branch should have prefix `release/X.X.X`. For example `release/1.1.14`
2. On newly created branch, rebase on `resesok-otp` to fetch release commits.
3. Modify update `pom.xml` with new version and commit.
4. Create pull request from newly created branch to `resesok-otp`.
5. When PR completes post-merge pipeline will be triggered automatically. Pipeline pushes git tag on merge commit, builds jar with maven and publishes it to nexus repository. It also builds new helm package and triggers deploy to K8s cluster.

![Release Flow](/.images/release_flow.png)
