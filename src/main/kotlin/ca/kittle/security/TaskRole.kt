package ca.kittle.security

import ca.kittle.Stack
import com.pulumi.aws.iam.kotlin.*

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

    val policy = createImageAndLogPolicy(env)
    val paramPolicy = createParameterStoreReadPolicy(env)
    val mongoPolicy = createMongoAccessPolicy(env)
    attachImageAndLogPolicy(env, role, policy)
    attachParamStoreReadPolicy(env, role, paramPolicy)
    attachMongoAccessPolicy(env, role, mongoPolicy)
    return role
}

suspend fun createImageAndLogPolicy(env: Stack): Policy = policy("${env.name.lowercase()}-task-execution-policy") {
    args {
        policy(
            """
                {
                      "Version": "2012-10-17",
                      "Statement": [
                        {
                          "Effect": "Allow",
                          "Action": [
                            "ecr:GetAuthorizationToken",
                            "ecr:GetDownloadUrlForLayer",
                            "ecr:BatchGetImage",
                            "ecr:BatchCheckLayerAvailability",
                            "logs:CreateLogStream",
                            "logs:PutLogEvents",
                            "logs:CreateLogGroup"
                          ],
                          "Resource": "*"
                        }
                      ]
                    }
            """.trimIndent()
        )
    }
}

suspend fun createParameterStoreReadPolicy(env: Stack): Policy = policy("${env.name.lowercase()}-paramstore-read-policy") {
    args {
        policy(
            """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Action": [
                        "ssm:GetParameters",
                        "secretsmanager:GetSecretValue",
                        "kms:Decrypt"
                      ],
                      "Resource": "*"
                    }
                  ]
                }                
            """.trimIndent()
        )
    }
}

suspend fun createMongoAccessPolicy(env: Stack): Policy = policy("${env.name.lowercase()}-mongo-access-policy") {
    args {
        policy(
            """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Action": [
                        "documentdb:Connect",
                        "documentdb:GetItem"
                      ],
                      "Resource": "*"
                    }
                  ]
                }                
            """.trimIndent()
        )
    }
}

private suspend fun attachImageAndLogPolicy(env: Stack, role: Role, policy: Policy) {
    val roleName = role.name.applyValue(fun(name: String): String { return name })
    val policyArn = policy.arn.applyValue(fun(name: String): String { return name })
    rolePolicyAttachment("${env.name.lowercase()}-task-execution-attachment") {
        args {
            role(roleName)
            policyArn(policyArn)
        }
    }
}

private suspend fun attachParamStoreReadPolicy(env: Stack, role: Role, policy: Policy) {
    val roleName = role.name.applyValue(fun(name: String): String { return name })
    val policyArn = policy.arn.applyValue(fun(name: String): String { return name })
    rolePolicyAttachment("${env.name.lowercase()}-paramstore-read-attachment") {
        args {
            role(roleName)
            policyArn(policyArn)
        }
    }
}

private suspend fun attachMongoAccessPolicy(env: Stack, role: Role, policy: Policy) {
    val roleName = role.name.applyValue(fun(name: String): String { return name })
    val policyArn = policy.arn.applyValue(fun(name: String): String { return name })
    rolePolicyAttachment("${env.name.lowercase()}-mongo-access-attachment") {
        args {
            role(roleName)
            policyArn(policyArn)
        }
    }
}