pipeline {
    agent any
    tools {
        maven 'Maven 3.3.9'
    }
    stages {
        stage('Build') {
            steps {
                sh 'mvn build -DskipTests'
            }
        }
        stage('Test'){
            steps {
                sh 'mvn verify'
            }
        }
    }
}