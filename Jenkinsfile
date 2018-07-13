pipeline {
    agent any

    environment {
        repo = "docker.adeo.no:5000"
        fasit_env = 't1'
        application_namespace = 't1'
        fasit_env_test = 't8'
        application_namespace_test = 't8'
        zone = 'fss'
    }

    stages {
        stage('Initialize') {
            steps {
                script {
                    app_name = sh(script: " sh gradlew properties | grep ^name: | sed 's/name: //'", returnStdout: true).trim()
                    version = sh(script: "sh gradlew properties | grep ^version: | sed 's/version: //'", returnStdout: true).trim()
                    branchName = "${env.BRANCH_NAME}"
                    if (branchName != "master") {
                        version = "${version}.${branchName.replaceAll('/', '-')}.${env.BUILD_ID}"
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
                    deployUtils.buildAndPushDockerImage(repo, applicationFullName)
                }
            }
        }

        stage('Deploy') {
            steps {
                script {
                    echo "Deploy '${branchName}'?"
                    if (branchName.startsWith('feature')) {
                        echo "\tdeploying to t8 (test)"
                        deployUtils.naisDeploy(app_name, version, fasit_env_test, application_namespace_test, zone)
                    } else if (branchName == 'master') {
                        echo "\tdeploying to t8 (master)"
                        deployUtils.naisDeploy(app_name, version, fasit_env, application_namespace, zone)
                    } else {
                        echo "Skipping deploy"
                    }
                }
            }
        }
    }
}