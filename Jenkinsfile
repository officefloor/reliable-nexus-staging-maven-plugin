pipeline {
	agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
		disableConcurrentBuilds()
		timeout(time: 16, unit: 'HOURS')
    }

	tools {
	    maven 'maven-3.6.0'
	    jdk 'jdk8'
	}
	
	stages {
		
		stage('Check master') {
	        when {
	        	allOf {
	        	    not { branch 'master' }
	        	}
	        }
            steps {
            	script {
            		currentBuild.result = 'ABORTED'
            	}
            	error "Aborting as not on master branch"
            }
        }
	
		stage('Enhance') {
	        steps {
	        	echo "JAVA_HOME = ${env.JAVA_HOME}"
	        	sh 'mvn -B clean test'
	        }
		}
	}

}
