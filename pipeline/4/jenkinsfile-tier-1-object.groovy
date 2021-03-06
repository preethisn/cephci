/*
    Pipeline script for executing Tier 1 object test suites for RH Ceph 4.x
*/
// Global variables section

def nodeName = "centos-7"
def cephVersion = "nautilus"
def sharedLib
def testResults = [:]

// Pipeline script entry point

node(nodeName) {

    timeout(unit: "MINUTES", time: 30) {
        stage('Install prereq') {
            checkout([
                $class: 'GitSCM',
                branches: [[name: '*/master']],
                doGenerateSubmoduleConfigurations: false,
                extensions: [[
                    $class: 'SubmoduleOption',
                    disableSubmodules: false,
                    parentCredentials: false,
                    recursiveSubmodules: true,
                    reference: '',
                    trackingSubmodules: false
                ]],
                submoduleCfg: [],
                userRemoteConfigs: [[url: 'https://github.com/red-hat-storage/cephci.git']]
            ])
            script {
                sharedLib = load("${env.WORKSPACE}/pipeline/vars/common.groovy")
                sharedLib.prepareNode()
            }
        }
    }

    timeout(unit: "MINUTES", time: 120) {
        stage('Single-site') {
            script {
                withEnv([
                    "sutVMConf=conf/inventory/rhel-7.9-server-x86_64.yaml",
                    "sutConf=conf/${cephVersion}/rgw/tier_1_rgw.yaml",
                    "testSuite=suites/${cephVersion}/rgw/tier_1_object.yaml",
                    "addnArgs=--post-results --log-level DEBUG"
                ]) {
                    rc = sharedLib.runTestSuite()
                    testResults['Single_Site'] = rc
                }
            }
        }
    }

    stage('Publish Results') {
        script {
            sharedLib.sendEMail("Object-Tier-1",testResults)
        }
    }
}
