package ca.kittle.security

import ca.kittle.Stack
import com.pulumi.aws.iam.kotlin.InstanceProfile
import com.pulumi.aws.iam.kotlin.Policy
import com.pulumi.aws.iam.kotlin.Role
import com.pulumi.aws.iam.kotlin.instanceProfile
import com.pulumi.aws.iam.kotlin.policy
import com.pulumi.aws.iam.kotlin.role
import com.pulumi.aws.iam.kotlin.rolePolicyAttachment
import com.pulumi.core.Output

suspend fun createEc2ContainerHostProfile(env: Stack): InstanceProfile {
    val role = role("${env.stackName}-ec2-container-host-role") {
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
                      "Effect": "Allow"
                    }
                  ]
                }
                """.trimIndent()
            )
        }
    }

    val appConfigReadPolicy = policy("${env.stackName}-ec2-app-config-read-policy") {
        args {
            policy(
                """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Action": [
                        "ssm:GetParameter",
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

    attachManagedPolicy(env, role, "ecr-readonly", "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly")
    attachManagedPolicy(env, role, "ssm-core", "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore")
    attachManagedPolicy(env, role, "app-config-read", appConfigReadPolicy)

    val roleName = role.name.applyValue(fun(name: String): String { return name })
    return instanceProfile("${env.stackName}-ec2-container-host-profile") {
        args {
            role(roleName)
        }
    }
}

private suspend fun attachManagedPolicy(env: Stack, role: Role, suffix: String, policyArnValue: String) {
    attachManagedPolicy(env, role, suffix, Output.of(policyArnValue))
}

private suspend fun attachManagedPolicy(env: Stack, role: Role, suffix: String, policyArnValue: Output<String>) {
    val roleName = role.name.applyValue(fun(name: String): String { return name })
    rolePolicyAttachment("${env.stackName}-${suffix}-attachment") {
        args {
            role(roleName)
            policyArn(policyArnValue)
        }
    }
}

private suspend fun attachManagedPolicy(env: Stack, role: Role, suffix: String, policy: Policy) {
    val policyArn = policy.arn.applyValue(fun(arn: String): String { return arn })
    attachManagedPolicy(env, role, suffix, policyArn)
}
