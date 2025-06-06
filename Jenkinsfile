pipeline {
    agent any

    tools {
        maven 'MVN'
        jdk 'JDK_17'
    }

    environment {
        DOCKERHUB_USER = 'diegozm'
        DOCKER_CREDENTIALS_ID = 'password'
        SERVICES = 'service-discovery cloud-config api-gateway user-service favourite-service order-service product-service proxy-client locust'
        K8S_NAMESPACE = 'default'
        BUILD_VERSION =  '0.1.0'
        DOCKER_REGISTRY  = 'diegozm'
    }

    stages {

        stage('Init') {
            steps {
                script {
                    def profileConfig = [
                        master : ['prod', '-prod'],
                        stage  : ['stage', '-stage']
                    ]
                    def config = profileConfig[env.BRANCH_NAME] ?: ['dev', '-dev']

                    env.SPRING_PROFILES_ACTIVE = config[0]
                    env.IMAGE_TAG = config[0]
                    env.DEPLOYMENT_SUFFIX = config[1]

                    echo "📦 Branch: ${env.BRANCH_NAME}"
                    echo "🌱 Spring profile: ${env.SPRING_PROFILES_ACTIVE}"
                    echo "🏷️ Image tag: ${env.IMAGE_TAG}"
                    echo "📂 Deployment suffix: ${env.DEPLOYMENT_SUFFIX}"
                }
            }
        }


        stage('Ensure Namespace') {
            steps {
                bat "kubectl get namespace ${K8S_NAMESPACE} || kubectl create namespace ${K8S_NAMESPACE}"
            }
        }

        stage('Checkout') {
            steps {
                git branch: "${env.BRANCH_NAME}", url: 'https://github.com/DiegoZM1006/ecommerce-microservice-backend-app.git'
            }
        }

        stage('Verify Tools') {
            steps {
                bat 'java -version'
                bat 'mvn -version'
                bat 'docker --version'
                bat 'kubectl config current-context'
            }
        }

        stage('Build & Package') {
            when { anyOf { branch 'stage'; branch 'master'; branch 'dev'; } }
            steps {
                bat "mvn clean package -DskipTests"
            }
        }

        stage('Build & Push Docker Images') {
            when { anyOf { branch 'stage'; branch 'master' } }
            steps {
                withCredentials([string(credentialsId: "${DOCKER_CREDENTIALS_ID}", variable: 'credential')]) {
                    bat "echo ${credential} | docker login -u ${DOCKERHUB_USER} --password-stdin"

                    script {
                        SERVICES.split().each { service ->
                            bat "docker build -t ${DOCKERHUB_USER}/${service}:${IMAGE_TAG} .\\${service}"
                            bat "docker push ${DOCKERHUB_USER}/${service}:${IMAGE_TAG}"
                        }
                    }
                }
            }
        }

        stage('Unit Tests') {
            when {
                anyOf {
                    branch 'dev'; branch 'stage';
                    expression { env.BRANCH_NAME.startsWith('feature/') }
                }
            }
            steps {
                script {
                    ['payment-service'].each {
                        bat "mvn test -pl ${it}"
                    }
                }
            }
        }

        stage('Integration Tests') {
            when {
                anyOf {
                    branch 'dev';
                    expression { env.BRANCH_NAME.startsWith('feature/') }
                }
            }
            steps {
                script {
                    ['payment-service'].each {
                        bat "mvn verify -pl ${it}"
                    }
                }
            }
        }

        stage('E2E Tests') {
            when {
                anyOf {
                    branch 'stage';
                }
            }
            steps {
                bat '''        
                echo "Ejecutando pruebas E2E..."
                cd e2e-tests
                newman run "E2E-tests.json"
                cd ..
                '''
            }
        }

        stage('Levantar contenedores para pruebas') {
            when {
                anyOf {
                    branch 'stage'
                }
            }
            steps {
                script {
                    bat '''

                    docker network create ecommerce-test || true

                    echo 🚀 Levantando ZIPKIN...
                    docker run -d --name zipkin-container --network ecommerce-test -p 9411:9411 openzipkin/zipkin

                    echo 🚀 Levantando EUREKA...
                    docker run -d --name service-discovery-container --network ecommerce-test -p 8761:8761 ^
                        -e SPRING_PROFILES_ACTIVE=dev ^
                        -e SPRING_ZIPKIN_BASE_URL=http://zipkin-container:9411 ^
                        darwinl06/service-discovery:%IMAGE_TAG%

                    call :waitForService http://localhost:8761/actuator/health

                    echo 🚀 Levantando CLOUD-CONFIG...
                    docker run -d --name cloud-config-container --network ecommerce-test -p 9296:9296 ^
                        -e SPRING_PROFILES_ACTIVE=dev ^
                        -e SPRING_ZIPKIN_BASE_URL=http://zipkin-container:9411 ^
                        -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://service-discovery-container:8761/eureka/ ^
                        -e EUREKA_INSTANCE=cloud-config-container ^
                        darwinl06/cloud-config:%IMAGE_TAG%

                    call :waitForService http://localhost:9296/actuator/health

                    call :runService order-service 8300
                    call :runService payment-service 8400
                    call :runService product-service 8500
                    call :runService shipping-service 8600
                    call :runService user-service 8700
                    call :runService favourite-service 8800

                    echo ✅ Todos los contenedores están arriba y saludables.
                    exit /b 0

                    :runService
                    set "NAME=%~1"
                    set "PORT=%~2"
                    echo 🚀 Levantando %NAME%...
                    docker run -d --name %NAME%-container --network ecommerce-test -p %PORT%:%PORT% ^
                        -e SPRING_PROFILES_ACTIVE=dev ^
                        -e SPRING_ZIPKIN_BASE_URL=http://zipkin-container:9411 ^
                        -e SPRING_CONFIG_IMPORT=optional:configserver:http://cloud-config-container:9296 ^
                        -e EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://service-discovery-container:8761/eureka ^
                        -e EUREKA_INSTANCE=%NAME%-container ^
                        darwinl06/%NAME%:%IMAGE_TAG%
                    call :waitForService http://localhost:%PORT%/%NAME%/actuator/health
                    exit /b 0

                    :waitForService
                    set "URL=%~1"
                    echo ⏳ Esperando a que %URL% esté disponible...
                    :wait_loop
                    for /f "delims=" %%i in ('curl -s %URL% ^| jq -r ".status"') do (
                        if "%%i"=="UP" goto :eof
                    )
                    timeout /t 5 /nobreak
                    goto wait_loop
                    '''
                }
            }
        }

        stage('Run Load Tests with Locust') {
            when {
                anyOf {
                    branch 'stage'
                }
            }
            steps {
                script {
                    bat '''
                    echo 🚀 Levantando Locust para order-service...

                    docker run --rm --network ecommerce-test ^
                      -v "%CD%\\locust:/mnt" ^
                      -v "%CD%\\locust-results:/app" ^
                      darwinl06/locust:%IMAGE_TAG% ^
                      -f /mnt/test/order-service/locustfile.py ^
                      --host http://order-service-container:8300 ^
                      --headless -u 10 -r 2 -t 1m ^
                      --csv order-service-stats --csv-full-history

                    echo 🚀 Levantando Locust para payment-service...

                    docker run --rm --network ecommerce-test ^
                      -v "%CD%\\locust:/mnt" ^
                      -v "%CD%\\locust-results:/app" ^
                      darwinl06/locust:%IMAGE_TAG% ^
                      -f /mnt/test/payment-service/locustfile.py ^
                      --host http://payment-service-container:8400 ^
                      --headless -u 10 -r 1 -t 1m ^
                      --csv payment-service-stats --csv-full-history

                    echo 🚀 Levantando Locust para favourite-service...

                    docker run --rm --network ecommerce-test ^
                      -v "%CD%\\locust:/mnt" ^
                      -v "%CD%\\locust-results:/app" ^
                      darwinl06/locust:%IMAGE_TAG% ^
                      -f /mnt/test/favourite-service/locustfile.py ^
                      --host http://favourite-service-container:8800 ^
                      --headless -u 10 -r 2 -t 1m ^
                      --csv favourite-service-stats --csv-full-history

                    echo ✅ Pruebas completadas
                    '''
                }
            }
        }

        stage('Run Stress Tests with Locust') {
            when {
                anyOf {
                    branch 'stage'
                }
            }
            steps {
                script {
                    bat '''
                    echo 🔥 Levantando Locust para prueba de estrés...

                    docker run --rm --network ecommerce-test ^
                    -v "%CD%\\locust:/mnt" ^
                    -v "%CD%\\locust-results:/app" ^
                    darwinl06/locust:%IMAGE_TAG% ^
                    -f /mnt/test/order-service/locustfile.py ^
                    --host http://order-service-container:8300 ^
                    --headless -u 50 -r 5 -t 1m ^
                    --csv order-service-stress --csv-full-history

                    docker run --rm --network ecommerce-test ^
                    -v "%CD%\\locust:/mnt" ^
                    -v "%CD%\\locust-results:/app" ^
                    darwinl06/locust:%IMAGE_TAG% ^
                    -f /mnt/test/payment-service/locustfile.py ^
                    --host http://payment-service-container:8400 ^
                    --headless -u 50 -r 5 -t 1m ^
                    --csv payment-service-stress --csv-full-history

                    docker run --rm --network ecommerce-test ^
                    -v "%CD%\\locust:/mnt" ^
                    -v "%CD%\\locust-results:/app" ^
                    darwinl06/locust:%IMAGE_TAG% ^
                    -f /mnt/test/favourite-service/locustfile.py ^
                    --host http://favourite-service-container:8800 ^
                    --headless -u 50 -r 5 -t 1m ^
                    --csv favourite-service-stress --csv-full-history

                    echo ✅ Pruebas de estrés completadas
                    '''
                }
            }
        }



        stage('Detener y eliminar contenedores') {
            when {
                anyOf {
                    branch 'stage'
                    expression { env.BRANCH_NAME.startsWith('feature/') }
                }
            }
            steps {
                script {
                    bat """
                    echo 🛑 Deteniendo y eliminando contenedores...

                    docker rm -f locust || exit 0
                    docker rm -f favourite-service-container || exit 0
                    docker rm -f user-service-container || exit 0
                    docker rm -f shipping-service-container || exit 0
                    docker rm -f product-service-container || exit 0
                    docker rm -f payment-service-container || exit 0
                    docker rm -f order-service-container || exit 0
                    docker rm -f cloud-config-container || exit 0
                    docker rm -f service-discovery-container || exit 0
                    docker rm -f zipkin-container || exit 0

                    echo 🧹 Todos los contenedores eliminados
                    """
                }
            }
        }
        
        stage('Deploy Common Config') {
            when { anyOf { branch 'master' } }
            steps {
                bat "kubectl apply -f k8s\\common-config.yaml -n ${K8S_NAMESPACE}"
            }
        }        
        
        stage('Deploy Core Services') {
            when { anyOf { branch 'master' } }
            steps {
                bat "kubectl apply -f k8s\\zipkin -n ${K8S_NAMESPACE}"
                bat "kubectl rollout status deployment/zipkin -n ${K8S_NAMESPACE} --timeout=200s"

                bat "kubectl apply -f k8s\\service-discovery -n ${K8S_NAMESPACE}"
                bat "kubectl set image deployment/service-discovery service-discovery=${DOCKERHUB_USER}/service-discovery:${IMAGE_TAG} -n ${K8S_NAMESPACE}"
                bat "kubectl set env deployment/service-discovery SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE} -n ${K8S_NAMESPACE}"
                bat "kubectl rollout status deployment/service-discovery -n ${K8S_NAMESPACE} --timeout=200s"

                bat "kubectl apply -f k8s\\cloud-config -n ${K8S_NAMESPACE}"
                bat "kubectl set image deployment/cloud-config cloud-config=${DOCKERHUB_USER}/cloud-config:${IMAGE_TAG} -n ${K8S_NAMESPACE}"
                bat "kubectl set env deployment/cloud-config SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE} -n ${K8S_NAMESPACE}"
                bat "kubectl rollout status deployment/cloud-config -n ${K8S_NAMESPACE} --timeout=300s"
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    echo "Deploying to Kubernetes namespace: ${NAMESPACE}"
                    bat '''
                        # Create namespace if it doesn't exist
                        kubectl create namespace ${K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

                        # Apply common configurations
                        kubectl apply -f k8s/common-config.yaml -n ${K8S_NAMESPACE}

                        # Deploy infrastructure services first
                        echo "Deploying infrastructure services..."

                        # Update image tags in deployment files
                        sed -i "s|image: .*service-discovery.*|image: ${DOCKER_REGISTRY}/service-discovery:${BUILD_VERSION}|g" k8s/service-discovery/deployment.yaml
                        sed -i "s|image: .*cloud-config.*|image: ${DOCKER_REGISTRY}/cloud-config:${BUILD_VERSION}|g" k8s/cloud-config/deployment.yaml

                        kubectl apply -f k8s/zipkin/ -n ${K8S_NAMESPACE}
                        kubectl apply -f k8s/service-discovery/ -n ${K8S_NAMESPACE}
                        kubectl apply -f k8s/cloud-config/ -n ${K8S_NAMESPACE}

                        # Wait for infrastructure services
                        kubectl wait --for=condition=ready pod -l app=service-discovery -n ${K8S_NAMESPACE} --timeout=300s
                        kubectl wait --for=condition=ready pod -l app=cloud-config -n ${K8S_NAMESPACE} --timeout=300s

                        echo "Infrastructure services deployed successfully"

                        # Deploy API Gateway
                        sed -i "s|image: .*api-gateway.*|image: ${DOCKER_REGISTRY}/api-gateway:${BUILD_VERSION}|g" k8s/api-gateway/deployment.yaml
                        kubectl apply -f k8s/api-gateway/ -n ${K8S_NAMESPACE}

                        # Deploy business services
                        echo "Deploying business services..."

                        # Update image tags for business services
                        sed -i "s|image: .*payment-service.*|image: ${DOCKER_REGISTRY}/payment-service:${BUILD_VERSION}|g" k8s/payment-service/deployment.yaml
                        sed -i "s|image: .*order-service.*|image: ${DOCKER_REGISTRY}/order-service:${BUILD_VERSION}|g" k8s/order-service/deployment.yaml
                        sed -i "s|image: .*favourite-service.*|image: ${DOCKER_REGISTRY}/favourite-service:${BUILD_VERSION}|g" k8s/favourite-service/deployment.yaml
                        sed -i "s|image: .*product-service.*|image: ${DOCKER_REGISTRY}/product-service:${BUILD_VERSION}|g" k8s/product-service/deployment.yaml
                        sed -i "s|image: .*user-service.*|image: ${DOCKER_REGISTRY}/user-service:${BUILD_VERSION}|g" k8s/user-service/deployment.yaml
                        sed -i "s|image: .*shipping-service.*|image: ${DOCKER_REGISTRY}/shipping-service:${BUILD_VERSION}|g" k8s/shipping-service/deployment.yaml
                        sed -i "s|image: .*proxy-client.*|image: ${DOCKER_REGISTRY}/proxy-client:${BUILD_VERSION}|g" k8s/proxy-client/deployment.yaml

                        kubectl apply -f k8s/payment-service/ -n ${K8S_NAMESPACE}
                        kubectl apply -f k8s/order-service/ -n ${K8S_NAMESPACE}
                        kubectl apply -f k8s/favourite-service/ -n ${K8S_NAMESPACE}
                        kubectl apply -f k8s/product-service/ -n ${K8S_NAMESPACE}
                        kubectl apply -f k8s/user-service/ -n ${K8S_NAMESPACE}
                        kubectl apply -f k8s/shipping-service/ -n ${K8S_NAMESPACE}
                        kubectl apply -f k8s/proxy-client/ -n ${K8S_NAMESPACE}

                        echo "All services deployed successfully"
                    '''
                }
            }
        }     
        
        stage('Generate and Archive Release Notes') {
            when {
                branch 'master'
            }
            steps {
                bat '''
                echo "📝 Generando Release Notes con convco..."
                convco changelog > RELEASE_NOTES.md
                '''
                archiveArtifacts artifacts: 'RELEASE_NOTES.md', fingerprint: true
            }
        }
    }

    post {
        success {
            echo "✅ Pipeline OK (${env.BRANCH_NAME}) - ${SPRING_PROFILES_ACTIVE}"
        }
        failure {
            echo "❌ Falló pipeline en ${env.BRANCH_NAME}. Ver logs."
        }
        unstable {
            echo "⚠️ Finalizó con advertencias en ${env.BRANCH_NAME}"
        }
    }
}
