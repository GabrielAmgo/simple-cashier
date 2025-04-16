pipeline {
    agent any

    environment {
        DOCKER_HUB_CREDS = credentials('docker-hub-credentials')
        KUBE_CONFIG = credentials('kubeconfig')
        IMAGE_NAME = 'gabrielamg/simple-cashier'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Docker Build & Push') {
            steps {
                sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} ."
                sh "echo ${DOCKER_HUB_CREDS_PSW} | docker login -u ${DOCKER_HUB_CREDS_USR} --password-stdin"
                sh "docker push ${IMAGE_NAME}:${IMAGE_TAG}"
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                sh "sed -i 's|gabrielamg/simple-cashier:1.0|${IMAGE_NAME}:${IMAGE_TAG}|g' kubernetes/deployment.yaml"
                sh "kubectl apply -f kubernetes/deployment.yaml"
                sh "kubectl apply -f kubernetes/redis.yaml"
                sh "kubectl apply -f kubernetes/prometheus.yaml"
                sh "kubectl apply -f kubernetes/grafana.yaml"
            }
        }
    }

    post {
        always {
            sh "docker logout"
            cleanWs()
        }
    }
}