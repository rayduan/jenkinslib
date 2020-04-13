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
    return  home
}

//构建类型
def build(buildType, buildShell) {
    def home = buildHome(buildType)
    if ("${buildType}" == "npm") {
        sh """ 
            export NODE_HOME=${buildHome} 
            export PATH=\$NODE_HOME/bin:\$PATH 
            ${home}/bin/${buildType} ${buildShell}"""
    } else {
        sh "${home}/bin/${buildType}  ${buildShell}"
    }
}
