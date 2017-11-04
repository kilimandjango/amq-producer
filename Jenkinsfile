#!groovy
String mailTo = "kilian.henneboehle@mailbox.org"
def appName

@NonCPS
def notifyEveryUnstableBuild(String mailTo) {
    step([$class: 'Mailer', recipients: mailTo, notifyEveryUnstableBuild: true, sendToIndividuals: false, perModuleEmail: false])
}

timestamps {
    catchError {
        node {      
			appName = "amq-producer-test"
			def gitUrl      
            env.SKIP_TLS = 'true'		
            env.PROJECT_NAMESPACE = sh returnStdout: true, script: "cat /run/secrets/kubernetes.io/serviceaccount/namespace"
            stage("Delete Workspace") {
                try {
                    sh 'find . \\( -iname "*" ! -iname ".nfs*" \\) -delete'
                } catch (err) {
              }
            }
          	stage("checkout"){
                openshiftImageStream name: 'openjdk18-openshift', tag: 'latest', verbose: 'false'
           		deleteDir()
            	checkout(scm)
                gitUrl = sh returnStdout: true, script: 'git config remote.origin.url'
                if (gitUrl.size() < 1) {
                    error "gitUrl not found"
                }
          	}
           	stage("amq-producer-builder-image OSCP Build") {            
            	openshiftBuild bldCfg: 'amq-producer-builder-image', checkForTriggeredDeployments: 'false', showBuildLogs: 'true', verbose: 'false'
            }          
            stage("Maven build"){

                dir("test") {
                    configFileProvider(
                            def mvnHome = tool 'm3'
                            [configFile(fileId: '8858527d-791a-4ed2-aa51-51a5ad392f97', variable: 'MAVEN_SETTINGS')]) {
                        sh '${mvnHome}/bin/mvn -X -B -U -e -Dmaven.test.failure.ignore=true -s $MAVEN_SETTINGS install'
                    }

                  archiveArtifacts '**/target/*.jar'
                }
            } // end of dir("test")
			stage("Create Testprojekt Resources") {
            	oscpDeleteResource(appName) // Delete Testprojekt Resources
                	sh """oc new-app -f amq-producer-s2i.yaml -n ${env.PROJECT_NAMESPACE} -l app=${appName} -p \
					APPLICATION_NAME=${appName} -p SOURCE_REPOSITORY_REF='' -p CONTEXT_DIR=test -p SOURCE_REPOSITORY_URL=${gitUrl}"""
            }
            stage("Testprojekt OSCP Build") {
            	openshiftBuild apiURL: '', authToken: "", bldCfg: appName, buildName: '', checkForTriggeredDeployments: 'true', commitID: '', env:
                            [[name: 'BUILD_URL', value: "${BUILD_URL}"]], namespace: '', showBuildLogs: 'true', verbose: 'false'
                    openshiftDeploy apiURL: '', authToken: '', depCfg: appName, namespace: '', verbose: 'false', waitTime: '300', waitUnit: 'sec'
            }
            stage("tag imagestream") {
            	openshiftTag alias: 'false', destStream: 'amq-producer-s2i', destTag: 'latest', srcStream: 'amq-producer-image-builderi', srcTag: 'latest', verbose: 'false'
            }
            stage("Delete Testprojekt Resources") {
            	oscpDeleteResource(appName)
            }

        } // Ende node
    } // Ende  catchError
    node {
        notifyEveryUnstableBuild(mailTo)
    }
} // Ende timestamps

def oscpDeleteResource(String appLabel) {
    try {
        openshiftDeleteResourceByLabels apiURL: '', authToken: '', keys: 'application', namespace: '',
           types: 'Route,Service,DeploymentConfig,ImageStream,BuildConfig,Build,ReplicationController,Pod,ServiceAccount,PersistentVolumeClaim,RoleBinding',
           values: appLabel, verbose: 'false'
    } catch (e) {
        echo 'exception on openshiftDelete test-jenkins: ' + e
    }
}

@NonCPS
def makePushUrl(String gitUrl, String gitCredentials) {
    def gitUrlMatcher = gitUrl =~ '(.+://)(.+)'
    if (gitUrlMatcher) {
        def gitProtocol = gitUrlMatcher[0][1]
        def gitHostAndPath = gitUrlMatcher[0][2]
        def gitUrlMatcher2 = gitHostAndPath =~ '@(.+)'
        if (gitUrlMatcher2) {
            gitHostAndPath = gitUrlMatcher2[0][1]
        }
        return "${gitProtocol}${gitCredentials}@${gitHostAndPath}"
    } else {
        error("GIT URL konnte nicht ausgelesen werden!")
    }
}
