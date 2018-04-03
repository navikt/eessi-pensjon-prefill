
pipeline {
    agent any
    tools {
        maven('Maven 3.3.9')
    }

    environment {
        repo = "docker.adeo.no:5000"
        FASIT_ENV = 't1'
        APPLICATION_NAMESPACE = 't1'
        ZONE = 'fss'
    }

    stages {
        stage('Initialize') {
            steps {
                script {
                    pom = readMavenPom(file: 'pom.xml')
                    app_name = pom.artifactId
                    version = pom.version
                    if (version.endsWith("-SNAPSHOT")) {
                        commitHashShort = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                        version = "${pom.version}.${env.BUILD_ID}-${commitHashShort}"
                    }
                    applicationFullName = "${app_name}:${version}"
                }
            }
        }

        stage('Build') {
            steps {
                sh('mvn -DskipTests clean install')
            }
        }

        stage('Test') {
            steps {
                sh('mvn -Dmaven.test.failure.ignore=true verify')
            }
            post {
                success {
                    junit('target/surefire-reports/**/*.xml')
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
                    deploy.naisDeploy(app_name, version, 'u89', 'u89', 'fss')
                }
            }
        }
    }
}