#!/usr/bin/env groovy

pipeline {
    agent any
    tools {
        jdk "java8"
    }
    stages {
        stage('Clean') {
            steps {
                echo 'Cleaning Project'
                sh 'chmod +x gradlew'
                sh './gradlew clean'
            }
        }

        stage('Build') {
            steps {
                echo 'Building'
                sh './gradlew build curseforge'
            }
        }

        stage('Archive artifacts') {
            steps {
                echo 'Archive'
                archiveArtifacts 'build/libs*/*jar'
            }
        }

        stage('Publish artifacts') {
            steps {
                echo 'Publishing'
                sh './gradlew publish'
            }
        }
    }
}