package org.devops

//格式化输出
def PrintMes(value, color) {
    colors = ['red'   : "\033[40;31m >>>>>>>>>>>${value}<<<<<<<<<<< \033[0m",
              'blue'  : "\033[47;34m ${value} \033[0m",
              'green' : "[1;32m>>>>>>>>>>${value}>>>>>>>>>>[m",
              'green1': "\033[40;32m >>>>>>>>>>>${value}<<<<<<<<<<< \033[0m"]
    ansiColor('xterm') {
        println(colors[color])
    }
}

def checkOut(srcUrl, branchName) {
    checkout([$class                           : 'GitSCM', branches: [[name: "${branchName}"]],
              doGenerateSubmoduleConfigurations: false,
              extensions                       : [],
              submoduleCfg                     : [],
              userRemoteConfigs                : [[credentialsId: '0fb2d9d5-ee9e-4901-bf65-9a613a679871', url: "${srcUrl}"]]])
}


def sonarScan(buildType) {
    def home = buildHome(buildType)
    withSonarQubeEnv('sonarqube') {
        // If you have configured more than one global server connection, you can specify its name
        sh "${home}/bin/${buildType} clean verify  -Dmaven.test.skip=true sonar:sonar"
    }
}

def buildHome(buildType) {
    def buildTools = ["mvn": "M3", "ant": "ANT", "gradle": "GRADLE", "npm": "NPM"]
    println("当前选择的构建类型为 ${buildType}")
    def home = tool buildTools[buildType]
    return home
}

//构建类型
def build(buildType, buildShell) {
    def home = buildHome(buildType)
    sh "${home}/bin/${buildType}  ${buildShell}"
}

//获取POM中的坐标
def GetGav() {
    def pom = readMavenPom file: 'pom.xml'
    env.pomVersion = "${pom.version}"
    env.pomArtifact = "${pom.properties["deploy.packname"]}"
    if (pomArtifact == null || pomArtifact == "") {
        env.pomArtifact = "${pom.artifactId}"
    }
    env.pomGroupId = "${pom.groupId}"

    println("${pomGroupId}-${pomArtifact}-${pomVersion}")

    return ["${pomGroupId}", "${pomArtifact}", "${pomVersion}"]
}

def deploy(deployHosts) {
    GetGav()
    withCredentials([usernamePassword(credentialsId: 'nexus', passwordVariable: 'password', usernameVariable: 'username')]) {
        def repository = "http://192.168.80.131:8081/repository/maven-snapshots/"
        println("开始部署")
        ansiblePlaybook(
                installation: 'Ansible',
                playbook: '/etc/ansible/deploy.yml',
                extraVars: [
                        groupId:"${pomGroupId}",
                        artifactId:"${pomArtifact}",
                        appVersion:"${pomVersion}",
                        deployIp:"${deployHosts}",
                        repository:"${repository}",
                        username:"${username}",
                        password:"${password}"
                ]
        )
        // some block
//        ansiblePlaybook extras: 'groupId=${pomGroupId} artifactId=${pomArtifact} appVersion=${pomVersion} deployIp=${deployHosts} repository=${repository}', installation: 'Ansible', playbook: '/etc/ansible/deploy.yml'
//        sh """
//            ${home}/ansible-playbook /etc/ansible/deploy.yml
//            -e "groupId=${pomGroupId} artifactId=${pomArtifact} appVersion=${pomVersion} deployIp=${deployHosts} repository=${repository}"  """
    }
}
