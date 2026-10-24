// Jenkins pipeline for the Web UI tests.
// Needs: Pipeline, JUnit and Credentials Binding plugins.
// The tool names below must match Manage Jenkins > Tools.
// For BrowserStack runs, add a "Username with password" credential with id "browserstack-credentials".
pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'Maven3'
    }

    parameters {
        choice(name: 'ENV', choices: ['qa', 'sit', 'uat'], description: 'Environment to test')
        choice(name: 'BROWSER', choices: ['chrome', 'edge', 'firefox'], description: 'Browser to run on')
        booleanParam(name: 'PRIVATE_MODE', defaultValue: false, description: 'Incognito / InPrivate / private window')
        booleanParam(name: 'HEADLESS', defaultValue: true, description: 'Run without a visible browser window')
        choice(name: 'EXECUTION', choices: ['local', 'browserstack'], description: 'Run on the agent or on BrowserStack')
        string(name: 'SUITE', defaultValue: 'src/test/resources/testng.xml', description: 'TestNG suite file')
    }

    options {
        timestamps()
        timeout(time: 60, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Run tests') {
            steps {
                script {
                    def mvnCmd = "mvn -B clean test" +
                            " -Denv=${params.ENV}" +
                            " -DsuiteXmlFile=${params.SUITE}" +
                            " -Dbrowser=${params.BROWSER}" +
                            " -Dprivate.mode=${params.PRIVATE_MODE}" +
                            " -Dheadless=${params.HEADLESS}" +
                            " -Dexecution=${params.EXECUTION}"

                    if (params.EXECUTION == 'browserstack') {
                        withCredentials([usernamePassword(credentialsId: 'browserstack-credentials',
                                usernameVariable: 'BROWSERSTACK_USERNAME',
                                passwordVariable: 'BROWSERSTACK_ACCESS_KEY')]) {
                            runCommand(mvnCmd)
                        }
                    } else {
                        runCommand(mvnCmd)
                    }
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
            archiveArtifacts allowEmptyArchive: true, artifacts: 'reports/**, logs/**'
        }
    }
}

// Lets the same Jenkinsfile run on Windows and Linux agents.
def runCommand(String cmd) {
    if (isUnix()) {
        sh cmd
    } else {
        bat cmd
    }
}
