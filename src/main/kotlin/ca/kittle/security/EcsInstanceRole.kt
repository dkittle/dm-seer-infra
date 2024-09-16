package ca.kittle.security

import ca.kittle.Stack
import com.pulumi.aws.iam.kotlin.*

suspend fun createEcsInstanceRole(env: Stack): Role {
    val role = role("${env.stackName}-ecs-instance-role") {
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
            policy("${env.stackName}-parameter-store-read-policy") {
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
                              "Resource": [
                                "*"
                              ]
                            }
                          ]
                        }                
                    """.trimIndent())
                }
            }

        }
    }
    createInstanceProfile(env, role)
    attachEcsInstancePolicy(env, role)
    attachS3ReadPolicy(env, role)
//    attachParameterStorePolicy(env, role)
    return role
}

private suspend fun createInstanceProfile(env: Stack, role: Role): InstanceProfile {
    val roleName = role.name.applyValue(fun(name: String): String { return name })
    return instanceProfile("${env.stackName}-instance-profile") {
        args {
            role(roleName)
        }
    }
}


private suspend fun attachEcsInstancePolicy(env: Stack, role: Role) {
    val roleName = role.name.applyValue(fun(name: String): String { return name })
    rolePolicyAttachment("${env.stackName}-ecs-instance-attachment") {
        args {
            role(roleName)
//            policyArn("arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role")
            policyArn("arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy")
        }
    }
}

private suspend fun attachS3ReadPolicy(env: Stack, role: Role) {
    val roleName = role.name.applyValue(fun(name: String): String { return name })
    rolePolicyAttachment("${env.stackName}-s3-read-role-attachment") {
        args {
            role(roleName)
            policyArn("arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess")
        }
    }
}

private suspend fun attachParameterStorePolicy(env: Stack, role: Role) {
    val roleName = role.name.applyValue(fun(name: String): String { return name })
    rolePolicyAttachment("${env.stackName}-parameter-store-read-role-attachment") {
        args {
            role(roleName)
            policy("${env.stackName}-parameter-store-read-policy") {
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
                              "Resource": [
                                "*"
                              ]
                            }
                          ]
                        }                
                    """.trimIndent())
                }
            }

        }
    }
}

//"arn:aws:ssm:region:aws_account_id:parameter/parameter_name",
//"arn:aws:secretsmanager:region:aws_account_id:secret:secret_name",
//"arn:aws:kms:region:aws_account_id:key/key_id"