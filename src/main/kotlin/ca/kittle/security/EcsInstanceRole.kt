package ca.kittle.security

import ca.kittle.Stack
import com.pulumi.aws.iam.kotlin.*

suspend fun createEcsInstanceRole(env: Stack): Role {
    val role = role("${env.stackEnv}-ecs-instance-role") {
        args {
            assumeRolePolicy(
                """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Action": "sts:AssumeRole",
                      "Principal": {
                        "Service": "ec2.amazonaws.com"
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
    createInstanceProfile(env, role)
    attachEcsInstanceRole(env, role)
    attachS3ReadRole(env, role)
    return role
}

private suspend fun createInstanceProfile(env: Stack, role: Role): InstanceProfile {
    val roleName = role.name.applyValue(fun(name: String): String { return name })
    return instanceProfile("${env.stackEnv}-instance-profile") {
        args {
            role(roleName)
        }
    }
}


private suspend fun attachEcsInstanceRole(env: Stack, role: Role) {
    val roleName = role.name.applyValue(fun(name: String): String { return name })
    rolePolicyAttachment("${env.stackEnv}-ecs-instance-attachment") {
        args {
            role(roleName)
            policyArn("arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role")
        }
    }
}

private suspend fun attachS3ReadRole(env: Stack, role: Role) {
    val roleName = role.name.applyValue(fun(name: String): String { return name })
    rolePolicyAttachment("${env.stackEnv}-s3-read-role-attachment") {
        args {
            role(roleName)
            policyArn("arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess")
        }
    }
}