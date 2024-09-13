package ca.kittle.cluster

import ca.kittle.Stack
import com.pulumi.aws.appconfig.kotlin.environment
import com.pulumi.aws.ecs.kotlin.TaskDefinition
import com.pulumi.aws.ecs.kotlin.taskDefinition
import com.pulumi.aws.iam.kotlin.Role

suspend fun createTaskDefinition(env: Stack, taskExecutionRole: Role): TaskDefinition {
    val roleArn = taskExecutionRole.arn.applyValue(fun(arn: String): String { return arn })
    return taskDefinition("${env.name.lowercase()}-task-definition") {
        args {
            family("ecs-dmseer-family")
            networkMode("awsvpc")
            executionRoleArn(roleArn)
            containerDefinitions("""
                [
                    {
                        "name": "dmseer-app",
                        "image": "814245790557.dkr.ecr.ca-central-1.amazonaws.com/prod-dmseer-repository-2e55088/latest",
                        "memory": 800,
                        "portMappings": [
                            {
                                "containerPort": 80,
                                "hostPort": 80
                            }
                        ]
                    }
                ]
                """.trimIndent())
        }
    }
}