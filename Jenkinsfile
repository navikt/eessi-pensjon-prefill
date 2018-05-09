pipeline {
    agent any
    tools {
        maven('Maven 3.3.9')
    }

    environment {
        repo = "docker.adeo.no:5000"
        FASIT_ENV = 't8'
        APPLICATION_NAMESPACE = 't8'
        ZONE = 'fss'
    }

    stages {
        stage('Initialize') {
            steps {
                script {
                    app_name = sh(script: "gradle properties | grep ^name: | sed 's/name: //'", returnStdout: true).trim()
                    version = sh(script: "gradle properties | grep ^version: | sed 's/version: //'", returnStdout: true).trim()
                    branchName = "${env.BRANCH_NAME}"
                    if (version.endsWith("-SNAPSHOT")) {
                        commitHashShort = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                        version = "${version}.${branchName.replaceAll('/', '-')}.${env.BUILD_ID}-${commitHashShort}"
                    }
                    applicationFullName = "${app_name}:${version}"
                }
            }
        }

        stage('Build') {
            steps {
                sh('gradle clean assemble')
            }
        }

        stage('Test') {
            steps {
                sh('gradle test')
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
                        echo "\tdeploying to u2"
                        deploy.naisDeploy(app_name, version, FASIT_ENV, APPLICATION_NAMESPACE, ZONE)
                    } else if(branchName == 'master') {
                        echo "\tdeploying to t8"
                        deploy.naisDeploy(app_name, version, FASIT_ENV, APPLICATION_NAMESPACE, ZONE)
                    } else {
                        echo "Skipping deploy"
                    }
                }
            }
        }
    }
}