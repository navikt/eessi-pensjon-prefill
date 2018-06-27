pipeline {
    agent any

    environment {
        repo = "docker.adeo.no:5000"
        FASIT_ENV= 't1'
        APPLICATION_NAMESPACE = 't1'
        FASIT_ENV_TEST = 't8'
        APPLICATION_NAMESPACE_TEST = 't8'
        ZONE = 'fss'
    }

    stages {
        stage('Initialize') {
            steps {
                script {
                    app_name = sh(script: " sh gradlew properties | grep ^name: | sed 's/name: //'", returnStdout: true).trim()
                    version = sh(script: "sh gradlew properties | grep ^version: | sed 's/version: //'", returnStdout: true).trim()
                    branchName = "${env.BRANCH_NAME}"
                    if (branchName != "master") {
                        commitHashShort = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                        version = "${version}.${branchName.replaceAll('/', '-')}.${env.BUILD_ID}-${commitHashShort}"
                    } else {
                        version = "${version}-${env.BUILD_ID}"
                    }
                    applicationFullName = "${app_name}:${version}"
                }
            }
        }

        stage('Build') {
            steps {
                sh('sh gradlew clean assemble')
            }
        }

        stage('Test') {
            steps {
                sh('sh gradlew test')
            }
            post {
                always {
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
                }
            }
        }

        stage('Docker') {
            steps {
                script {
                    docker.withRegistry("https://${repo}") {
                        image = docker.build("${applicationFullName}", "--build-arg GIT_COMMIT_ID=${commitHashShort} .")
                        image.push()
                        image.push('latest')
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                script {
                    echo "Deploy '${branchName}'?"
                    if (branchName.startsWith('feature')) {
                        echo "\tdeploying to t8 (test)"
                        deploy.naisDeploy(app_name, version, FASIT_ENV_TEST, APPLICATION_NAMESPACE_TEST, ZONE)
                    } else if (branchName == 'master') {
                        echo "\tdeploying to t1 (master)"
                        deploy.naisDeploy(app_name, version, FASIT_ENV, APPLICATION_NAMESPACE, ZONE)
                    } else {
                        echo "Skipping deploy"
                    }
                }
            }
        }
    }
}