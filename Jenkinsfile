Map pipelineParams = [ "mavenTestGoal"   : "verify -Dannotation.failOnError=false",
                       "mavenDeployGoal" : "deploy -DskipTests -Dannotation.failOnError=false",
                       "projectType"     : "Extensions" ]

runtimeBuild(pipelineParams)
