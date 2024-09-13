package ca.kittle.security

import ca.kittle.Stack
import com.pulumi.aws.iam.kotlin.Role
import com.pulumi.aws.iam.kotlin.role
import com.pulumi.aws.iam.kotlin.rolePolicyAttachment

suspend fun createTaskExecutionRole(env: Stack): Role {
    val role = role("${env.name.lowercase()}-task-execution-role") {
        args {
            assumeRolePolicy(
                """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Action": "sts:AssumeRole",
                      "Principal": {
                        "Service": "ecs-tasks.amazonaws.com"
                      },
                      "Effect": "Allow",
                      "Sid": ""
                    }
                  ]
                }
                """.trimIndent()
            )
        }
    }

    attachTaskExecutionRole(env, role)
    return role
}

private suspend fun attachTaskExecutionRole(env: Stack, role: Role) {
    val roleName = role.name.applyValue(fun(name: String): String { return name })
    rolePolicyAttachment("${env.name.lowercase()}-task-execution-attachment") {
        args {
            role(roleName)
            policyArn("arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy")
        }
    }
}