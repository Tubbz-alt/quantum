node {
    try {
        go_dir = "/opt/go/src/github.com/Supernomad/quantum"

        stage("Checkout") {
            checkout([
                $class: 'GitSCM',
                branches: scm.branches,
                doGenerateSubmoduleConfigurations: false,
                extensions: scm.extensions + [[$class: 'SubmoduleOption', parentCredentials: true, recursiveSubmodules: true]],
                userRemoteConfigs: scm.userRemoteConfigs
            ])
        }

        builder = docker.build('builder', '--pull -f ./dist/Dockerfile.builder ./dist/')
        builder.inside('--net host') {
            stage("Dependencies") {
                sh """
                    mkdir -p /opt/go/src/github.com/Supernomad
                    ln -s ${env.WORKSPACE} ${go_dir}
                """
                sh "cd ${go_dir}; make setup_ci"
            }

            stage("Compile") {
                sh "cd ${go_dir}; make lint compile"
            }

            stage("Test") {
                sh "cd ${go_dir}; make test_ci"
            }

            stage('Results') {
                junit allowEmptyResults: true, testResults: 'tests.xml'
                step([$class: 'PlotBuilder', csvFileName: 'plot-54309763.csv', exclZero: false, group: 'benchmarks', keepRecords: false, logarithmic: false, numBuilds: '', style: 'line', title: 'Allocations Per Call', useDescr: false, xmlSeries: [[file: 'benchmarks.xml', nodeType: 'NODESET', url: '', xpath: '/Benchmarks/AllocsPerOp/*']], yaxis: 'Allocs (count)', yaxisMaximum: '', yaxisMinimum: ''])
                step([$class: 'PlotBuilder', csvFileName: 'plot-56564010.csv', exclZero: false, group: 'benchmarks', keepRecords: false, logarithmic: false, numBuilds: '', style: 'line', title: 'Allocated Bytes Per Call', useDescr: false, xmlSeries: [[file: 'benchmarks.xml', nodeType: 'NODESET', url: '', xpath: '/Benchmarks/AllocsBytesPerOp/*']], yaxis: 'Allocs (B)', yaxisMaximum: '', yaxisMinimum: ''])
                step([$class: 'PlotBuilder', csvFileName: 'plot-21467362.csv', exclZero: false, group: 'benchmarks', keepRecords: false, logarithmic: false, numBuilds: '', style: 'line', title: 'Time Per Call', useDescr: false, xmlSeries: [[file: 'benchmarks.xml', nodeType: 'NODESET', url: '', xpath: '/Benchmarks/NsPerOp/*']], yaxis: 'Time (ns)', yaxisMaximum: '', yaxisMinimum: ''])
                step([$class: 'CoberturaPublisher', autoUpdateHealth: false, autoUpdateStability: false, coberturaReportFile: 'coverage.xml', failUnhealthy: false, failUnstable: false, maxNumberOfBuilds: 0, onlyStable: false, sourceEncoding: 'ASCII', zoomCoverageChart: false])
                archiveArtifacts '*.xml, quantum'
            }
        }
    }
    finally {
        cleanWs()
    }
}
