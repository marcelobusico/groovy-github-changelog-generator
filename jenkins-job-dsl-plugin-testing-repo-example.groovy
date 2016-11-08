job('DslBuildApp') {
    parameters {
        stringParam('PARAMETER_A', 'PARAMETER_VALUE', 'This is the Parameter A')
    }
    properties {
        promotions {
            promotion {
                name('PublishRelease')
                conditions {
                    manual('')
                }
                actions {
                    shell('groovy /var/lib/jenkins-scripts/ChangelogGenerator.groovy --create-github-release=true')
                }
            }
        }
    }
    scm {
        git {
        remote {
                name('origin')
                url('git@github.com:marcelobusico/changelog-testing-repo.git')
            }
            branch('refs/heads/release-2.0')
        }
    }
    steps {
        shell("echo 'Hey, this is a generated build using DSL Script.'")
    }
    publishers {
        git {
            pushOnlyIfSuccess()
            tag('origin', '$JOB_NAME-$BUILD_NUMBER') {
                message('Automatically created tag by jenkins job.')
                create()
            }
        }
        postBuildScripts {
            steps {
                shell('/var/lib/jenkins-scripts/post-build-script.sh')
            }
            onlyIfBuildSucceeds(true)
            onlyIfBuildFails(false)
        }
    }
}
