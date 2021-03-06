/*
    Pipeline script for executing Tier 2 object test suites for RH Ceph 5.0.
*/
// Global variables section

def nodeName = "centos-7"
def cephVersion = "pacific"
def sharedLib
def testResults = [:]

// Pipeline script entry point

node(nodeName) {

    timeout(unit: "MINUTES", time: 30) {
        stage('Configure node') {
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

    timeout(unit: "HOURS", time: 4) {
        stage('Regression') {
            script {
                withEnv([
                    "sutVMConf=conf/inventory/rhel-8.4-server-x86_64-medlarge.yaml",
                    "sutConf=conf/${cephVersion}/rgw/tier_2_rgw_regression.yaml",
                    "testSuite=suites/${cephVersion}/rgw/tier_2_rgw_regression.yaml",
                    "addnArgs=--post-results --log-level DEBUG"
                ]) {
                    rc = sharedLib.runTestSuite()
                    testResults['Regression'] = rc
                }
            }
        }
    }

    stage('Publish Results') {
        script {
            sharedLib.sendEMail("Tier-2-Object", testResults)
        }
    }

}
