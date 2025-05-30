#!/usr/bin/env groovy

pipeline {
    agent any

    tools {
        maven 'MVN'
        jdk 'JDK_17'
    }

    environment {
        MAVEN_OPTS = '-Dmaven.test.failure.ignore=false'
        DOCKER_REGISTRY = 'diegozm'
        KUBECONFIG = "${WORKSPACE}/.kube/config"
        NAMESPACE = 'staging'
        ENVIRONMENT = 'stage'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 60, unit: 'MINUTES')
        retry(1)
        skipStagesAfterUnstable()
    }

    stages {
        stage('Environment Setup') {
            steps {
                script {
                    echo 'Building pipeline in staging environment'
                    echo "Environment: ${ENVIRONMENT}"
                    echo "Namespace: ${NAMESPACE}"

                    // Clean workspace
                    cleanWs()
                }
            }
        }

        stage('Checkout') {
            steps {
                git branch: "${env.BRANCH_NAME}", url: 'https://github.com/DiegoZM1006/ecommerce-microservice-backend-app.git'
            }
        }

        // stage('Code Quality Analysis') {
        //     parallel {
        //         stage('Security Scan') {
        //             steps {
        //                 script {
        //                     try {
        //                         // OWASP Dependency Check
        //                         bat 'mvn org.owasp:dependency-check-maven:check'
        //                     } catch (Exception e) {
        //                         echo "Security scan failed: ${e.getMessage()}"
        //                         currentBuild.result = 'UNSTABLE'
        //                     }
        //                 }
        //             }
        //             post {
        //                 always {
        //                     publishHTML([
        //                         allowMissing: true,
        //                         alwaysLinkToLastBuild: true,
        //                         keepAll: true,
        //                         reportDir: 'target/dependency-check-report',
        //                         reportFiles: 'dependency-check-report.html',
        //                         reportName: 'OWASP Dependency Check Report'
        //                     ])
        //                 }
        //             }
        //         }
                
        //         stage('Static Code Analysis') {
        //             steps {
        //                 script {
        //                     try {
        //                         // SpotBugs analysis
        //                         bat 'mvn compile spotbugs:check'
        //                     } catch (Exception e) {
        //                         echo "Static analysis failed: ${e.getMessage()}"
        //                         currentBuild.result = 'UNSTABLE'
        //                     }
        //                 }
        //             }
        //         }
        //     }
        // }

        stage('Build Services') {
            steps {
                script {
                    echo 'Building all microservices...'
                    bat '''
                        mvn clean compile -DskipTests
                        echo "Build completed successfully"
                    '''
                }
            }
        }

        // stage('Unit Tests') {
        //     steps {
        //         bat '''
        //         # Configurar JAVA_HOME para Java 11
        //         export JAVA_HOME=$HOME/java11
        //         export PATH=$HOME/java11/bin:$HOME/bin:$HOME/maven/bin:$HOME/nodejs/bin:$PATH
    
        //         echo "Verificando versión de Java para Maven:"
        //         java -version
                
        //         echo "Ejecutando pruebas unitarias en el servicio de productos"
        //         cd payment-service
    
        //         # Limpiar target anterior
        //         rm -rf target/
    
        //         # Usar Maven con Java 11
        //         mvn clean test -Dmaven.compiler.source=11 -Dmaven.compiler.target=11 -Dmaven.test.failure.ignore=true
    
        //         cd ..
        //         '''
        //     }
        //     post {
        //         always {
        //             publishTestResults testResultsPattern: '**/target/surefire-reports/*.xml'
        //             publishHTML([
        //                 allowMissing: true,
        //                 alwaysLinkToLastBuild: true,
        //                 keepAll: true,
        //                 reportDir: 'target/site/jacoco',
        //                 reportFiles: 'index.html',
        //                 reportName: 'Code Coverage Report'
        //             ])
        //         }
        //     }
        // }

        stage('Package Services') {
            steps {
                script {
                    echo 'Packaging all services...'
                    bat '''
                        mvn package -DskipTests
                    '''
                }
            }
        }

        // stage('Integration Tests') {
        //     steps {
        //         script {
        //             echo 'Running integration tests...'
        //             try {
        //                 bat '''
        //                     # Start test containers
        //                     docker-compose -f compose.yml up -d --build
        //                     sleep 30

        //                     # Wait for services to be ready
        //                     for i in {1..30}; do
        //                         if curl -f http://localhost:8762/actuator/health; then
        //                             echo "Service Discovery is ready"
        //                             break
        //                         fi
        //                         echo "Waiting for services to start... ($i/30)"
        //                         sleep 10
        //                     done

        //                     # Run integration tests
        //                     mvn test -Dtest.profile=integration
        //                 '''
        //             } catch (Exception e) {
        //                 echo "Integration tests failed: ${e.getMessage()}"
        //                 currentBuild.result = 'UNSTABLE'
        //             } finally {
        //                 bat 'docker-compose -f compose.yml down -v || true'
        //             }
        //         }
        //     }
        //     post {
        //         always {
        //             publishTestResults testResultsPattern: '**/target/failsafe-reports/*.xml'
        //         }
        //     }
        // }

        // stage('E2E Tests') {
        //     steps {
        //         script {
        //             echo 'Running E2E tests with Newman...'
        //             try {
        //                 bat '''
        //                     # Start application stack
        //                     docker-compose -f compose.yml up -d --build
        //                     sleep 45

        //                     # Wait for API Gateway
        //                     for i in {1..30}; do
        //                         if curl -f http://localhost:8762/actuator/health; then
        //                             echo "API Gateway is ready"
        //                             break
        //                         fi
        //                         echo "Waiting for API Gateway... ($i/30)"
        //                         sleep 10
        //                     done

        //                     # Install Newman if not present
        //                     npm install -g newman || true

        //                     # Run E2E tests
        //                     newman run e2e-tests/E2E-tests.json \\
        //                         --environment e2e-tests/environment.json \\
        //                         --reporters cli,htmlextra \\
        //                         --reporter-htmlextra-export newman-report.html \\
        //                         --bail || echo "E2E tests completed with warnings"
        //                 '''
        //             } catch (Exception e) {
        //                 echo "E2E tests failed: ${e.getMessage()}"
        //                 currentBuild.result = 'UNSTABLE'
        //             } finally {
        //                 bat 'docker-compose -f compose.yml down -v || true'
        //             }
        //         }
        //     }
        //     post {
        //         always {
        //             publishHTML([
        //                 allowMissing: true,
        //                 alwaysLinkToLastBuild: true,
        //                 keepAll: true,
        //                 reportDir: '.',
        //                 reportFiles: 'newman-report.html',
        //                 reportName: 'E2E Test Report'
        //             ])
        //         }
        //     }
        // }

        // stage('Load Tests') {
        //     steps {
        //         script {
        //             echo 'Running load tests with Locust...'
        //             try {
        //                 bat '''
        //                     cd locust

        //                     # Start application stack for load testing
        //                     docker-compose -f ../compose.yml up -d --build
        //                     sleep 60

        //                     # Install Locust requirements
        //                     pip3 install -r requirements.txt || pip install -r requirements.txt

        //                     # Run load tests
        //                     locust -f locustfile.py \\
        //                         --host=http://localhost:8762 \\
        //                         --users=50 \\
        //                         --spawn-rate=5 \\
        //                         --run-time=5m \\
        //                         --headless \\
        //                         --html=load-test-report.html \\
        //                         --csv=load-test-results || echo "Load tests completed"

        //                     # Also run individual service tests
        //                     echo "Running individual service load tests..."

        //                     locust -f test/payment-service/locustfile.py \\
        //                         --host=http://localhost:8762 \\
        //                         --users=20 \\
        //                         --spawn-rate=2 \\
        //                         --run-time=2m \\
        //                         --headless \\
        //                         --html=payment-load-test.html || echo "Payment service load test completed"

        //                     locust -f test/order-service/locustfile.py \\
        //                         --host=http://localhost:8762 \\
        //                         --users=20 \\
        //                         --spawn-rate=2 \\
        //                         --run-time=2m \\
        //                         --headless \\
        //                         --html=order-load-test.html || echo "Order service load test completed"

        //                     locust -f test/favourite-service/locustfile.py \\
        //                         --host=http://localhost:8762 \\
        //                         --users=20 \\
        //                         --spawn-rate=2 \\
        //                         --run-time=2m \\
        //                         --headless \\
        //                         --html=favourite-load-test.html || echo "Favourite service load test completed"
        //                 '''
        //             } catch (Exception e) {
        //                 echo "Load tests failed: ${e.getMessage()}"
        //                 currentBuild.result = 'UNSTABLE'
        //             } finally {
        //                 bat 'docker-compose -f compose.yml down -v || true'
        //             }
        //         }
        //     }
        //     post {
        //         always {
        //             publishHTML([
        //                 allowMissing: true,
        //                 alwaysLinkToLastBuild: true,
        //                 keepAll: true,
        //                 reportDir: 'locust',
        //                 reportFiles: '*.html',
        //                 reportName: 'Load Test Reports'
        //             ])
        //         }
        //     }
        // }

        stage('Build Docker Images') {
            parallel {
                stage('Infrastructure Images') {
                    steps {
                        script {
                            echo 'Building infrastructure service images...'
                            bat '''
                                # Build infrastructure services
                                docker build -t ${DOCKER_REGISTRY}/service-discovery:${BUILD_VERSION} service-discovery/
                                docker build -t ${DOCKER_REGISTRY}/cloud-config:${BUILD_VERSION} cloud-config/
                                docker build -t ${DOCKER_REGISTRY}/api-gateway:${BUILD_VERSION} api-gateway/

                                # Tag latest for current environment
                                docker tag ${DOCKER_REGISTRY}/service-discovery:${BUILD_VERSION} ${DOCKER_REGISTRY}/service-discovery:${ENVIRONMENT}-latest
                                docker tag ${DOCKER_REGISTRY}/cloud-config:${BUILD_VERSION} ${DOCKER_REGISTRY}/cloud-config:${ENVIRONMENT}-latest
                                docker tag ${DOCKER_REGISTRY}/api-gateway:${BUILD_VERSION} ${DOCKER_REGISTRY}/api-gateway:${ENVIRONMENT}-latest
                            '''
                        }
                    }
                }

                stage('Business Services Images') {
                    steps {
                        script {
                            echo 'Building business service images...'
                            bat '''
                                # Build business services
                                docker build -t ${DOCKER_REGISTRY}/payment-service:${BUILD_VERSION} payment-service/
                                docker build -t ${DOCKER_REGISTRY}/order-service:${BUILD_VERSION} order-service/
                                docker build -t ${DOCKER_REGISTRY}/favourite-service:${BUILD_VERSION} favourite-service/
                                docker build -t ${DOCKER_REGISTRY}/product-service:${BUILD_VERSION} product-service/
                                docker build -t ${DOCKER_REGISTRY}/user-service:${BUILD_VERSION} user-service/
                                docker build -t ${DOCKER_REGISTRY}/shipping-service:${BUILD_VERSION} shipping-service/
                                docker build -t ${DOCKER_REGISTRY}/proxy-client:${BUILD_VERSION} proxy-client/

                                # Tag latest for current environment
                                docker tag ${DOCKER_REGISTRY}/payment-service:${BUILD_VERSION} ${DOCKER_REGISTRY}/payment-service:${ENVIRONMENT}-latest
                                docker tag ${DOCKER_REGISTRY}/order-service:${BUILD_VERSION} ${DOCKER_REGISTRY}/order-service:${ENVIRONMENT}-latest
                                docker tag ${DOCKER_REGISTRY}/favourite-service:${BUILD_VERSION} ${DOCKER_REGISTRY}/favourite-service:${ENVIRONMENT}-latest
                                docker tag ${DOCKER_REGISTRY}/product-service:${BUILD_VERSION} ${DOCKER_REGISTRY}/product-service:${ENVIRONMENT}-latest
                                docker tag ${DOCKER_REGISTRY}/user-service:${BUILD_VERSION} ${DOCKER_REGISTRY}/user-service:${ENVIRONMENT}-latest
                                docker tag ${DOCKER_REGISTRY}/shipping-service:${BUILD_VERSION} ${DOCKER_REGISTRY}/shipping-service:${ENVIRONMENT}-latest
                                docker tag ${DOCKER_REGISTRY}/proxy-client:${BUILD_VERSION} ${DOCKER_REGISTRY}/proxy-client:${ENVIRONMENT}-latest
                            '''
                        }
                    }
                }
            }
        }

        stage('Push to Registry') {
            steps {
                script {
                    echo 'Pushing images to registry...'
                    bat '''
                        # Push infrastructure services
                        docker push ${DOCKER_REGISTRY}/service-discovery:${BUILD_VERSION}
                        docker push ${DOCKER_REGISTRY}/cloud-config:${BUILD_VERSION}
                        docker push ${DOCKER_REGISTRY}/api-gateway:${BUILD_VERSION}

                        # Push business services
                        docker push ${DOCKER_REGISTRY}/payment-service:${BUILD_VERSION}
                        docker push ${DOCKER_REGISTRY}/order-service:${BUILD_VERSION}
                        docker push ${DOCKER_REGISTRY}/favourite-service:${BUILD_VERSION}
                        docker push ${DOCKER_REGISTRY}/product-service:${BUILD_VERSION}
                        docker push ${DOCKER_REGISTRY}/user-service:${BUILD_VERSION}
                        docker push ${DOCKER_REGISTRY}/shipping-service:${BUILD_VERSION}
                        docker push ${DOCKER_REGISTRY}/proxy-client:${BUILD_VERSION}

                        # Push latest tags
                        docker push ${DOCKER_REGISTRY}/service-discovery:${ENVIRONMENT}-latest
                        docker push ${DOCKER_REGISTRY}/cloud-config:${ENVIRONMENT}-latest
                        docker push ${DOCKER_REGISTRY}/api-gateway:${ENVIRONMENT}-latest
                        docker push ${DOCKER_REGISTRY}/payment-service:${ENVIRONMENT}-latest
                        docker push ${DOCKER_REGISTRY}/order-service:${ENVIRONMENT}-latest
                        docker push ${DOCKER_REGISTRY}/favourite-service:${ENVIRONMENT}-latest
                        docker push ${DOCKER_REGISTRY}/product-service:${ENVIRONMENT}-latest
                        docker push ${DOCKER_REGISTRY}/user-service:${ENVIRONMENT}-latest
                        docker push ${DOCKER_REGISTRY}/shipping-service:${ENVIRONMENT}-latest
                        docker push ${DOCKER_REGISTRY}/proxy-client:${ENVIRONMENT}-latest
                    '''
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    echo "Deploying to Kubernetes namespace: ${NAMESPACE}"
                    bat '''
                        # Create namespace if it doesn't exist
                        kubectl create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

                        # Apply common configurations
                        kubectl apply -f k8s/common-config.yaml -n ${NAMESPACE}

                        # Deploy infrastructure services first
                        echo "Deploying infrastructure services..."

                        # Update image tags in deployment files
                        sed -i "s|image: .*service-discovery.*|image: ${DOCKER_REGISTRY}/service-discovery:${BUILD_VERSION}|g" k8s/service-discovery/deployment.yaml
                        sed -i "s|image: .*cloud-config.*|image: ${DOCKER_REGISTRY}/cloud-config:${BUILD_VERSION}|g" k8s/cloud-config/deployment.yaml

                        kubectl apply -f k8s/zipkin/ -n ${NAMESPACE}
                        kubectl apply -f k8s/service-discovery/ -n ${NAMESPACE}
                        kubectl apply -f k8s/cloud-config/ -n ${NAMESPACE}

                        # Wait for infrastructure services
                        kubectl wait --for=condition=ready pod -l app=service-discovery -n ${NAMESPACE} --timeout=300s
                        kubectl wait --for=condition=ready pod -l app=cloud-config -n ${NAMESPACE} --timeout=300s

                        echo "Infrastructure services deployed successfully"

                        # Deploy API Gateway
                        sed -i "s|image: .*api-gateway.*|image: ${DOCKER_REGISTRY}/api-gateway:${BUILD_VERSION}|g" k8s/api-gateway/deployment.yaml
                        kubectl apply -f k8s/api-gateway/ -n ${NAMESPACE}

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

                        kubectl apply -f k8s/payment-service/ -n ${NAMESPACE}
                        kubectl apply -f k8s/order-service/ -n ${NAMESPACE}
                        kubectl apply -f k8s/favourite-service/ -n ${NAMESPACE}
                        kubectl apply -f k8s/product-service/ -n ${NAMESPACE}
                        kubectl apply -f k8s/user-service/ -n ${NAMESPACE}
                        kubectl apply -f k8s/shipping-service/ -n ${NAMESPACE}
                        kubectl apply -f k8s/proxy-client/ -n ${NAMESPACE}

                        echo "All services deployed successfully"
                    '''
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                script {
                    echo 'Verifying deployment...'
                    bat '''
                        # Wait for all deployments to be ready
                        echo "Waiting for deployments to be ready..."

                        kubectl wait --for=condition=available deployment --all -n ${NAMESPACE} --timeout=600s

                        # Check service health
                        echo "Checking service health..."

                        # Port forward to check services
                        kubectl port-forward svc/api-gateway 8762:8080 -n ${NAMESPACE} &
                        PF_PID=$!
                        sleep 10

                        # Health checks
                        for i in {1..30}; do
                            if curl -f http://localhost:8762/actuator/health; then
                                echo "API Gateway health check passed"
                                break
                            fi
                            echo "Waiting for API Gateway... ($i/30)"
                            sleep 10
                        done

                        # Check individual services through API Gateway
                        services=("payment-service" "order-service" "favourite-service" "product-service" "user-service" "shipping-service")

                        for service in "${services[@]}"; do
                            echo "Checking $service health..."
                            curl -f "http://localhost:8762/api/$service/actuator/health" || echo "$service health check failed"
                        done

                        # Cleanup port forward
                        kill $PF_PID || true

                        echo "Deployment verification completed"

                        # Show deployment status
                        kubectl get pods -n ${NAMESPACE}
                        kubectl get services -n ${NAMESPACE}
                    '''
                }
            }
        }

        stage('Smoke Tests') {
            steps {
                script {
                    echo 'Running smoke tests on deployed services...'
                    bat '''
                        # Port forward for smoke tests
                        kubectl port-forward svc/api-gateway 8762:8080 -n ${NAMESPACE} &
                        PF_PID=$!
                        sleep 10

                        # Basic smoke tests
                        echo "Running basic smoke tests..."

                        # Test API Gateway
                        curl -f http://localhost:8762/actuator/health

                        # Test service endpoints
                        curl -f http://localhost:8762/api/payment-service/payments || echo "Payment service test failed"
                        curl -f http://localhost:8762/api/order-service/orders || echo "Order service test failed"
                        curl -f http://localhost:8762/api/favourite-service/favourites || echo "Favourite service test failed"

                        # Cleanup
                        kill $PF_PID || true

                        echo "Smoke tests completed"
                    '''
                }
            }
        }
    }

    post {
        always {
            script {
                echo 'Pipeline execution completed'

                // Archive artifacts
                archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true
                archiveArtifacts artifacts: 'locust/*.html', allowEmptyArchive: true
                archiveArtifacts artifacts: 'newman-report.html', allowEmptyArchive: true

                // Clean up Docker images to save space
                bat '''
                    docker system prune -f --volumes || true
                    docker image prune -f || true
                '''
            }
        }

        success {
            script {
                if (env.BRANCH_NAME == 'master' || env.BRANCH_NAME == 'prod') {
                    echo 'Production deployment successful!'
                    // Send notification
                    emailext(
                        subject: "✅ Pipeline Success: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                        body: """
                        Pipeline executed successfully!

                        Branch: ${env.BRANCH_NAME}
                        Build: ${env.BUILD_NUMBER}
                        Environment: ${ENVIRONMENT}
                        Namespace: ${NAMESPACE}

                        All services have been deployed and verified.
                        """,
                        to: "${env.CHANGE_AUTHOR_EMAIL ?: 'dev-team@company.com'}"
                    )
                }
            }
        }

        failure {
            script {
                echo 'Pipeline failed!'
                // Clean up on failure
                bat '''
                    docker-compose -f compose.yml down -v || true
                    kubectl delete namespace ${NAMESPACE} --ignore-not-found=true || true
                '''

                // Send failure notification
                emailext(
                    subject: "❌ Pipeline Failed: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                    body: """
                    Pipeline execution failed!

                    Branch: ${env.BRANCH_NAME}
                    Build: ${env.BUILD_NUMBER}
                    Stage: ${env.STAGE_NAME}

                    Please check the build logs for details.

                    Build URL: ${env.BUILD_URL}
                    """,
                    to: "${env.CHANGE_AUTHOR_EMAIL ?: 'dev-team@company.com'}"
                )
            }
        }

        unstable {
            script {
                echo 'Pipeline completed with warnings'
                emailext(
                    subject: "⚠️ Pipeline Unstable: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                    body: """
                    Pipeline completed with warnings!

                    Branch: ${env.BRANCH_NAME}
                    Build: ${env.BUILD_NUMBER}

                    Some tests may have failed or warnings were detected.
                    Please review the build results.

                    Build URL: ${env.BUILD_URL}
                    """,
                    to: "${env.CHANGE_AUTHOR_EMAIL ?: 'dev-team@company.com'}"
                )
            }
        }

        cleanup {
            script {
                echo 'Cleaning up workspace...'
                // Clean workspace
                cleanWs()

                // Clean up any remaining port forwards
                bat '''
                    pkill -f "kubectl port-forward" || true
                    pkill -f "locust" || true
                '''
            }
        }
    }
}
