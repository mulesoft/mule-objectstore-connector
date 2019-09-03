Map pipelineParams = ["mavenTestGoal"  : "verify -Dannotation.failOnError=false",
                      "mavenDeployGoal": "deploy -DskipTests -DskipITs -Dinvoker.skip=true -Dannotation.failOnError=false"
]

runtimeExtensionsBuild(pipelineParams)
