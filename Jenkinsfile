#!/usr/bin/env groovy
@Library('jenkins-pipeline-lib') _

node {
    def commitHash
    try {
        cleanWs()

        def version
        stage("checkout") {
            withCredentials([string(credentialsId: 'navikt-ci-oauthtoken', variable: 'GITHUB_OAUTH_TOKEN')]) {
                sh "git init"
                sh "git pull https://${GITHUB_OAUTH_TOKEN}:x-oauth-basic@github.com/navikt/eessi-pensjon-fagmodul.git"
            }

            sh "make bump-version"

            version = sh(script: 'cat VERSION', returnStdout: true).trim()

            commitHash = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
            github.commitStatus("navikt-ci-oauthtoken", "navikt/eessi-pensjon-fagmodul", 'continuous-integration/jenkins', commitHash, 'pending', "Build #${env.BUILD_NUMBER} has started")
        }

        stage("build") {
            try {
                sh "make VERSION=${version}"
            } catch (e) {
                junit('build/test-results/test/**/*.xml')
                publishHTML([
                        allowMissing         : false,
                        alwaysLinkToLastBuild: false,
                        keepAll              : true,
                        reportDir            : 'build/reports/tests/test',
                        reportFiles          : 'index.html',
                        reportName           : 'HTML Report',
                        reportTitles         : ''
                ])

                throw e
            }
        }

        stage("release") {
            withCredentials([usernamePassword(credentialsId: 'nexusUploader', usernameVariable: 'NEXUS_USERNAME', passwordVariable: 'NEXUS_PASSWORD')]) {
                sh "docker login -u ${env.NEXUS_USERNAME} -p ${env.NEXUS_PASSWORD} repo.adeo.no:5443"
            }

            sh "make release"

            withCredentials([string(credentialsId: 'navikt-ci-oauthtoken', variable: 'GITHUB_OAUTH_TOKEN')]) {
                sh "git push --tags https://${GITHUB_OAUTH_TOKEN}@github.com/navikt/eessi-pensjon-fagmodul HEAD:master"
            }
        }

        stage("upload manifest") {
            withCredentials([usernamePassword(credentialsId: 'nexusUploader', usernameVariable: 'NEXUS_USERNAME', passwordVariable: 'NEXUS_PASSWORD')]) {
                sh "make manifest VERSION=${version}"
            }
        }

        stage("deploy") {
            build([
                    job       : 'nais-deploy-pipeline',
                    wait      : false,
                    parameters: [
                            string(name: 'APP', value: "eessi-fagmodul"),
                            string(name: 'REPO', value: "navikt/eessi-pensjon-fagmodul"),
                            string(name: 'VERSION', value: version),
                            string(name: 'COMMIT_HASH', value: commitHash),
                            string(name: 'DEPLOY_ENV', value: 't1')
                    ]
            ])
        }

        github.commitStatus("navikt-ci-oauthtoken", "navikt/eessi-pensjon-fagmodul", 'continuous-integration/jenkins', commitHash, 'success', "Build #${env.BUILD_NUMBER} has finished")
    } catch (err) {
        github.commitStatus("navikt-ci-oauthtoken", "navikt/eessi-pensjon-fagmodul", 'continuous-integration/jenkins', commitHash, 'failure', "Build #${env.BUILD_NUMBER} has failed")

        throw err
    }
}
